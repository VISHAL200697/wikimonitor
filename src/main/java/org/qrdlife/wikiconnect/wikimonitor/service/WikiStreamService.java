package org.qrdlife.wikiconnect.wikimonitor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.qrdlife.wikiconnect.wikimonitor.WikiMonitorApplication;
import org.qrdlife.wikiconnect.wikimonitor.model.RecentChange;
import org.qrdlife.wikiconnect.wikimonitor.model.User;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Service responsible for connecting to the Wikimedia EventStreams SSE
 * endpoint,
 * processing events, and broadcasting them to connected clients.
 * Supports per-user filtering via AbuseFilterService.
 */
@Slf4j
@Service
public class WikiStreamService {

    private final OkHttpClient client;
    private final ObjectMapper mapper;
    private final AbuseFilterService abuseFilter;
    private final UserService userService;
    private final Map<SseEmitter, StreamContext> emitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private EventSource eventSource;

    private volatile String lastEventId;
    // Global pause removed in favor of per-user pause in StreamContext

    public WikiStreamService(ObjectMapper mapper, AbuseFilterService abuseFilter, UserService userService) {
        this.mapper = mapper;
        this.abuseFilter = abuseFilter;
        this.userService = userService;
        this.client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.SECONDS) // No timeout for SSE
                .build();
    }

    /**
     * Connects to the Wikimedia SSE stream and starts listening for events.
     * Automatically handles reconnections on failure.
     */
    @PostConstruct
    public void startStream() {
        if (eventSource != null) {
            eventSource.cancel();
        }

        Request.Builder builder = new Request.Builder()
                .url("https://stream.wikimedia.org/v2/stream/recentchange")
                .header("User-Agent", WikiMonitorApplication.userAgent);

        if (lastEventId != null) {
            log.info("Resuming stream from last event ID: {}", lastEventId);
            builder.header("Last-Event-ID", lastEventId);
        }
        Request request = builder.build();
        EventSource.Factory factory = EventSources.createFactory(client);
        this.eventSource = factory.newEventSource(request, new EventSourceListener() {
            @Override
            public void onOpen(@NotNull EventSource eventSource, @NotNull Response response) {
                log.info("Connected to Wikimedia Stream");
            }

            @Override
            public void onEvent(@NotNull EventSource eventSource, @Nullable String id, @Nullable String type,
                    @NotNull String data) {
                if (id != null) {
                    lastEventId = id;
                }
                // Global pause check removed
                try {
                    RecentChange rc = mapper.readValue(data, RecentChange.class);
                    broadcastAsync(rc);
                } catch (Exception e) {
                    log.error("Error processing event: {}", e.getMessage());
                }
            }

            @Override
            public void onClosed(@NotNull EventSource eventSource) {
                log.info("Stream closed");
                scheduleReconnect();
            }

            @Override
            public void onFailure(@NotNull EventSource eventSource, @Nullable Throwable t,
                    @Nullable Response response) {
                log.error("Stream failure: {}", t != null ? t.toString() : "Unknown");
                scheduleReconnect();
            }
        });
    }

    private void scheduleReconnect() {
        if (scheduler.isShutdown()) {
            return;
        }
        try {
            scheduler.schedule(this::startStream, 5, TimeUnit.SECONDS);
        } catch (java.util.concurrent.RejectedExecutionException e) {
            log.debug("Scheduler rejected task, likely shutting down.");
        }
    }

    private void broadcastAsync(RecentChange rc) {
        if (emitters.isEmpty())
            return;

        String payload;
        try {
            ObjectNode node = mapper.valueToTree(rc);
            node.put("flagged", true);
            payload = mapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            log.error("Error serializing RecentChange", e);
            return;
        }

        emitters.forEach((emitter, context) -> {
            if (context.paused)
                return;

            CompletableFuture.runAsync(() -> {
                try {
                    List<String> matchedFilters = abuseFilter.matches(rc, context.user);
                    if (!matchedFilters.isEmpty()) {
                        ObjectNode node = mapper.valueToTree(rc);
                        node.put("flagged", true);
                        node.putPOJO("matchedFilters", matchedFilters);
                        String userPayload = mapper.writeValueAsString(node);

                        SseEmitter.SseEventBuilder event = SseEmitter.event()
                                .id(lastEventId)
                                .data(userPayload);

                        emitter.send(event);
                    }
                } catch (Exception e) {
                    emitter.completeWithError(e);
                    emitters.remove(emitter);
                }
            }, executor);
        });
    }

    @Deprecated
    private void broadcast(RecentChange rc) {
        emitters.entrySet().parallelStream().forEach(entry -> {
            SseEmitter emitter = entry.getKey();
            StreamContext context = entry.getValue();

            if (context.paused) {
                return;
            }
            try {
                List<String> matchedFilters = abuseFilter.matches(rc, context.user);
                if (!matchedFilters.isEmpty()) {
                    ObjectNode node = mapper.valueToTree(rc);
                    node.put("flagged", true);
                    node.putPOJO("matchedFilters", matchedFilters);
                    log.debug("Broadcasting match to {}", context.user.getUsername());
                    emitter.send(mapper.writeValueAsString(node));
                }
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        });
    }

    /**
     * Subscribes a client to the event stream.
     *
     * @param principal The authenticated user principal, or null if anonymous.
     * @return An SseEmitter instance for receiving events.
     */
    public SseEmitter subscribe(Principal principal) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        User user = null;
        if (principal != null) {
            try {
                user = (User) ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal)
                        .getPrincipal();
            } catch (Exception e) {
                user = (User) userService.loadUserByUsername(principal.getName());
            }
        }

        if (user != null) {
            emitters.put(emitter, new StreamContext(user));
            log.info("Client subscribed: {}", user.getUsername());
        } else {
            log.info("Client subscribed: Anonymous");
        }

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        return emitter;
    }

    /**
     * Pauses or resumes the stream for a specific user.
     *
     * @param principal The authenticated user.
     * @param paused    True to pause, false to resume.
     */
    public void setPaused(Principal principal, boolean paused) {
        if (principal == null)
            return;
        String username = principal.getName();
        emitters.values().stream()
                .filter(ctx -> ctx.user.getUsername().equals(username))
                .forEach(ctx -> {
                    ctx.paused = paused;
                    log.debug("Stream paused for {}: {}", username, paused);
                });
    }

    public boolean isPaused(Principal principal) {
        if (principal == null)
            return false;
        String username = principal.getName();
        return emitters.values().stream()
                .filter(ctx -> ctx.user.getUsername().equals(username))
                .findFirst()
                .map(ctx -> ctx.paused)
                .orElse(false);
    }

    public void updateUser(User updatedUser) {
        if (updatedUser == null || updatedUser.getId() == null) {
            return;
        }
        emitters.values().stream()
                .filter(ctx -> ctx.user.getId().equals(updatedUser.getId()))
                .forEach(ctx -> ctx.user = updatedUser);
    }

    @PreDestroy
    public void cleanup() {
        if (eventSource != null) {
            eventSource.cancel();
        }
        client.dispatcher().executorService().shutdown();
        scheduler.shutdown();
    }

    private static class StreamContext {
        User user;
        boolean paused;

        StreamContext(User user) {
            this.user = user;
            this.paused = false;
        }
    }
}

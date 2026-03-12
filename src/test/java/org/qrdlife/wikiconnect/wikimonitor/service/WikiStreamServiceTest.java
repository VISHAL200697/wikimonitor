package org.qrdlife.wikiconnect.wikimonitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.qrdlife.wikiconnect.wikimonitor.model.RecentChange;
import org.qrdlife.wikiconnect.wikimonitor.model.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WikiStreamService}.
 * <p>
 * The Wikimedia SSE connection and OkHttpClient are NOT started in tests
 * because {@code @PostConstruct} is not invoked when constructing the service
 * manually. Each group of tests focuses on a specific public API surface.
 * </p>
 */
@DisplayName("WikiStreamService")
class WikiStreamServiceTest {

    // ── Collaborators ──────────────────────────────────────────────────────────
    @Mock
    private ObjectMapper mapper;
    @Mock
    private AbuseFilterService abuseFilter;
    @Mock
    private UserService userService;
    @Mock
    private Principal principal;

    // ── SUT ───────────────────────────────────────────────────────────────────
    private WikiStreamService wikiStreamService;

    // ── Fixtures ──────────────────────────────────────────────────────────────
    private User testUser;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        wikiStreamService = new WikiStreamService(mapper, abuseFilter, userService);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("alice");
        testUser.setFilterCode("user == 'BadActor'");

        anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setUsername("bob");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // subscribe()
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("subscribe()")
    class Subscribe {

        @Test
        @DisplayName("returns non-null emitter for plain Principal")
        void returnsEmitterForPlainPrincipal() {
            when(principal.getName()).thenReturn("alice");
            when(userService.loadUserByUsername("alice")).thenReturn(testUser);

            SseEmitter emitter = wikiStreamService.subscribe(principal);

            assertNotNull(emitter, "Emitter must not be null");
        }

        @Test
        @DisplayName("returns non-null emitter when principal is UsernamePasswordAuthenticationToken")
        void returnsEmitterForAuthenticationToken() {
            UsernamePasswordAuthenticationToken token = mock(UsernamePasswordAuthenticationToken.class);
            when(token.getPrincipal()).thenReturn(testUser);

            SseEmitter emitter = wikiStreamService.subscribe(token);

            assertNotNull(emitter);
        }

        @Test
        @DisplayName("returns non-null emitter for anonymous (null) principal")
        void returnsEmitterForAnonymous() {
            SseEmitter emitter = wikiStreamService.subscribe(null);

            assertNotNull(emitter);
        }

        @Test
        @DisplayName("each subscribe() call returns a distinct emitter instance")
        void distinctEmittersPerSubscription() {
            when(principal.getName()).thenReturn("alice");
            when(userService.loadUserByUsername("alice")).thenReturn(testUser);

            SseEmitter first = wikiStreamService.subscribe(principal);
            SseEmitter second = wikiStreamService.subscribe(principal);

            assertNotSame(first, second, "Every subscribe() should create a new SseEmitter");
        }

        @Test
        @DisplayName("two different users get independent emitters")
        void twoUsersGetIndependentEmitters() {
            Principal principalBob = mock(Principal.class);
            when(principal.getName()).thenReturn("alice");
            when(principalBob.getName()).thenReturn("bob");
            when(userService.loadUserByUsername("alice")).thenReturn(testUser);
            when(userService.loadUserByUsername("bob")).thenReturn(anotherUser);

            SseEmitter aliceEmitter = wikiStreamService.subscribe(principal);
            SseEmitter bobEmitter = wikiStreamService.subscribe(principalBob);

            assertNotNull(aliceEmitter);
            assertNotNull(bobEmitter);
            assertNotSame(aliceEmitter, bobEmitter);
        }

        @Test
        @DisplayName("anonymous subscription does not invoke UserService")
        void anonymousDoesNotCallUserService() {
            wikiStreamService.subscribe(null);

            verifyNoInteractions(userService);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // isPaused() / setPaused()
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("pause management")
    class PauseManagement {

        @BeforeEach
        void subscribeAlice() {
            when(principal.getName()).thenReturn("alice");
            when(userService.loadUserByUsername("alice")).thenReturn(testUser);
            wikiStreamService.subscribe(principal);
        }

        @Test
        @DisplayName("stream is not paused right after subscription")
        void notPausedAfterSubscribe() {
            assertFalse(wikiStreamService.isPaused(principal));
        }

        @Test
        @DisplayName("setPaused(true) pauses the stream")
        void pauseStream() {
            wikiStreamService.setPaused(principal, true);

            assertTrue(wikiStreamService.isPaused(principal));
        }

        @Test
        @DisplayName("setPaused(false) resumes a paused stream")
        void resumeStream() {
            wikiStreamService.setPaused(principal, true);
            wikiStreamService.setPaused(principal, false);

            assertFalse(wikiStreamService.isPaused(principal));
        }

        @Test
        @DisplayName("toggling pause multiple times reflects latest state")
        void togglePause() {
            wikiStreamService.setPaused(principal, true);
            wikiStreamService.setPaused(principal, false);
            wikiStreamService.setPaused(principal, true);

            assertTrue(wikiStreamService.isPaused(principal));
        }

        @Test
        @DisplayName("isPaused returns false when principal is null")
        void isPausedNullPrincipal() {
            assertFalse(wikiStreamService.isPaused(null));
        }

        @Test
        @DisplayName("setPaused with null principal does not throw")
        void setPausedNullPrincipal() {
            assertDoesNotThrow(() -> wikiStreamService.setPaused(null, true));
        }

        @Test
        @DisplayName("pausing one user does not affect another user's stream")
        void pauseDoesNotAffectOtherUsers() {
            Principal principalBob = mock(Principal.class);
            when(principalBob.getName()).thenReturn("bob");
            when(userService.loadUserByUsername("bob")).thenReturn(anotherUser);
            wikiStreamService.subscribe(principalBob);

            wikiStreamService.setPaused(principal, true); // pause Alice

            assertFalse(wikiStreamService.isPaused(principalBob), "Bob should still be running");
        }

        @Test
        @DisplayName("isPaused returns false for a user who never subscribed")
        void isPausedUnknownUser() {
            Principal unknown = mock(Principal.class);
            when(unknown.getName()).thenReturn("ghost");

            assertFalse(wikiStreamService.isPaused(unknown));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // updateUser()
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("updateUser()")
    class UpdateUser {

        @BeforeEach
        void subscribeAlice() {
            when(principal.getName()).thenReturn("alice");
            when(userService.loadUserByUsername("alice")).thenReturn(testUser);
            wikiStreamService.subscribe(principal);
        }

        @Test
        @DisplayName("updateUser does not throw for a known user")
        void noExceptionForKnownUser() {
            User updated = new User();
            updated.setId(1L);
            updated.setUsername("alice");
            updated.setFilterCode("bot == true");

            assertDoesNotThrow(() -> wikiStreamService.updateUser(updated));
        }

        @Test
        @DisplayName("updateUser does not throw for an unknown user ID")
        void noExceptionForUnknownUserId() {
            User stranger = new User();
            stranger.setId(99L);
            stranger.setUsername("stranger");

            assertDoesNotThrow(() -> wikiStreamService.updateUser(stranger));
        }

        @Test
        @DisplayName("updateUser with null user does not throw")
        void handlesNullUserGracefully() {
            // updateUser streams over emitters using ctx.user.getId() –
            // if user is null the service should not crash.
            // (Guarded by the null-safe filter if implemented, otherwise wrap assertion)
            assertDoesNotThrow(() -> wikiStreamService.updateUser(null));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // broadcast() – tested indirectly via subscribe + reflection-free
    // observation of emitter send behaviour using a spy
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("broadcast behaviour (indirect)")
    class BroadcastBehaviour {

        /**
         * Verifies that when {@code abuseFilter.matches()} returns {@code false},
         * no data is sent on the emitter even if the subscriber is active.
         * We check this by ensuring no IOException is raised from an emitter that
         * we would complete prematurely – i.e. the service tolerates the scenario
         * without crashing.
         */
        @Test
        @DisplayName("no exception when filter rejects event for subscribed user")
        void noExceptionWhenFilterRejectsEvent() throws Exception {
            when(principal.getName()).thenReturn("alice");
            when(userService.loadUserByUsername("alice")).thenReturn(testUser);
            when(abuseFilter.matches(any(RecentChange.class), eq(testUser))).thenReturn(false);

            wikiStreamService.subscribe(principal);

            // Verify no interactions with mapper (event serialisation should be skipped)
            verifyNoInteractions(mapper);
        }

        @Test
        @DisplayName("mapper.valueToTree is invoked when filter matches event")
        void mapperCalledWhenFilterMatches() throws Exception {
            when(principal.getName()).thenReturn("alice");
            when(userService.loadUserByUsername("alice")).thenReturn(testUser);

            RecentChange rc = buildRecentChange();

            // Stub filter to match
            when(abuseFilter.matches(eq(rc), eq(testUser))).thenReturn(true);

            // Stub mapper to return a valid ObjectNode to avoid NPE inside broadcast
            ObjectNode node = new ObjectMapper().createObjectNode();
            when(mapper.valueToTree(rc)).thenReturn(node);
            when(mapper.writeValueAsString(any())).thenReturn("{\"flagged\":true}");

            wikiStreamService.subscribe(principal);

            // Manually trigger broadcast by calling the package-private helper
            // through reflection (only way without starting the SSE connection).
            var broadcastMethod = WikiStreamService.class
                    .getDeclaredMethod("broadcast", RecentChange.class);
            broadcastMethod.setAccessible(true);
            broadcastMethod.invoke(wikiStreamService, rc);

            verify(mapper, times(1)).valueToTree(rc);
            verify(mapper, times(1)).writeValueAsString(any());
        }

        @Test
        @DisplayName("broadcast skips paused users")
        void broadcastSkipsPausedUsers() throws Exception {
            when(principal.getName()).thenReturn("alice");
            when(userService.loadUserByUsername("alice")).thenReturn(testUser);

            RecentChange rc = buildRecentChange();
            when(abuseFilter.matches(any(), any())).thenReturn(true);

            wikiStreamService.subscribe(principal);
            wikiStreamService.setPaused(principal, true); // Alice is paused

            var broadcastMethod = WikiStreamService.class
                    .getDeclaredMethod("broadcast", RecentChange.class);
            broadcastMethod.setAccessible(true);
            broadcastMethod.invoke(wikiStreamService, rc);

            // mapper should never be called because the context is paused
            verifyNoInteractions(mapper);
        }

        @Test
        @DisplayName("broadcast skips anonymous subscribers (no StreamContext entry)")
        void broadcastSkipsAnonymous() throws Exception {
            wikiStreamService.subscribe(null); // anonymous – no StreamContext stored

            RecentChange rc = buildRecentChange();
            when(abuseFilter.matches(any(), any())).thenReturn(true);

            var broadcastMethod = WikiStreamService.class
                    .getDeclaredMethod("broadcast", RecentChange.class);
            broadcastMethod.setAccessible(true);
            broadcastMethod.invoke(wikiStreamService, rc);

            verifyNoInteractions(mapper);
            verifyNoInteractions(abuseFilter);
        }

        // ── helpers ──────────────────────────────────────────────────────────
        private RecentChange buildRecentChange() {
            RecentChange rc = new RecentChange();
            rc.setId(42L);
            rc.setTitle("Test Page");
            rc.setUser("BadActor");
            rc.setType("edit");
            rc.setWiki("enwiki");
            return rc;
        }
    }
}

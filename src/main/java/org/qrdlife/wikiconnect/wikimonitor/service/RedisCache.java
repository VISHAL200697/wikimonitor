package org.qrdlife.wikiconnect.wikimonitor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCache {

    // Atomic RPUSH + LTRIM so concurrent writers can never observe a list larger than maxSize.
    private static final RedisScript<Void> APPEND_TRIM_SCRIPT = new DefaultRedisScript<>(
            "redis.call('RPUSH', KEYS[1], ARGV[1]) " +
            "local max = tonumber(ARGV[2]) " +
            "if max > 0 then redis.call('LTRIM', KEYS[1], -max, -1) end",
            Void.class
    );

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public void appendToList(String key, Object value, int maxSize) {
        String json;
        try {
            json = mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize cache value for key [{}]: {}", key, e.getMessage());
            return;
        }
        try {
            redis.execute(APPEND_TRIM_SCRIPT, Collections.singletonList(key), json, String.valueOf(maxSize));
        } catch (Exception e) {
            log.warn("Redis append/trim failed for key [{}]: {}", key, e.getMessage());
        }
    }

    public List<JsonNode> rangeFromList(String key) {
        List<String> entries;
        try {
            entries = redis.opsForList().range(key, 0, -1);
        } catch (Exception e) {
            log.warn("Redis LRANGE failed for key [{}]: {}", key, e.getMessage());
            return Collections.emptyList();
        }
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }
        List<JsonNode> out = new ArrayList<>(entries.size());
        for (String json : entries) {
            try {
                out.add(mapper.readTree(json));
            } catch (Exception e) {
                log.warn("Skipping malformed cache entry: {}", e.getMessage());
            }
        }
        return out;
    }
}

package org.qrdlife.wikiconnect.wikimonitor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCache {

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public <T> T get(String key, Class<T> type) {
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return mapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("Redis GET failed for key [{}]: {}", key, e.getMessage());
            return null;
        }
    }

    public void set(String key, Object value, Duration ttl) {
        try {
            String json = mapper.writeValueAsString(value);
            redis.opsForValue().set(key, json, ttl);
        } catch (Exception e) {
            log.warn("Redis SET failed for key [{}]: {}", key, e.getMessage());
        }
    }

    public void appendToList(String key, Object value, int maxSize) {
        try {
            String json = mapper.writeValueAsString(value);
            redis.opsForList().rightPush(key, json);
            if (maxSize > 0) {
                redis.opsForList().trim(key, -maxSize, -1);
            }
        } catch (Exception e) {
            log.warn("Redis RPUSH failed for key [{}]: {}", key, e.getMessage());
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

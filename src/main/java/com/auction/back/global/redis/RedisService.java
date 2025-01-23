package com.auction.back.global.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    // 키/값 저장 (만료시간 적용)
    public void setValue(String key, String value, long timeoutSeconds) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        ops.set(key, value, Duration.ofSeconds(timeoutSeconds));
    }

    // 값 조회
    public String getValue(String key) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        return ops.get(key);
    }

    // 값 삭제
    public void deleteValue(String key) {
        redisTemplate.delete(key);
    }
}

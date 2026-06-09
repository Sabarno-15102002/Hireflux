package com.sabarno.hireflux.service.impl;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.sabarno.hireflux.dto.response.KafkaMetricsResponse;
import com.sabarno.hireflux.service.MetricsService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MetricsServiceImpl implements MetricsService {

    private static final String SUCCESS_KEY = "metrics:kafka:resume:success";

    private static final String FAILED_KEY = "metrics:kafka:resume:failed";

    private static final String RETRY_KEY = "metrics:kafka:resume:retry";

    private static final String DLQ_KEY = "metrics:kafka:resume:dlq";

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void incrementResumeSuccess() {
        redisTemplate.opsForValue().increment(SUCCESS_KEY, 1);
    }

    @Override
    public void incrementResumeFailure() {
        redisTemplate.opsForValue().increment(FAILED_KEY, 1);
    }

    @Override
    public void incrementResumeRetry() {
        redisTemplate.opsForValue().increment(RETRY_KEY, 1);
    }

    @Override
    public void incrementResumeDlq() {
        redisTemplate.opsForValue().increment(DLQ_KEY, 1);
    }

    @Override
    public void recordResumeProcessingTime(long ms) {

        redisTemplate.opsForList().rightPush(
                "metrics:kafka:resume:processing-times",
                String.valueOf(ms)
        );
    }

    @Override
    public KafkaMetricsResponse getKafkaMetrics() {

        return new KafkaMetricsResponse(
                getLong(SUCCESS_KEY),
                getLong(FAILED_KEY),
                getLong(RETRY_KEY),
                getLong(DLQ_KEY)
        );
    }

    private long getLong(String key) {

        String value = redisTemplate.opsForValue().get(key);
        return value == null ? 0 : Long.parseLong(value);
    }

}

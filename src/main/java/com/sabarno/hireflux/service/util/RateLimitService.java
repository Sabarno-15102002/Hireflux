package com.sabarno.hireflux.service.util;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.sabarno.hireflux.exception.impl.BadRequestException;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

@Service
public class RateLimitService {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public Bucket resolveBucket(
            String key,
            long capacity,
            Duration duration
    ) {
        if(capacity <= 0) throw new BadRequestException("Capacity must be positive");
        return cache.computeIfAbsent(
                key,
                k -> createBucket(capacity, duration)
        );
    }

    private Bucket createBucket(long capacity, Duration duration) {

        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, duration)
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
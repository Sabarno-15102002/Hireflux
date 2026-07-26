package com.sabarno.hireflux.service.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.sabarno.hireflux.exception.impl.BadRequestException;

import io.github.bucket4j.Bucket;

class RateLimitServiceTest {
 
    private final RateLimitService rateLimitService = new RateLimitService();
 
    @Test
    void testResolveBucket_shouldReturnSameBucketInstance_forSameKey() {
        Bucket bucket1 = rateLimitService.resolveBucket("user-123", 5, Duration.ofMinutes(1));
        Bucket bucket2 = rateLimitService.resolveBucket("user-123", 5, Duration.ofMinutes(1));
 
        assertSame(bucket1, bucket2);
    }
 
    @Test
    void testResolveBucket_shouldReturnDifferentBucketInstances_forDifferentKeys() {
        Bucket bucketA = rateLimitService.resolveBucket("user-A", 5, Duration.ofMinutes(1));
        Bucket bucketB = rateLimitService.resolveBucket("user-B", 5, Duration.ofMinutes(1));
 
        assertNotSame(bucketA, bucketB);
    }
 
    @Test
    void testResolveBucket_shouldIgnoreCapacityAndDuration_onSubsequentCallsForSameKey() {
        Bucket firstBucket = rateLimitService.resolveBucket("user-123", 2, Duration.ofMinutes(1));
        Bucket secondBucket = rateLimitService.resolveBucket("user-123", 100, Duration.ofMinutes(1));
 
        assertSame(firstBucket, secondBucket);
 
        assertTrue(secondBucket.tryConsume(1));
        assertTrue(secondBucket.tryConsume(1));
        assertFalse(secondBucket.tryConsume(1));
    }
 
    @Test
    void testResolveBucket_shouldAllowConsumptionUpToConfiguredCapacity() {
        Bucket bucket = rateLimitService.resolveBucket("user-cap-3", 3, Duration.ofMinutes(1));
 
        assertTrue(bucket.tryConsume(1));
        assertTrue(bucket.tryConsume(1));
        assertTrue(bucket.tryConsume(1));
        assertFalse(bucket.tryConsume(1));
    }
 
    @Test
    void testResolveBucket_shouldDenyConsumption_whenCapacityIsZero() {
        BadRequestException exception = assertThrows(
            BadRequestException.class,
                        () -> rateLimitService.resolveBucket("user-cap-3", 0, Duration.ofMinutes(1)) 
        );

        assertEquals("Capacity must be positive", exception.getMessage());
    }
 
    @Test
    void testResolveBucket_shouldTrackAvailableTokens_asBucketIsConsumed() {
        Bucket bucket = rateLimitService.resolveBucket("user-tokens", 5, Duration.ofMinutes(1));
 
        assertEquals(5, bucket.getAvailableTokens());
        bucket.tryConsume(2);
        assertEquals(3, bucket.getAvailableTokens());
    }
 
    @Test
    void testResolveBucket_shouldBeThreadSafe_whenSameKeyRequestedConcurrently() throws InterruptedException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger distinctInstanceCount = new AtomicInteger(0);
        Bucket[] results = new Bucket[threadCount];
 
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                results[index] = rateLimitService.resolveBucket("shared-key", 10, Duration.ofMinutes(1));
                latch.countDown();
            });
        }
 
        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
 
        Bucket first = results[0];
        for (Bucket b : results) {
            if (b != first) {
                distinctInstanceCount.incrementAndGet();
            }
        }

        assertEquals(0, distinctInstanceCount.get());
    }
 
    @Test
    void testResolveBucket_shouldCreateIndependentBuckets_thatDoNotShareTokens() {
        Bucket bucketA = rateLimitService.resolveBucket("independent-A", 2, Duration.ofMinutes(1));
        Bucket bucketB = rateLimitService.resolveBucket("independent-B", 2, Duration.ofMinutes(1));
 
        assertTrue(bucketA.tryConsume(1));
        assertTrue(bucketA.tryConsume(1));
        assertFalse(bucketA.tryConsume(1));
 
        // B is unaffected by A's consumption
        assertTrue(bucketB.tryConsume(1));
        assertTrue(bucketB.tryConsume(1));
        assertFalse(bucketB.tryConsume(1));
    }
}
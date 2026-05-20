package com.sabarno.hireflux.utility;

import com.sabarno.hireflux.exception.impl.RateLimitExceededException;

import io.github.bucket4j.Bucket;

public class RateLimitUtil {
    private RateLimitUtil() {
        /* This utility class should not be instantiated */
    }


    public static void consume(Bucket bucket, String message) {

        if (!bucket.tryConsume(1)) {
            throw new RateLimitExceededException(message);
        }
    }
}
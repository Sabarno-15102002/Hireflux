package com.sabarno.hireflux.utility;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sabarno.hireflux.exception.impl.RateLimitExceededException;

import io.github.bucket4j.Bucket;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitUtilTest {
 
    @Mock
    private Bucket bucket;
 
    @Test
    void testConsume_shouldNotThrow_whenBucketHasCapacity() {
        when(bucket.tryConsume(1)).thenReturn(true);
 
        assertDoesNotThrow(() -> RateLimitUtil.consume(bucket, "Too many requests"));
 
        verify(bucket).tryConsume(1);
    }
 
    @Test
    void testConsume_shouldThrowRateLimitExceededException_whenBucketIsExhausted() {
        when(bucket.tryConsume(1)).thenReturn(false);
 
        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> RateLimitUtil.consume(bucket, "Too many requests")
        );
 
        assertEquals("Too many requests", exception.getMessage());
    }
 
    @Test
    void testConsume_shouldAlwaysRequestExactlyOneToken() {
        when(bucket.tryConsume(1)).thenReturn(true);
 
        RateLimitUtil.consume(bucket, "message");
 
        verify(bucket, times(1)).tryConsume(1);
        verifyNoMoreInteractions(bucket);
    }
 
    @Test
    void testConsume_shouldPropagateExactMessage_intoException() {
        when(bucket.tryConsume(1)).thenReturn(false);
 
        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> RateLimitUtil.consume(bucket, "Custom rate limit message for endpoint X")
        );
 
        assertEquals("Custom rate limit message for endpoint X", exception.getMessage());
    }
 
    @Test
    void testConsume_shouldPropagateNullMessage_ifProvided() {
        when(bucket.tryConsume(1)).thenReturn(false);
 
        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> RateLimitUtil.consume(bucket, null)
        );
 
        assertNull(exception.getMessage());
    }
 
    @Test
    void testConstructor_shouldBePrivate_toPreventInstantiation() throws Exception {
        Constructor<RateLimitUtil> constructor = RateLimitUtil.class.getDeclaredConstructor();
 
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
    }
 
    @Test
    void testConstructor_shouldNotThrow_whenInvokedReflectively() throws Exception {
        Constructor<RateLimitUtil> constructor = RateLimitUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
 
        assertDoesNotThrow(() -> {
            try {
                constructor.newInstance();
            } catch (InvocationTargetException e) {
                throw e.getCause() instanceof RuntimeException ? (RuntimeException) e.getCause() : new RuntimeException(e.getCause());
            }
        });
    }
}
 

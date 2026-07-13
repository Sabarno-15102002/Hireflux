package com.sabarno.hireflux.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.sabarno.hireflux.dto.response.KafkaMetricsResponse;
import com.sabarno.hireflux.service.impl.MetricsServiceImpl;

@ExtendWith(MockitoExtension.class)
class MetricsServiceImplTest {

    private static final String SUCCESS_KEY = "metrics:kafka:resume:success";
    private static final String FAILED_KEY = "metrics:kafka:resume:failed";
    private static final String RETRY_KEY = "metrics:kafka:resume:retry";
    private static final String DLQ_KEY = "metrics:kafka:resume:dlq";
    private static final String PROCESSING_TIMES_KEY = "metrics:kafka:resume:processing-times";
 
    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;
 
    @Mock
    private ListOperations<String, String> listOperations;

    @InjectMocks
    private MetricsServiceImpl metricsService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForList()).thenReturn(listOperations);
    }

    @Test
    void testIncrementResumeSuccess() {
        metricsService.incrementResumeSuccess();
        verify(valueOperations).increment(SUCCESS_KEY, 1);
        verifyNoInteractions(listOperations);
    }

    @Test
    void testIncrementResumeFailure() {
        metricsService.incrementResumeFailure();
        verify(valueOperations).increment(FAILED_KEY, 1);
        verifyNoInteractions(listOperations);
    }

    @Test
    void testIncrementResumeRetry() {
        metricsService.incrementResumeRetry();
        verify(valueOperations).increment(RETRY_KEY, 1);
        verifyNoInteractions(listOperations);
    }

    @Test
    void testIncrementResumeDlq() {
        metricsService.incrementResumeDlq();
        verify(valueOperations).increment(DLQ_KEY, 1);
        verifyNoInteractions(listOperations);
    }

    @Test
    void testRecordResumeProcessingTime() {
        long processingTime = 123L;
        metricsService.recordResumeProcessingTime(processingTime);
        verify(listOperations).rightPush(PROCESSING_TIMES_KEY, String.valueOf(processingTime));
        verifyNoInteractions(valueOperations);
    }

    @Test
    void testGetKafkaMetrics_KeysExist() {
        when(valueOperations.get(SUCCESS_KEY)).thenReturn("100");
        when(valueOperations.get(FAILED_KEY)).thenReturn("5");
        when(valueOperations.get(RETRY_KEY)).thenReturn("3");
        when(valueOperations.get(DLQ_KEY)).thenReturn("1");
 
        KafkaMetricsResponse response = metricsService.getKafkaMetrics();
 
        assertNotNull(response);
        assertEquals(100L, response.getSuccessfulProcesses());
        assertEquals(5L, response.getFailedProcesses());
        assertEquals(3L, response.getRetryCount());
        assertEquals(1L, response.getDlqCount());
    }

    @Test
    void testGetKafkaMetrics_KeysDoNotExist() {
        when(valueOperations.get(anyString())).thenReturn(null);
 
        KafkaMetricsResponse response = metricsService.getKafkaMetrics();
 
        assertNotNull(response);
        assertEquals(0L, response.getSuccessfulProcesses());
        assertEquals(0L, response.getFailedProcesses());
        assertEquals(0L, response.getRetryCount());
        assertEquals(0L, response.getDlqCount());
    }
}

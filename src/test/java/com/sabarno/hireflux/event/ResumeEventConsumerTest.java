package com.sabarno.hireflux.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sabarno.hireflux.dto.event.ResumeUploadedEvent;
import com.sabarno.hireflux.exception.NonRetryableProcessingException;
import com.sabarno.hireflux.service.MetricsService;
import com.sabarno.hireflux.service.ResumeService;

@ExtendWith(MockitoExtension.class)
class ResumeEventConsumerTest {
 
    @Mock
    private ResumeService resumeService;
 
    @Mock
    private MetricsService metricsService;
 
    private ResumeEventConsumer consumer;
 
    private ResumeUploadedEvent event;
 
    @BeforeEach
    void setUp() {
        consumer = new ResumeEventConsumer(resumeService, metricsService);
        event = new ResumeUploadedEvent(UUID.randomUUID(), "uploads/resume123.pdf");
    }
 
    @Test
    void testConsumeResumeUploaded_shouldProcessResume_whenNoExceptionOccurs() {
        consumer.consumeResumeUploaded(event);
 
        verify(resumeService).processResume(event.getResumeId(), event.getFileKey());
        verifyNoInteractions(metricsService);
    }
 
    @Test
    void testConsumeResumeUploaded_shouldIncrementFailureMetric_andRethrow_onNonRetryableProcessingException() {
        NonRetryableProcessingException nonRetryable =
                new NonRetryableProcessingException("Invalid resume format");
        doThrow(nonRetryable).when(resumeService)
                .processResume(event.getResumeId(), event.getFileKey());
 
        NonRetryableProcessingException thrown = assertThrows(
                NonRetryableProcessingException.class,
                () -> consumer.consumeResumeUploaded(event)
        );
 
        assertSame(nonRetryable, thrown);
        verify(metricsService).incrementResumeFailure();
        verify(metricsService, never()).incrementResumeRetry();
    }
 
    @Test
    void testConsumeResumeUploaded_shouldIncrementRetryMetric_andRethrow_onUnexpectedException() {
        RuntimeException unexpected = new RuntimeException("Temporary S3 outage");
        doThrow(unexpected).when(resumeService)
                .processResume(event.getResumeId(), event.getFileKey());
 
        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> consumer.consumeResumeUploaded(event)
        );
 
        assertSame(unexpected, thrown);
        verify(metricsService).incrementResumeRetry();
        verify(metricsService, never()).incrementResumeFailure();
    }
 
    @Test
    void testConsumeResumeUploaded_shouldTreatOtherRuntimeExceptionSubtypes_asUnexpected() {
        IllegalStateException unexpected = new IllegalStateException("Something broke");
        doThrow(unexpected).when(resumeService)
                .processResume(event.getResumeId(), event.getFileKey());
 
        assertThrows(IllegalStateException.class, () -> consumer.consumeResumeUploaded(event));
 
        verify(metricsService).incrementResumeRetry();
        verify(metricsService, never()).incrementResumeFailure();
    }
 
    @Test
    void testConsumeResumeUploaded_shouldPassEventFieldsThrough_toResumeService() {
        UUID resumeId = UUID.randomUUID();
        ResumeUploadedEvent customEvent = new ResumeUploadedEvent(resumeId, "uploads/custom.pdf");
 
        consumer.consumeResumeUploaded(customEvent);
 
        verify(resumeService).processResume(resumeId, "uploads/custom.pdf");
    }
 
    @Test
    void testHandleDlt_shouldIncrementDlqMetric() {
        consumer.handleDlt(event);
 
        verify(metricsService).incrementResumeDlq();
    }
 
    @Test
    void testHandleDlt_shouldNotInteractWithResumeService() {
        consumer.handleDlt(event);
 
        verifyNoInteractions(resumeService);
    }
 
    @Test
    void testHandleDlt_shouldNotThrow_evenIfCalledRepeatedly() {
        assertDoesNotThrow(() -> {
            consumer.handleDlt(event);
            consumer.handleDlt(event);
        });
 
        verify(metricsService, times(2)).incrementResumeDlq();
    }
}

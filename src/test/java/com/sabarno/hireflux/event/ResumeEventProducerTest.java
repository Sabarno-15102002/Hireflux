package com.sabarno.hireflux.event;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import com.sabarno.hireflux.dto.event.ResumeUploadedEvent;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeEventProducerTest {
 
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
 
    private ResumeEventProducer producer;
 
    private static final String TOPIC = "resume-uploaded-topic";
 
    @BeforeEach
    void setUp() {
        producer = new ResumeEventProducer(kafkaTemplate);
        ReflectionTestUtils.setField(producer, "resumeUploadTopicName", TOPIC);
    }
 
    @Test
    void testPublishResumeUploaded_shouldSendEventToConfiguredTopic_withResumeIdAsKey() {
        UUID resumeId = UUID.randomUUID();
        String fileKey = "uploads/resume123.pdf";
 
        SendResult<String, Object> sendResult = buildSendResult(0);
        when(kafkaTemplate.send(eq(TOPIC), eq(resumeId.toString()), any()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));
 
        producer.publishResumeUploaded(resumeId, fileKey);
 
        verify(kafkaTemplate).send(eq(TOPIC), eq(resumeId.toString()), any(ResumeUploadedEvent.class));
    }
 
    @Test
    void testPublishResumeUploaded_shouldSendEventWithCorrectResumeIdAndFileKey() {
        UUID resumeId = UUID.randomUUID();
        String fileKey = "uploads/resume123.pdf";
 
        SendResult<String, Object> sendResult = buildSendResult(0);
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));
 
        ArgumentCaptor<ResumeUploadedEvent> eventCaptor = ArgumentCaptor.forClass(ResumeUploadedEvent.class);
        producer.publishResumeUploaded(resumeId, fileKey);
 
        verify(kafkaTemplate).send(eq(TOPIC), eq(resumeId.toString()), eventCaptor.capture());
        ResumeUploadedEvent sentEvent = eventCaptor.getValue();
 
        assertEquals(resumeId, sentEvent.getResumeId());
        assertEquals(fileKey, sentEvent.getFileKey());
    }
 
    @Test
    void testPublishResumeUploaded_shouldCompleteWithoutThrowing_whenSendSucceeds() {
        UUID resumeId = UUID.randomUUID();
        SendResult<String, Object> sendResult = buildSendResult(2);
 
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));
 
        assertDoesNotThrow(() -> producer.publishResumeUploaded(resumeId, "uploads/resume.pdf"));
    }
 
    @Test
    void testPublishResumeUploaded_shouldUseResumeIdAsPartitioningKey_forDifferentResumes() {
        UUID resumeId1 = UUID.randomUUID();
        UUID resumeId2 = UUID.randomUUID();
 
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(buildSendResult(0)));
 
        producer.publishResumeUploaded(resumeId1, "uploads/a.pdf");
        producer.publishResumeUploaded(resumeId2, "uploads/b.pdf");
 
        verify(kafkaTemplate).send(eq(TOPIC), eq(resumeId1.toString()), any());
        verify(kafkaTemplate).send(eq(TOPIC), eq(resumeId2.toString()), any());
    }
 
    private SendResult<String, Object> buildSendResult(int partition) {
        TopicPartition topicPartition = new TopicPartition(TOPIC, partition);
        RecordMetadata metadata = new RecordMetadata(topicPartition, 0, 0, 0, 0, 0);
        return new SendResult<>(null, metadata);
    }

    @Test
    void testPublishResumeUploaded_shouldLogError_whenSendFails() {
        UUID resumeId = UUID.randomUUID();
        RuntimeException kafkaFailure = new RuntimeException("Broker unavailable");
 
        CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(kafkaFailure);
 
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(failedFuture);
 
        Logger logger = (Logger) LoggerFactory.getLogger(ResumeEventProducer.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
 
        try {
            assertDoesNotThrow(() -> producer.publishResumeUploaded(resumeId, "uploads/resume.pdf"));
 
            boolean errorLogged = listAppender.list.stream().anyMatch(event ->
                    event.getLevel() == Level.ERROR
                            && event.getFormattedMessage().contains("publish_resume_uploaded_failed")
                            && event.getFormattedMessage().contains(resumeId.toString()));
 
            assertTrue(errorLogged, "Expected an ERROR log for the failed publish attempt");
        } finally {
            logger.detachAppender(listAppender);
        }
    }
}
package com.sabarno.hireflux.event;

import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.sabarno.hireflux.dto.event.ResumeUploadedEvent;
import com.sabarno.hireflux.exception.NonRetryableProcessingException;
import com.sabarno.hireflux.service.MetricsService;
import com.sabarno.hireflux.service.ResumeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResumeEventConsumer {

    private final ResumeService resumeService;

    private final MetricsService metricsService;

    @KafkaListener(
            topics = "${kafka.topic.resume-uploaded.name}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeResumeUploaded(ResumeUploadedEvent event) {
        log.info("Processing resume event: {}", event.getResumeId());

        try {
            resumeService.processResume(
                event.getResumeId(),
                event.getFileKey()
            );
        } catch (NonRetryableProcessingException e) {
            metricsService.incrementResumeFailure();
            log.error("Resume processing failed for resumeId={}", event.getResumeId(), e);
            throw e; // Let the exception propagate to trigger retry and DLT handling
        }
        catch (Exception e) {
            metricsService.incrementResumeRetry();
            log.error("Unexpected error while processing resumeId={}", event.getResumeId(), e);
            throw e; // Let the exception propagate to trigger retry and DLT handling
        }
    }

    @DltHandler
    public void handleDlt(ResumeUploadedEvent event) {

        log.error(
                "Message moved to DLT for resumeId={}",
                event.getResumeId()
        );
        metricsService.incrementResumeDlq();
    }
}
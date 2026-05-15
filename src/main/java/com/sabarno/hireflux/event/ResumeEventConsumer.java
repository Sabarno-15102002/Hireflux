package com.sabarno.hireflux.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.sabarno.hireflux.dto.event.ResumeUploadedEvent;
import com.sabarno.hireflux.service.ResumeService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ResumeEventConsumer {

    @Autowired
    private ResumeService resumeService;

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
        } catch (Exception e) {
            log.error("Resume processing failed for resumeId={}", event.getResumeId(), e);
        }
    }

    @DltHandler
    public void handleDlt(ResumeUploadedEvent event) {

        log.error(
                "Message moved to DLT for resumeId={}",
                event.getResumeId()
        );

        // optional:
        // notify admin
        // save failure metadata
        // trigger alert
    }
}
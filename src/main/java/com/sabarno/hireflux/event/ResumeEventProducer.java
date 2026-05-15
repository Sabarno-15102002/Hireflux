package com.sabarno.hireflux.event;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.sabarno.hireflux.dto.event.ResumeUploadedEvent;

@Service
public class ResumeEventProducer {

    @Value("${kafka.topic.resume-uploaded.name}")
    private String resumeUploadTopicName;

    @Autowired
    private KafkaTemplate<String, ResumeUploadedEvent> kafkaTemplate;

    public void publishResumeUploaded(UUID resumeId, String fileKey) {
        ResumeUploadedEvent event = new ResumeUploadedEvent(resumeId, fileKey);
        kafkaTemplate.send(resumeUploadTopicName, resumeId.toString(), event);
    }
}
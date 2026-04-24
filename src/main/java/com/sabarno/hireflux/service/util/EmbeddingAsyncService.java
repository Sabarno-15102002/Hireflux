package com.sabarno.hireflux.service.util;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabarno.hireflux.exception.impl.BadRequestException;
import com.sabarno.hireflux.repository.JobRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmbeddingAsyncService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private ObjectMapper objectMapper;

    @Async
    public void generateAndSaveEmbedding(UUID jobId, String jobText) {
        try {
            List<Double> embedding = embeddingService.createEmbedding(jobText);

            String embeddingJson = toJson(embedding);

            jobRepository.findById(jobId).ifPresent(job -> {
                job.setEmbedding(embeddingJson);
                jobRepository.save(job);
            });

        } catch (BadRequestException e) {
            // log properly (don’t fail user flow)
            log.error("Embedding generation failed for jobId: {}", jobId, e);
        }
    }

    private String toJson(List<Double> embedding) throws BadRequestException {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (Exception e) {
            throw new BadRequestException("Error serializing embedding");
        }
    }
}
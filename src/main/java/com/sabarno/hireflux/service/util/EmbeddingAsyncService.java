package com.sabarno.hireflux.service.util;

import java.util.List;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabarno.hireflux.exception.impl.BadRequestException;
import com.sabarno.hireflux.repository.JobRepository;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmbeddingAsyncService {

    private final JobRepository jobRepository;

    private final EmbeddingService embeddingService;

    private final ObjectMapper objectMapper;

    private final MeterRegistry meterRegistry;

    @Async
    public void generateAndSaveEmbedding(UUID jobId, String jobText) {
        Timer.Sample sample = Timer.start(meterRegistry);
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
        finally {
            sample.stop(meterRegistry.timer("embedding.generation.time"));
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
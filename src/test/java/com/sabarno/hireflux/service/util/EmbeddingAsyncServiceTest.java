package com.sabarno.hireflux.service.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabarno.hireflux.entity.Job;
import com.sabarno.hireflux.repository.JobRepository;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class EmbeddingAsyncServiceTest {
 
    @Mock
    private JobRepository jobRepository;
 
    @Mock
    private EmbeddingService embeddingService;
 
    @Mock
    private ObjectMapper objectMapper;
 
    private MeterRegistry meterRegistry;
 
    private EmbeddingAsyncService embeddingAsyncService;
 
    private UUID jobId;
    private Job job;
 
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        embeddingAsyncService = new EmbeddingAsyncService(
                jobRepository, embeddingService, objectMapper, meterRegistry);
 
        jobId = UUID.randomUUID();
        job = new Job();
        job.setId(jobId);
    }
 
    @Test
    void testGenerateAndSaveEmbedding_shouldSaveEmbeddingOnJob_whenJobExists() throws Exception {
        List<Double> embedding = List.of(0.1, 0.2, 0.3);
        when(embeddingService.createEmbedding("some job text")).thenReturn(embedding);
        when(objectMapper.writeValueAsString(embedding)).thenReturn("[0.1,0.2,0.3]");
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
 
        embeddingAsyncService.generateAndSaveEmbedding(jobId, "some job text");
 
        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(jobCaptor.capture());
        assertEquals("[0.1,0.2,0.3]", jobCaptor.getValue().getEmbedding());
    }
 
    @Test
    void testGenerateAndSaveEmbedding_shouldDoNothing_whenJobNoLongerExists() throws Exception {
        List<Double> embedding = List.of(0.1, 0.2, 0.3);
        when(embeddingService.createEmbedding("some job text")).thenReturn(embedding);
        when(objectMapper.writeValueAsString(embedding)).thenReturn("[0.1,0.2,0.3]");
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());
 
        assertDoesNotThrow(() ->
                embeddingAsyncService.generateAndSaveEmbedding(jobId, "some job text"));
 
        verify(jobRepository, never()).save(any());
    }
 
    @Test
    void testGenerateAndSaveEmbedding_shouldSwallowBadRequestException_fromJsonSerializationFailure() throws Exception {
        List<Double> embedding = List.of(0.1, 0.2, 0.3);
        when(embeddingService.createEmbedding("some job text")).thenReturn(embedding);
        when(objectMapper.writeValueAsString(embedding)).thenThrow(new RuntimeException("serialization boom"));
 
        // toJson() wraps the RuntimeException into a BadRequestException,
        // which generateAndSaveEmbedding's own catch block then swallows
        // entirely -- the method should complete normally, not propagate.
        assertDoesNotThrow(() ->
                embeddingAsyncService.generateAndSaveEmbedding(jobId, "some job text"));
 
        verifyNoInteractions(jobRepository);
    }
 
    @Test
    void testGenerateAndSaveEmbedding_shouldNotQueryJobRepository_whenSerializationFails() throws Exception {
        List<Double> embedding = List.of(0.1, 0.2, 0.3);
        when(embeddingService.createEmbedding("some job text")).thenReturn(embedding);
        when(objectMapper.writeValueAsString(embedding)).thenThrow(new RuntimeException("serialization boom"));
 
        embeddingAsyncService.generateAndSaveEmbedding(jobId, "some job text");
 
        verify(jobRepository, never()).findById(any());
        verify(jobRepository, never()).save(any());
    }
 
    @Test
    void testGenerateAndSaveEmbedding_shouldPropagateException_whenEmbeddingServiceThrowsUnchecked() {
        when(embeddingService.createEmbedding("some job text"))
                .thenThrow(new RuntimeException("OpenAI API unavailable"));
 
        assertThrows(RuntimeException.class, () ->
                embeddingAsyncService.generateAndSaveEmbedding(jobId, "some job text"));
 
        verifyNoInteractions(jobRepository);
    }
 
    @Test
    void testGenerateAndSaveEmbedding_shouldRecordTimerMetric_onSuccess() throws Exception {
        List<Double> embedding = List.of(0.1, 0.2, 0.3);
        when(embeddingService.createEmbedding("some job text")).thenReturn(embedding);
        when(objectMapper.writeValueAsString(embedding)).thenReturn("[0.1,0.2,0.3]");
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
 
        embeddingAsyncService.generateAndSaveEmbedding(jobId, "some job text");
 
        assertEquals(1, meterRegistry.timer("embedding.generation.time").count());
    }
 
    @Test
    void testGenerateAndSaveEmbedding_shouldRecordTimerMetric_evenWhenSerializationFails() throws Exception {
        // The finally block ensures the timer stops regardless of outcome.
        List<Double> embedding = List.of(0.1, 0.2, 0.3);
        when(embeddingService.createEmbedding("some job text")).thenReturn(embedding);
        when(objectMapper.writeValueAsString(embedding)).thenThrow(new RuntimeException("boom"));
 
        embeddingAsyncService.generateAndSaveEmbedding(jobId, "some job text");
 
        assertEquals(1, meterRegistry.timer("embedding.generation.time").count());
    }
 
    @Test
    void testGenerateAndSaveEmbedding_shouldPassExactJobTextToEmbeddingService() throws Exception {
        when(embeddingService.createEmbedding(anyString())).thenReturn(List.of(1.0));
        when(objectMapper.writeValueAsString(any())).thenReturn("[1.0]");
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
 
        embeddingAsyncService.generateAndSaveEmbedding(jobId, "Backend Engineer role description");
 
        verify(embeddingService).createEmbedding("Backend Engineer role description");
    }
}
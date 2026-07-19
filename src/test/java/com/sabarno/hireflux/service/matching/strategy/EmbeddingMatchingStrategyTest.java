package com.sabarno.hireflux.service.matching.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabarno.hireflux.entity.Job;
import com.sabarno.hireflux.entity.Resume;
import com.sabarno.hireflux.service.matching.MatchContext;
import com.sabarno.hireflux.utility.enums.ResumeUploadStatus;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class EmbeddingMatchingStrategyTest {
 
    @Mock
    private ObjectMapper objectMapper;
 
    @Mock
    private MatchContext context;
 
    @Mock
    private Resume resume;
 
    @Mock
    private Job job;
 
    private EmbeddingMatchingStrategy strategy;
 
    @BeforeEach
    void setUp() {
        strategy = new EmbeddingMatchingStrategy(objectMapper);
 
        lenient().when(context.getResume()).thenReturn(resume);
        lenient().when(context.getJob()).thenReturn(job);
        lenient().when(resume.getUploadStatus()).thenReturn(ResumeUploadStatus.PROCESSED);
        lenient().when(resume.getEmbedding()).thenReturn("[1.0, 0.0]");
        lenient().when(job.getEmbedding()).thenReturn("[1.0, 0.0]");
    }

    @Test
    void testCalculate_shouldReturnZero_whenResumeNotProcessed() throws Exception {
        when(resume.getUploadStatus()).thenReturn(ResumeUploadStatus.UPLOADED);
 
        double result = strategy.calculate(context);
 
        assertEquals(0.0, result);
        verifyNoInteractions(objectMapper);
    }
 
    @Test
    void testCalculate_shouldReturnZero_whenResumeEmbeddingIsInvalidJson() throws Exception {
        when(objectMapper.readValue(eq("[1.0, 0.0]"), any(TypeReference.class)))
                .thenThrow(new JsonMappingException(null, "bad json"));
 
        double result = strategy.calculate(context);
 
        assertEquals(0.0, result);
    }
 
    @Test
    void testCalculate_shouldReturnZero_whenJobEmbeddingIsInvalidJson() throws Exception {
        when(job.getEmbedding()).thenReturn("not-json");
        when(objectMapper.readValue(eq("[1.0, 0.0]"), any(TypeReference.class)))
                .thenReturn(List.of(1.0, 0.0));
        when(objectMapper.readValue(eq("not-json"), any(TypeReference.class)))
                .thenThrow(new JsonMappingException(null, "bad json"));
 
        double result = strategy.calculate(context);
 
        assertEquals(0.0, result);
    }
 
    @Test
    void testCalculate_shouldReturnOne_forIdenticalVectors() throws Exception {
        stubEmbeddings(List.of(1.0, 0.0), List.of(1.0, 0.0));
 
        double result = strategy.calculate(context);
 
        assertEquals(1.0, result, 0.0001);
    }
 
    @Test
    void testCalculate_shouldReturnZero_forOrthogonalVectors() throws Exception {
        stubEmbeddings(List.of(1.0, 0.0), List.of(0.0, 1.0));
 
        double result = strategy.calculate(context);
 
        assertEquals(0.0, result, 0.0001);
    }
 
    @Test
    void testCalculate_shouldReturnNegativeOne_forOppositeVectors() throws Exception {
        stubEmbeddings(List.of(1.0, 0.0), List.of(-1.0, 0.0));
 
        double result = strategy.calculate(context);
 
        assertEquals(-1.0, result, 0.0001);
    }
 
    @Test
    void testCalculate_shouldReturnExpectedPartialSimilarity_forNonTrivialVectors() throws Exception {
        stubEmbeddings(List.of(1.0, 2.0, 3.0), List.of(4.0, 5.0, 6.0));
 
        double result = strategy.calculate(context);
 
        // dot = 1*4 + 2*5 + 3*6 = 32
        // |resume| = sqrt(1+4+9) = sqrt(14), |job| = sqrt(16+25+36) = sqrt(77)
        double expected = 32.0 / (Math.sqrt(14) * Math.sqrt(77));
        assertEquals(expected, result, 0.0001);
    }
 
    @Test
    void testCalculate_shouldReturnZero_whenResumeVectorIsAllZeros() throws Exception {
        stubEmbeddings(List.of(0.0, 0.0), List.of(1.0, 1.0));
 
        double result = strategy.calculate(context);
 
        assertEquals(0.0, result);
    }
 
    @Test
    void testCalculate_shouldReturnZero_whenJobVectorIsAllZeros() throws Exception {
        stubEmbeddings(List.of(1.0, 1.0), List.of(0.0, 0.0));
 
        double result = strategy.calculate(context);
 
        assertEquals(0.0, result);
    }
 
    @Test
    void testCalculate_shouldReturnZero_whenVectorLengthsMismatch() throws Exception {
        stubEmbeddings(List.of(1.0, 2.0, 3.0), List.of(1.0, 2.0));
 
        double result = strategy.calculate(context);
 
        assertEquals(0.0, result);
    }
 
    private void stubEmbeddings(List<Double> resumeVec, List<Double> jobVec) throws Exception {
        String resumeJson = resumeVec.toString();
        String jobJson = jobVec.toString();
        when(resume.getEmbedding()).thenReturn(resumeJson);
        when(job.getEmbedding()).thenReturn(jobJson);
        when(objectMapper.readValue(eq(resumeJson), any(TypeReference.class))).thenReturn(resumeVec);
        if (!jobJson.equals(resumeJson)) {
            when(objectMapper.readValue(eq(jobJson), any(TypeReference.class))).thenReturn(jobVec);
        }
    }

    @Test
    void testWeight_shouldReturnPointFive() {
        assertEquals(0.5, strategy.weight());
    }
 
    @Test
    void testName_shouldReturnEmbedding() {
        assertEquals("embedding", strategy.name());
    }
}

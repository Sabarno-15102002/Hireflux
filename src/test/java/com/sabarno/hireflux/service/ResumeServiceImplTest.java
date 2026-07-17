package com.sabarno.hireflux.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.sabarno.hireflux.dto.ResumeParsedData;
import com.sabarno.hireflux.dto.response.ResumeResponse;
import com.sabarno.hireflux.entity.Resume;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.exception.NonRetryableProcessingException;
import com.sabarno.hireflux.exception.RetryableProcessingException;
import com.sabarno.hireflux.exception.impl.BadRequestException;
import com.sabarno.hireflux.exception.impl.FileProcessingException;
import com.sabarno.hireflux.exception.impl.ResourceNotFoundException;
import com.sabarno.hireflux.repository.ResumeRepository;
import com.sabarno.hireflux.service.impl.ResumeServiceImpl;
import com.sabarno.hireflux.service.util.EmbeddingService;
import com.sabarno.hireflux.service.util.OpenAIService;
import com.sabarno.hireflux.service.util.S3Service;
import com.sabarno.hireflux.utility.enums.ResumeUploadStatus;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import software.amazon.awssdk.core.exception.SdkClientException;

@ExtendWith(MockitoExtension.class)
class ResumeServiceImplTest {

    private ResumeServiceImpl resumeService;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private OpenAIService openAIService;

    @Mock
    private S3Service s3Service;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private MetricsService metricsService;

    @Mock
    private Counter counter;

    User user;

    @BeforeEach
    void setUp() {
        resumeService = new ResumeServiceImpl(
                resumeRepository,
                openAIService,
                s3Service,
                embeddingService,
                objectMapper,
                userService,
                meterRegistry,
                metricsService);
        user = new User();
        user.setId(UUID.randomUUID());

        lenient().when(meterRegistry.counter(anyString())).thenReturn(counter);
    }

    @Test
    void testSaveParsedResume_ResumeAlreadySaved() {
        Resume existing = new Resume();
        existing.setId(UUID.randomUUID());
        existing.setUser(user);
        existing.setFileName("resume.pdf");
        existing.setUploadStatus(ResumeUploadStatus.UPLOADED);

        when(resumeRepository.findByFileKey("key123")).thenReturn(Optional.of(existing));

        ResumeResponse response = resumeService.saveParsedResume(user, "key123", "resume.pdf");

        assertNotNull(response);
        assertEquals(existing.getId(), response.getId());
        verifyNoInteractions(userService, meterRegistry);
        verify(resumeRepository, never()).save(any());
    }

    @Test
    void testSaveParsedResume_NewFileKey() {
        when(resumeRepository.findByFileKey("key123")).thenReturn(Optional.empty());
        when(resumeRepository.save(any(Resume.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ResumeResponse response = resumeService.saveParsedResume(user, "key123", "resume.pdf");

        ArgumentCaptor<Resume> resumeCaptor = ArgumentCaptor.forClass(Resume.class);
        verify(userService).addResume(resumeCaptor.capture());
        Resume createdResume = resumeCaptor.getValue();

        assertEquals(user, createdResume.getUser());
        assertEquals("key123", createdResume.getFileKey());
        assertEquals("resume.pdf", createdResume.getFileName());
        assertEquals(ResumeUploadStatus.UPLOADED, createdResume.getUploadStatus());

        verify(counter).increment();
        verify(resumeRepository).save(any(Resume.class));
        assertNotNull(response);
        assertEquals("resume.pdf", response.getFileName());
    }

    @Test
    void testSaveParsedResume_SaveFailed() {
        when(resumeRepository.findByFileKey("key123")).thenReturn(Optional.empty());
        RuntimeException dbError = new RuntimeException("DB down");
        doThrow(dbError).when(userService).addResume(any(Resume.class));

        FileProcessingException exception = assertThrows(
                FileProcessingException.class,
                () -> resumeService.saveParsedResume(user, "key123", "resume.pdf"));

        assertEquals("Failed to save resume", exception.getMessage());
        assertSame(dbError, exception.getCause());
    }

    @Test
    void testGetResumeForUser_WithResume() {
        Resume resume = new Resume();
        resume.setUser(user);
        when(resumeRepository.findByUserId(user.getId())).thenReturn(List.of(resume));

        List<Resume> result = resumeService.getResumeForUser(user);

        assertEquals(1, result.size());
        verify(resumeRepository).findByUserId(user.getId());
    }

    @Test
    void testGetResumeForUser_NoResume() {
        when(resumeRepository.findByUserId(user.getId())).thenReturn(List.of());

        List<Resume> result = resumeService.getResumeForUser(user);

        assertTrue(result.isEmpty());
    }

    private Resume buildResume(ResumeUploadStatus status) {
        Resume resume = new Resume();
        resume.setId(UUID.randomUUID());
        resume.setUser(user);
        resume.setFileName("resume.pdf");
        resume.setUploadStatus(status);
        return resume;
    }

    @Test
    void testGetResumeById_shouldReturnResume_whenFound() {
        Resume resume = buildResume(ResumeUploadStatus.PROCESSED);
        when(resumeRepository.findById(resume.getId())).thenReturn(Optional.of(resume));

        Resume result = resumeService.getResumeById(resume.getId());

        assertSame(resume, result);
    }

    @Test
    void testGetResumeById_shouldThrowResourceNotFoundException_whenNotFound() {
        UUID resumeId = UUID.randomUUID();
        when(resumeRepository.findById(resumeId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> resumeService.getResumeById(resumeId));

        assertEquals("Resume not found", exception.getMessage());
    }

    @Test
    void testProcessResume_shouldThrowNonRetryableProcessingException_whenResumeNotFound() {
        UUID resumeId = UUID.randomUUID();
        when(resumeRepository.findById(resumeId)).thenReturn(Optional.empty());

        NonRetryableProcessingException exception = assertThrows(
                NonRetryableProcessingException.class,
                () -> resumeService.processResume(resumeId, "key123"));

        assertEquals("Resume not found", exception.getMessage());
        verifyNoInteractions(s3Service, openAIService, embeddingService, metricsService);
    }

    @Test
    void testProcessResume_shouldReturnEarly_whenAlreadyProcessed() {
        Resume resume = buildResume(ResumeUploadStatus.PROCESSED);
        when(resumeRepository.findById(resume.getId())).thenReturn(Optional.of(resume));

        resumeService.processResume(resume.getId(), "key123");

        // idempotency: return happens before the try/finally, so save()
        // is never called again and no downstream services are touched.
        verify(resumeRepository, never()).save(any());
        verifyNoInteractions(s3Service, openAIService, embeddingService, metricsService);
    }

    @Test
    void testProcessResume_shouldCompleteSuccessfully_whenAllStepsSucceed() throws Exception {
        Resume resume = buildResume(ResumeUploadStatus.UPLOADED);
        when(resumeRepository.findById(resume.getId())).thenReturn(Optional.of(resume));

        InputStream fileStream = new ByteArrayInputStream(
                "Experienced Java developer with Spring Boot skills".getBytes());
        when(s3Service.getObject("key123")).thenReturn(fileStream);

        ResumeParsedData parsedData = new ResumeParsedData();
        when(openAIService.parseResume(anyString())).thenReturn("{\"name\":\"Jane\"}");
        when(objectMapper.readValue(eq("{\"name\":\"Jane\"}"), eq(ResumeParsedData.class)))
                .thenReturn(parsedData);
        when(objectMapper.writeValueAsString(parsedData)).thenReturn("{\"parsed\":true}");

        List<Double> embedding = List.of(0.1, 0.2, 0.3);
        when(embeddingService.createEmbedding("{\"parsed\":true}")).thenReturn(embedding);
        when(objectMapper.writeValueAsString(embedding)).thenReturn("[0.1,0.2,0.3]");

        resumeService.processResume(resume.getId(), "key123");

        assertEquals(ResumeUploadStatus.PROCESSED, resume.getUploadStatus());
        assertEquals("{\"parsed\":true}", resume.getParsedData());
        assertEquals("[0.1,0.2,0.3]", resume.getEmbedding());
        assertNull(resume.getErrorMessage());

        verify(metricsService).incrementResumeSuccess();
        // saved once transitioning to PROCESSING, once more in the finally block
        verify(resumeRepository, times(2)).save(resume);
    }

    @Test
    void testProcessResume_shouldThrowRetryableProcessingException_onSdkClientException() {
        Resume resume = buildResume(ResumeUploadStatus.UPLOADED);
        when(resumeRepository.findById(resume.getId())).thenReturn(Optional.of(resume));
        when(s3Service.getObject("key123")).thenThrow(
                SdkClientException.builder().message("S3 unreachable").build());

        assertThrows(RetryableProcessingException.class,
                () -> resumeService.processResume(resume.getId(), "key123"));

        assertEquals(ResumeUploadStatus.FAILED, resume.getUploadStatus());
        assertNotNull(resume.getErrorMessage());
        verify(resumeRepository, times(2)).save(resume); // PROCESSING, then FAILED in finally
        verifyNoInteractions(metricsService);
    }

    @Test
    void testProcessResume_shouldThrowNonRetryableProcessingException_onInvalidFormatException() throws Exception {
        Resume resume = buildResume(ResumeUploadStatus.UPLOADED);
        when(resumeRepository.findById(resume.getId())).thenReturn(Optional.of(resume));

        InputStream fileStream = new ByteArrayInputStream("some resume text".getBytes());
        when(s3Service.getObject("key123")).thenReturn(fileStream);

        when(openAIService.parseResume(anyString())).thenReturn("{}");
        ResumeParsedData parsedData = new ResumeParsedData();
        when(objectMapper.readValue(eq("{}"), eq(ResumeParsedData.class))).thenReturn(parsedData);

        InvalidFormatException invalidFormat = InvalidFormatException.from(null, "bad format", "value", String.class);
        when(objectMapper.writeValueAsString(parsedData)).thenThrow(invalidFormat);

        assertThrows(NonRetryableProcessingException.class,
                () -> resumeService.processResume(resume.getId(), "key123"));

        assertEquals(ResumeUploadStatus.FAILED, resume.getUploadStatus());
        assertNotNull(resume.getErrorMessage());
    }

    @Test
    void testProcessResume_shouldThrowFileProcessingException_onUnexpectedError() {
        Resume resume = buildResume(ResumeUploadStatus.UPLOADED);
        when(resumeRepository.findById(resume.getId())).thenReturn(Optional.of(resume));
        when(s3Service.getObject("key123")).thenThrow(new RuntimeException("Unexpected boom"));

        assertThrows(FileProcessingException.class,
                () -> resumeService.processResume(resume.getId(), "key123"));

        assertEquals(ResumeUploadStatus.FAILED, resume.getUploadStatus());
        verify(resumeRepository, times(2)).save(resume);
    }

    @Test
    void testProcessResume_shouldThrowNonRetryableProcessingException_whenExtractedTextIsBlank() {
        Resume resume = buildResume(ResumeUploadStatus.UPLOADED);
        when(resumeRepository.findById(resume.getId())).thenReturn(Optional.of(resume));

        // Whitespace-only content -> Tika will extract blank text
        InputStream blankStream = new ByteArrayInputStream("   ".getBytes());
        when(s3Service.getObject("key123")).thenReturn(blankStream);

        // extractText wraps its internal NonRetryableProcessingException in
        // a FileProcessingException, so that's what actually propagates.
        assertThrows(FileProcessingException.class,
                () -> resumeService.processResume(resume.getId(), "key123"));

        assertEquals(ResumeUploadStatus.FAILED, resume.getUploadStatus());
    }

    @Test
    void testProcessResume_shouldWrapException_whenAIParsingFails() throws Exception {
        Resume resume = buildResume(ResumeUploadStatus.UPLOADED);
        when(resumeRepository.findById(resume.getId())).thenReturn(Optional.of(resume));

        InputStream fileStream = new ByteArrayInputStream("some resume text".getBytes());
        when(s3Service.getObject("key123")).thenReturn(fileStream);

        RuntimeException aiFailure = new RuntimeException("OpenAI timeout");
        when(openAIService.parseResume(anyString())).thenThrow(aiFailure);

        FileProcessingException outer = assertThrows(
                FileProcessingException.class,
                () -> resumeService.processResume(resume.getId(), "key123"));

        // outer wrap comes from processResume's own catch block
        assertEquals("Failed to process resume", outer.getMessage());

        // inner wrap comes from parseResumeWithAI's catch block
        assertInstanceOf(FileProcessingException.class, outer.getCause());
        assertEquals("Failed to parse resume with AI", outer.getCause().getMessage());
        assertSame(aiFailure, outer.getCause().getCause());

        assertEquals(ResumeUploadStatus.FAILED, resume.getUploadStatus());
    }

    @Test
    void testProcessResume_shouldWrapException_whenEmbeddingSerializationFails() throws Exception {
        Resume resume = buildResume(ResumeUploadStatus.UPLOADED);
        when(resumeRepository.findById(resume.getId())).thenReturn(Optional.of(resume));

        InputStream fileStream = new ByteArrayInputStream("some resume text".getBytes());
        when(s3Service.getObject("key123")).thenReturn(fileStream);

        ResumeParsedData parsedData = new ResumeParsedData();
        when(openAIService.parseResume(anyString())).thenReturn("{\"name\":\"Jane\"}");
        when(objectMapper.readValue(eq("{\"name\":\"Jane\"}"), eq(ResumeParsedData.class)))
                .thenReturn(parsedData);
        when(objectMapper.writeValueAsString(parsedData)).thenReturn("{\"parsed\":true}");

        List<Double> embedding = List.of(0.1, 0.2, 0.3);
        when(embeddingService.createEmbedding("{\"parsed\":true}")).thenReturn(embedding);

        RuntimeException jsonFailure = new RuntimeException("Serialization boom");
        when(objectMapper.writeValueAsString(embedding)).thenThrow(jsonFailure);

        FileProcessingException outer = assertThrows(
                FileProcessingException.class,
                () -> resumeService.processResume(resume.getId(), "key123"));

        assertEquals("Failed to process resume", outer.getMessage());

        // inner wrap comes from toJson's catch block
        assertInstanceOf(BadRequestException.class, outer.getCause());
        assertEquals("Error serializing embedding", outer.getCause().getMessage());

        assertEquals(ResumeUploadStatus.FAILED, resume.getUploadStatus());
    }
}

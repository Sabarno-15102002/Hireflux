package com.sabarno.hireflux.service.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sabarno.hireflux.entity.Job;
import com.sabarno.hireflux.entity.JobApplication;
import com.sabarno.hireflux.entity.Resume;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.exception.impl.BadRequestException;
import com.sabarno.hireflux.repository.JobApplicationRepository;
import com.sabarno.hireflux.service.matching.JobMatchingEngine;
import com.sabarno.hireflux.service.matching.MatchContext;

@ExtendWith(MockitoExtension.class)
class JobMatchingAlgoTest {
 
    @Mock
    private JobApplicationRepository jobApplicationRepository;
 
    @Mock
    private JobMatchingEngine jobMatchingEngine;
 
    @InjectMocks
    private JobMatchingAlgo jobMatchingAlgo;
 
    private Resume resume;
    private Job job;
    private User user;
    private JobApplication application;
 
    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
 
        resume = new Resume();
        resume.setUser(user);
 
        job = new Job();
        job.setId(UUID.randomUUID());
 
        application = new JobApplication();
        application.setId(UUID.randomUUID());
    }
 
    @Test
    void testCalculateScore_shouldSetAndSaveMatchScore_whenApplicationExists() throws Exception {
        when(jobApplicationRepository.findByApplicantIdAndJobId(user.getId(), job.getId()))
                .thenReturn(Optional.of(application));
        when(jobMatchingEngine.calculate(any(MatchContext.class))).thenReturn(0.85);
 
        jobMatchingAlgo.calculateScore(resume, job);
 
        ArgumentCaptor<JobApplication> captor = ArgumentCaptor.forClass(JobApplication.class);
        verify(jobApplicationRepository).save(captor.capture());
        assertEquals(0.85, captor.getValue().getMatchScore());
    }
 
    @Test
    void testCalculateScore_shouldPassResumeAndJobIntoMatchContext() throws Exception {
        when(jobApplicationRepository.findByApplicantIdAndJobId(user.getId(), job.getId()))
                .thenReturn(Optional.of(application));
        when(jobMatchingEngine.calculate(any(MatchContext.class))).thenReturn(0.5);
 
        jobMatchingAlgo.calculateScore(resume, job);
 
        ArgumentCaptor<MatchContext> contextCaptor = ArgumentCaptor.forClass(MatchContext.class);
        verify(jobMatchingEngine).calculate(contextCaptor.capture());
        assertEquals(resume, contextCaptor.getValue().getResume());
        assertEquals(job, contextCaptor.getValue().getJob());
    }
 
    @Test
    void testCalculateScore_shouldThrowBadRequestException_whenNoApplicationFound() {
        when(jobApplicationRepository.findByApplicantIdAndJobId(user.getId(), job.getId()))
                .thenReturn(Optional.empty());
 
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> jobMatchingAlgo.calculateScore(resume, job)
        );
 
        assertEquals("Couldn't calculate matching score", exception.getMessage());
        verifyNoInteractions(jobMatchingEngine);
        verify(jobApplicationRepository, never()).save(any());
    }
 
    @Test
    void testCalculateScore_shouldLoseOriginalExceptionMessage_whenApplicationNotFound() {
        // Documents a real gap: the inner "No application found" message
        // (and the original exception as a cause) is discarded when
        // caught and re-thrown as a new BadRequestException with a
        // generic message. Debugging this failure in production logs
        // would not reveal *why* the application lookup failed, only
        // that it did.
        when(jobApplicationRepository.findByApplicantIdAndJobId(user.getId(), job.getId()))
                .thenReturn(Optional.empty());
 
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> jobMatchingAlgo.calculateScore(resume, job)
        );
 
        assertNotEquals("No application found", exception.getMessage());
        assertNull(exception.getCause());
    }
 
    @Test
    void testCalculateScore_shouldPropagateException_whenMatchingEngineThrowsUnchecked() {
        // Documents current behavior: the catch block only catches
        // BadRequestException. If jobMatchingEngine.calculate() throws
        // anything else (e.g. a bug in a MatchingStrategy implementation),
        // it propagates uncaught rather than being wrapped consistently
        // like the "no application found" case.
        when(jobApplicationRepository.findByApplicantIdAndJobId(user.getId(), job.getId()))
                .thenReturn(Optional.of(application));
        when(jobMatchingEngine.calculate(any(MatchContext.class)))
                .thenThrow(new RuntimeException("Matching strategy failure"));
 
        assertThrows(RuntimeException.class, () -> jobMatchingAlgo.calculateScore(resume, job));
 
        verify(jobApplicationRepository, never()).save(any());
    }
 
    @Test
    void testCalculateScore_shouldNotSave_whenMatchingEngineThrows() {
        when(jobApplicationRepository.findByApplicantIdAndJobId(user.getId(), job.getId()))
                .thenReturn(Optional.of(application));
        when(jobMatchingEngine.calculate(any(MatchContext.class)))
                .thenThrow(new RuntimeException("boom"));
 
        assertThrows(RuntimeException.class, () -> jobMatchingAlgo.calculateScore(resume, job));
 
        verify(jobApplicationRepository, never()).save(any());
    }
 
    @Test
    void testCalculateScore_shouldHandleZeroMatchScore() throws Exception {
        when(jobApplicationRepository.findByApplicantIdAndJobId(user.getId(), job.getId()))
                .thenReturn(Optional.of(application));
        when(jobMatchingEngine.calculate(any(MatchContext.class))).thenReturn(0.0);
 
        jobMatchingAlgo.calculateScore(resume, job);
 
        ArgumentCaptor<JobApplication> captor = ArgumentCaptor.forClass(JobApplication.class);
        verify(jobApplicationRepository).save(captor.capture());
        assertEquals(0.0, captor.getValue().getMatchScore());
    }
}

package com.sabarno.hireflux.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sabarno.hireflux.dto.request.ApplyJobRequest;
import com.sabarno.hireflux.entity.Job;
import com.sabarno.hireflux.entity.JobApplication;
import com.sabarno.hireflux.entity.Resume;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.exception.impl.ConflictException;
import com.sabarno.hireflux.exception.impl.ResourceNotFoundException;
import com.sabarno.hireflux.exception.impl.UnauthorizedException;
import com.sabarno.hireflux.repository.JobApplicationRepository;
import com.sabarno.hireflux.repository.JobRepository;
import com.sabarno.hireflux.repository.ResumeRepository;
import com.sabarno.hireflux.service.impl.JobApplicationServiceImpl;
import com.sabarno.hireflux.service.util.JobMatchingAlgo;
import com.sabarno.hireflux.utility.enums.ApplicationStatus;
import com.sabarno.hireflux.utility.enums.JobStatus;
import com.sabarno.hireflux.utility.enums.UserRole;
import com.sabarno.hireflux.utility.projection.ApplicationSummary;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceImplTest {

    @Mock
    private JobRepository jobRepository;
 
    @Mock
    private ResumeRepository resumeRepository;
 
    @Mock
    private JobApplicationRepository applicationRepository;
 
    @Mock
    private UserService userService;
 
    @Mock
    private JobMatchingAlgo matchingAlgo;
 
    private MeterRegistry meterRegistry;
 
    private JobApplicationServiceImpl service;
 
    private User candidate;
    private Job activeJob;
    private Resume resume;
 
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
 
        service = new JobApplicationServiceImpl(
                jobRepository,
                resumeRepository,
                applicationRepository,
                userService,
                matchingAlgo,
                meterRegistry
        );
 
        candidate = new User();
        candidate.setId(UUID.randomUUID());
        candidate.setRole(UserRole.CANDIDATE);
 
        resume = new Resume();
        resume.setId(UUID.randomUUID());
        resume.setUser(candidate);
        candidate.setResumes(List.of(resume));
 
        activeJob = new Job();
        activeJob.setId(UUID.randomUUID());
        activeJob.setStatus(JobStatus.ACTIVE);
    }

    @Test
    void testApplyToJob_shouldThrowUnauthorizedException_whenUserIsNotCandidate() {
        candidate.setRole(UserRole.RECRUITER);
        ApplyJobRequest request = new ApplyJobRequest();
 
        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> service.applyToJob(activeJob.getId(), request, candidate)
        );
 
        assertEquals("Only candidates can apply", exception.getMessage());
        verifyNoInteractions(jobRepository, applicationRepository, resumeRepository);
    }
 
    @Test
    void testApplyToJob_shouldThrowResourceNotFoundException_whenJobDoesNotExist() {
        when(jobRepository.findById(activeJob.getId())).thenReturn(Optional.empty());
        ApplyJobRequest request = new ApplyJobRequest();
 
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.applyToJob(activeJob.getId(), request, candidate)
        );
 
        assertEquals("Job not found", exception.getMessage());
    }
 
    @Test
    void testApplyToJob_shouldThrowConflictException_whenAlreadyApplied() {
        when(jobRepository.findById(activeJob.getId())).thenReturn(Optional.of(activeJob));
        JobApplication existingApplication = new JobApplication();
        when(applicationRepository.findByApplicantIdAndJobId(candidate.getId(), activeJob.getId()))
                .thenReturn(Optional.of(existingApplication));
        ApplyJobRequest request = new ApplyJobRequest();
 
        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> service.applyToJob(activeJob.getId(), request, candidate)
        );
 
        assertEquals("Already applied to this job", exception.getMessage());
        verify(applicationRepository, never()).save(any());
    }
 
    @Test
    void testApplyToJob_shouldThrowConflictException_whenJobIsNotActive() {
        activeJob.setStatus(JobStatus.CLOSED);
        when(jobRepository.findById(activeJob.getId())).thenReturn(Optional.of(activeJob));
        when(applicationRepository.findByApplicantIdAndJobId(candidate.getId(), activeJob.getId()))
                .thenReturn(Optional.empty());
        ApplyJobRequest request = new ApplyJobRequest();
 
        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> service.applyToJob(activeJob.getId(), request, candidate)
        );
 
        assertEquals("Job is not open for applications", exception.getMessage());
    }
 
    @Test
    void testApplyToJob_shouldThrowResourceNotFoundException_whenSpecifiedResumeDoesNotExist() {
        stubJobAndNoExistingApplication();
        ApplyJobRequest request = new ApplyJobRequest();
        UUID missingResumeId = UUID.randomUUID();
        request.setResumeId(missingResumeId);
        when(resumeRepository.findById(missingResumeId)).thenReturn(Optional.empty());
 
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.applyToJob(activeJob.getId(), request, candidate)
        );
 
        assertEquals("Resume not found", exception.getMessage());
    }
 
    @Test
    void testApplyToJob_shouldThrowResourceNotFoundException_whenNoResumeIdAndUserHasNoResumes() {
        candidate.setResumes(List.of());
        stubJobAndNoExistingApplication();
        ApplyJobRequest request = new ApplyJobRequest(); // no resumeId
 
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.applyToJob(activeJob.getId(), request, candidate)
        );
 
        assertEquals("No resume found", exception.getMessage());
    }
 
    @Test
    void testApplyToJob_shouldThrowUnauthorizedException_whenResumeBelongsToAnotherUser() {
        stubJobAndNoExistingApplication();
 
        User someoneElse = new User();
        someoneElse.setId(UUID.randomUUID());
        Resume othersResume = new Resume();
        othersResume.setId(UUID.randomUUID());
        othersResume.setUser(someoneElse);
 
        ApplyJobRequest request = new ApplyJobRequest();
        request.setResumeId(othersResume.getId());
        when(resumeRepository.findById(othersResume.getId())).thenReturn(Optional.of(othersResume));
 
        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> service.applyToJob(activeJob.getId(), request, candidate)
        );
 
        assertEquals("Invalid resume", exception.getMessage());
    }
 
    @Test
    void testApplyToJob_shouldSaveApplication_whenUsingDefaultResume(){
        stubJobAndNoExistingApplication();
        ApplyJobRequest request = new ApplyJobRequest(); // no resumeId -> falls back to user's first resume
 
        service.applyToJob(activeJob.getId(), request, candidate);
 
        verify(applicationRepository).save(argThat(app ->
                app.getApplicant().equals(candidate)
                        && app.getJob().equals(activeJob)
                        && app.getResume().equals(resume)
                        && app.getStatus() == ApplicationStatus.APPLIED
                        && app.getAppliedAt() != null
        ));
    }
 
    @Test
    void testApplyToJob_shouldNotReAddResume_whenResumeAlreadyBelongsToUserProfile() {
        stubJobAndNoExistingApplication();
        ApplyJobRequest request = new ApplyJobRequest();
        request.setResumeId(resume.getId());
        when(resumeRepository.findById(resume.getId())).thenReturn(Optional.of(resume));
 
        service.applyToJob(activeJob.getId(), request, candidate);
 
        // resume is already in candidate.getResumes(), so it should not be re-added
        verify(userService, never()).addResume(any());
    }
 
    @Test
    void testApplyToJob_shouldAddResumeToProfile_whenResumeNotAlreadyInUsersList() {
        stubJobAndNoExistingApplication();
 
        Resume externalResume = new Resume();
        externalResume.setId(UUID.randomUUID());
        externalResume.setUser(candidate); // owned by candidate, but not in candidate.getResumes()
 
        ApplyJobRequest request = new ApplyJobRequest();
        request.setResumeId(externalResume.getId());
        when(resumeRepository.findById(externalResume.getId())).thenReturn(Optional.of(externalResume));
 
        service.applyToJob(activeJob.getId(), request, candidate);
 
        verify(userService).addResume(externalResume);
    }
 
    @Test
    void testApplyToJob_shouldCalculateMatchScore_andRegisterApplicationWithUser() {
        stubJobAndNoExistingApplication();
        ApplyJobRequest request = new ApplyJobRequest();
 
        service.applyToJob(activeJob.getId(), request, candidate);
 
        verify(matchingAlgo).calculateScore(resume, activeJob);
        verify(userService).addApplication(any(JobApplication.class));
    }
 
    @Test
    void testApplyToJob_shouldIncrementApplicationsSubmittedCounter() {
        stubJobAndNoExistingApplication();
        ApplyJobRequest request = new ApplyJobRequest();
 
        service.applyToJob(activeJob.getId(), request, candidate);
 
        assertEquals(1.0, meterRegistry.counter("applications.submitted").count());
    }
 
    private void stubJobAndNoExistingApplication() {
        when(jobRepository.findById(activeJob.getId())).thenReturn(Optional.of(activeJob));
        when(applicationRepository.findByApplicantIdAndJobId(candidate.getId(), activeJob.getId()))
                .thenReturn(Optional.empty());
    }
 
    @Test
    void testGetMyApplications_shouldDelegateToRepositoryWithUserId() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ApplicationSummary> expectedPage = new PageImpl<>(List.of());
        when(applicationRepository.findByApplicantId(candidate.getId(), pageable)).thenReturn(expectedPage);
 
        Page<ApplicationSummary> result = service.getMyApplications(candidate, pageable);
 
        assertSame(expectedPage, result);
        verify(applicationRepository).findByApplicantId(candidate.getId(), pageable);
    }
 
    @Test
    void testGetApplicationsForJob_shouldThrowUnauthorizedException_whenUserIsNotRecruiter() {
        Pageable pageable = PageRequest.of(0, 10);
 
        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> service.getApplicationsForJob(activeJob.getId(), candidate, pageable)
        );
 
        assertEquals("Only recruiters can view applicants", exception.getMessage());
        verifyNoInteractions(jobRepository, applicationRepository);
    }
 
    @Test
    void testGetApplicationsForJob_shouldThrowResourceNotFoundException_whenJobDoesNotExist() {
        User recruiter = buildRecruiter();
        Pageable pageable = PageRequest.of(0, 10);
        when(jobRepository.findById(activeJob.getId())).thenReturn(Optional.empty());
 
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.getApplicationsForJob(activeJob.getId(), recruiter, pageable)
        );
 
        assertEquals("No Job available with the id:" + activeJob.getId(), exception.getMessage());
    }
 
    @Test
    void testGetApplicationsForJob_shouldThrowUnauthorizedException_whenRecruiterDidNotPostJob() {
        User recruiter = buildRecruiter();
        activeJob.setPostedBy(new User());
        activeJob.getPostedBy().setId(UUID.randomUUID());
        Pageable pageable = PageRequest.of(0, 10);
        when(jobRepository.findById(activeJob.getId())).thenReturn(Optional.of(activeJob));
 
        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> service.getApplicationsForJob(activeJob.getId(), recruiter, pageable)
        );
 
        assertEquals("Not allowed", exception.getMessage());
    }
 
    @Test
    void testGetApplicationsForJob_shouldReturnApplications_whenRecruiterPostedTheJob() {
        User recruiter = buildRecruiter();
        activeJob.setPostedBy(recruiter);
        Pageable pageable = PageRequest.of(0, 10);
        Page<ApplicationSummary> expectedPage = new PageImpl<>(List.of());
 
        when(jobRepository.findById(activeJob.getId())).thenReturn(Optional.of(activeJob));
        when(applicationRepository.findByJobId(activeJob.getId(), pageable)).thenReturn(expectedPage);
 
        Page<ApplicationSummary> result = service.getApplicationsForJob(activeJob.getId(), recruiter, pageable);
 
        assertSame(expectedPage, result);
    }

    @Test
    void testUpdateStatus_shouldThrowUnauthorizedException_whenUserIsNotRecruiter() {
        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> service.updateStatus(UUID.randomUUID(), ApplicationStatus.SHORTLISTED, candidate)
        );
 
        assertEquals("Only recruiters can update status", exception.getMessage());
        verifyNoInteractions(applicationRepository);
    }
 
    @Test
    void testUpdateStatus_shouldThrowResourceNotFoundException_whenApplicationDoesNotExist() {
        User recruiter = buildRecruiter();
        UUID applicationId = UUID.randomUUID();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());
 
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.updateStatus(applicationId, ApplicationStatus.SHORTLISTED, recruiter)
        );
 
        assertEquals("Application not found", exception.getMessage());
    }
 
    @Test
    void testUpdateStatus_shouldThrowUnauthorizedException_whenRecruiterDidNotPostTheJob() {
        User recruiter = buildRecruiter();
        User anotherRecruiter = buildRecruiter();
 
        JobApplication application = new JobApplication();
        application.setId(UUID.randomUUID());
        Job job = new Job();
        job.setPostedBy(anotherRecruiter);
        application.setJob(job);
 
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
 
        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> service.updateStatus(application.getId(), ApplicationStatus.SHORTLISTED, recruiter)
        );
 
        assertEquals("Not allowed", exception.getMessage());
    }
 
    @Test
    void testUpdateStatus_shouldUpdateAndSave_whenRecruiterPostedTheJob() {
        User recruiter = buildRecruiter();
 
        JobApplication application = new JobApplication();
        application.setId(UUID.randomUUID());
        Job job = new Job();
        job.setPostedBy(recruiter);
        application.setJob(job);
        application.setStatus(ApplicationStatus.APPLIED);
 
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
 
        service.updateStatus(application.getId(), ApplicationStatus.SHORTLISTED, recruiter);
 
        assertEquals(ApplicationStatus.SHORTLISTED, application.getStatus());
        assertNotNull(application.getUpdatedAt());
        verify(applicationRepository).save(application);
    }
 
    private User buildRecruiter() {
        User recruiter = new User();
        recruiter.setId(UUID.randomUUID());
        recruiter.setRole(UserRole.RECRUITER);
        return recruiter;
    }
 
    @Test
    void testGetRankedCandidates_shouldDelegateToRepository() {
        UUID jobId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Page<ApplicationSummary> expectedPage = new PageImpl<>(List.of());
        when(applicationRepository.findTopCandidates(jobId, pageable)).thenReturn(expectedPage);
 
        Page<ApplicationSummary> result = service.getRankedCandidates(jobId, pageable);
 
        assertSame(expectedPage, result);
    }

    @Test
    void testGetAllApplications_shouldDelegateToRepository() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ApplicationSummary> expectedPage = new PageImpl<>(List.of());
        when(applicationRepository.findAllApplications(pageable)).thenReturn(expectedPage);
 
        Page<ApplicationSummary> result = service.getAllApplications(pageable);
 
        assertSame(expectedPage, result);
    }

}

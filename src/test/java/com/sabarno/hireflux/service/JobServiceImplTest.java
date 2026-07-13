package com.sabarno.hireflux.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.sabarno.hireflux.dto.request.JobRequest;
import com.sabarno.hireflux.dto.response.JobResponse;
import com.sabarno.hireflux.entity.Company;
import com.sabarno.hireflux.entity.Job;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.exception.impl.BadRequestException;
import com.sabarno.hireflux.exception.impl.ResourceNotFoundException;
import com.sabarno.hireflux.exception.impl.UnauthorizedException;
import com.sabarno.hireflux.repository.JobApplicationRepository;
import com.sabarno.hireflux.repository.JobRepository;
import com.sabarno.hireflux.service.impl.JobServiceImpl;
import com.sabarno.hireflux.service.impl.es.JobIndexService;
import com.sabarno.hireflux.service.util.EmbeddingAsyncService;
import com.sabarno.hireflux.utility.enums.JobStatus;
import com.sabarno.hireflux.utility.enums.JobType;
import com.sabarno.hireflux.utility.enums.UserRole;
import com.sabarno.hireflux.utility.projection.JobSummary;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    private JobServiceImpl jobService;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobApplicationRepository applicationRepository;

    @Mock
    private EmbeddingAsyncService embeddingAsyncService;

    @Mock
    private SkillGraphService skillGraphService;

    private MeterRegistry meterRegistry;

    @Mock
    private JobIndexService jobIndexService;

    private User recruiter;
    private Company company;
    private JobRequest request;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();

        jobService = new JobServiceImpl(
                jobRepository,
                applicationRepository,
                embeddingAsyncService,
                skillGraphService,
                meterRegistry,
                jobIndexService
        );
 
        company = new Company();
        company.setId(UUID.randomUUID());
        company.setName("Acme Corp");
 
        recruiter = new User();
        recruiter.setId(UUID.randomUUID());
        recruiter.setRole(UserRole.RECRUITER);
        recruiter.setCompany(company);
 
        request = new JobRequest();
        request.setTitle("Backend Engineer");
        request.setDescription("Build APIs");
        request.setJobType(JobType.FULL_TIME);
        request.setLocation("Remote");
        request.setMinExperienceRequired(2);
        request.setMaxExperienceRequired(5);
        request.setRequiredSkills(List.of("Java", "Spring"));
 
        lenient().when(jobRepository.save(any(Job.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testCreateJob_Success() throws BadRequestException {
        jobService.createJob(request, recruiter);
 
        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(jobCaptor.capture());
        Job savedJob = jobCaptor.getValue();
 
        assertEquals("Backend Engineer", savedJob.getTitle());
        assertEquals("Build APIs", savedJob.getDescription());
        assertEquals(company, savedJob.getCompany());
        assertEquals(recruiter, savedJob.getPostedBy());
        assertEquals("Remote", savedJob.getLocation());
        assertEquals(List.of("Java", "Spring"), savedJob.getRequiredSkills());
        assertEquals(JobStatus.ACTIVE, savedJob.getStatus());
        assertNotNull(savedJob.getCreatedAt());
        assertEquals(1.0, meterRegistry.counter("jobs.created").count());
    }

    @Test
    void testCreateJob_NullRequiredSkills() throws BadRequestException{
        request.setRequiredSkills(null);
        jobService.createJob(request, recruiter);

        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(jobCaptor.capture());
        Job savedJob = jobCaptor.getValue();

        assertEquals("Backend Engineer", savedJob.getTitle());
        assertEquals("Build APIs", savedJob.getDescription());
        assertEquals(company, savedJob.getCompany());
        assertEquals(recruiter, savedJob.getPostedBy());
        assertEquals("Remote", savedJob.getLocation());
        assertEquals(null, savedJob.getRequiredSkills());
        assertEquals(JobStatus.ACTIVE, savedJob.getStatus());
        assertNotNull(savedJob.getCreatedAt());
        assertEquals(1.0, meterRegistry.counter("jobs.created").count());
    }

    @Test
    void testCreateJob_UnauthorizedUser(){
        recruiter.setRole(UserRole.CANDIDATE);
 
        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> jobService.createJob(request, recruiter)
        );
 
        assertEquals("Only recruiters can post jobs", exception.getMessage());
        verifyNoInteractions(jobRepository, skillGraphService, embeddingAsyncService, jobIndexService);
    }

    @Test
    void testCreateJob_RecruiterWithoutCompany(){
        recruiter.setCompany(null);
 
        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> jobService.createJob(request, recruiter)
        );
 
        assertEquals("Recruiter must belong to a company", exception.getMessage());
        verifyNoInteractions(jobRepository, skillGraphService, embeddingAsyncService, jobIndexService);
    }

    @Test
    void testGetAllJobs(){
        Pageable pageable = PageRequest.of(0, 10);
        JobSummary summary = mock(JobSummary.class);
        Page<JobSummary> expectedPage = new PageImpl<>(List.of(summary), pageable, 1);
 
        when(jobRepository.findByStatus(JobStatus.ACTIVE, pageable)).thenReturn(expectedPage);
 
        Page<JobSummary> result = jobService.getAllJobs(pageable);
 
        assertEquals(1, result.getTotalElements());
        verify(jobRepository).findByStatus(JobStatus.ACTIVE, pageable);
    }

    @Test
    void testGetAllJobs_WithNoJobs(){
        Pageable pageable = PageRequest.of(0, 10);
        when(jobRepository.findByStatus(JobStatus.ACTIVE, pageable))
                .thenReturn(Page.empty(pageable));
 
        Page<JobSummary> result = jobService.getAllJobs(pageable);
 
        assertTrue(result.isEmpty());
    }

    private Job buildExistingJob(User postedBy) {
        Job job = new Job();
        job.setId(UUID.randomUUID());
        job.setTitle("Backend Engineer");
        job.setDescription("Build APIs");
        job.setLocation("Remote");
        job.setCompany(company);
        job.setPostedBy(postedBy);
        job.setStatus(JobStatus.ACTIVE);
        return job;
    }

    @Test
    void testRemoveJob_Success(){
        Job job = buildExistingJob(recruiter);

        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
 
        JobResponse response = jobService.removeJob(job.getId(), recruiter);
 
        assertEquals(JobStatus.CLOSED, job.getStatus());
        assertNotNull(response);
        assertEquals(job.getTitle(), response.getTitle());
        assertEquals(job.getLocation(), response.getLocation());
        verify(jobRepository).save(job);
        verify(jobIndexService).deleteJob(job.getId());
        verify(applicationRepository).rejectAllByJobId(job.getId());
    }

    @Test
    void testRemoveJob_NotFound(){
        UUID jobId = UUID.randomUUID();

        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> jobService.removeJob(jobId, recruiter)
        );

        assertEquals("Job not found", exception.getMessage());
        verifyNoInteractions(jobIndexService, applicationRepository);
    }

    @Test
    void testRemoveJob_UnauthorizedUser_Badrole(){
        Job job = buildExistingJob(recruiter);
        recruiter.setRole(UserRole.CANDIDATE);
        
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> jobService.removeJob(job.getId(), recruiter)
        );

        assertEquals("Only the recruiter who posted the job can remove it", exception.getMessage());
        verifyNoInteractions(jobIndexService, applicationRepository);
    }

    @Test
    void testRemoveJob_UnauthorizedUser_DifferentRecruiter(){
        User anotherRecruiter = new User();
        anotherRecruiter.setId(UUID.randomUUID());
        anotherRecruiter.setRole(UserRole.RECRUITER);
        anotherRecruiter.setCompany(company);

        Job job = buildExistingJob(anotherRecruiter);

        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));

        UnauthorizedException exception = assertThrows(
                UnauthorizedException.class,
                () -> jobService.removeJob(job.getId(), recruiter)
        );

        assertEquals("Only the recruiter who posted the job can remove it", exception.getMessage());
        verifyNoInteractions(jobIndexService, applicationRepository);
    }

    @Test
    void testRemoveJob_JobAlreadyClosed(){
        Job job = buildExistingJob(recruiter);
        job.setStatus(JobStatus.CLOSED);

        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> jobService.removeJob(job.getId(), recruiter)
        );

        assertEquals("Job is already closed", exception.getMessage());
        verifyNoInteractions(jobIndexService, applicationRepository);
    }

    @Test
    void testRemoveJob_AdminCanRemoveAnyonesJob(){
        User admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setRole(UserRole.ADMIN);

        Job job = buildExistingJob(recruiter);

        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));

        JobResponse response = jobService.removeJob(job.getId(), admin);

        assertEquals(JobStatus.CLOSED, job.getStatus());
        assertNotNull(response);
        verify(jobRepository).save(job);
        verify(jobIndexService).deleteJob(job.getId());
        verify(applicationRepository).rejectAllByJobId(job.getId());
    }

    @Test
    void testGetJobById_Success(){
        Job job = buildExistingJob(recruiter);
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
 
        Job result = jobService.getJobById(job.getId());
 
        assertSame(job, result);
    }

    @Test
    void testGetJobById_NotFound(){
        UUID jobId = UUID.randomUUID();
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());
 
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> jobService.getJobById(jobId)
        );
 
        assertEquals("Job not found", exception.getMessage());
    }
}

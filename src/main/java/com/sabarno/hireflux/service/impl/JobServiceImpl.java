package com.sabarno.hireflux.service.impl;

import java.time.LocalDateTime;
import java.util.UUID;

import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sabarno.hireflux.dto.request.JobRequest;
import com.sabarno.hireflux.dto.response.JobResponse;
import com.sabarno.hireflux.entity.Job;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.exception.impl.ResourceNotFoundException;
import com.sabarno.hireflux.exception.impl.UnauthorizedException;
import com.sabarno.hireflux.repository.JobApplicationRepository;
import com.sabarno.hireflux.repository.JobRepository;
import com.sabarno.hireflux.service.JobService;
import com.sabarno.hireflux.service.SkillGraphService;
import com.sabarno.hireflux.service.util.EmbeddingAsyncService;
import com.sabarno.hireflux.utility.enums.JobStatus;
import com.sabarno.hireflux.utility.enums.UserRole;
import com.sabarno.hireflux.utility.projection.JobSummary;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JobServiceImpl implements JobService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobApplicationRepository applicationRepository;

    @Autowired
    private EmbeddingAsyncService embeddingAsyncService;

    @Autowired
    private SkillGraphService skillGraphService;

    @Autowired
    private MeterRegistry meterRegistry;

    @CacheEvict(value = "jobs", allEntries = true)
    @Override
    public JobResponse createJob(JobRequest request, User user) throws BadRequestException {

        if (user.getRole() != UserRole.RECRUITER) {
            throw new UnauthorizedException("Only recruiters can post jobs");
        }

        if (user.getCompany() == null) {
            throw new UnauthorizedException("Recruiter must belong to a company");
        }

        Job job = createJobUtil(request, user);
        skillGraphService.updateGraph(request.getRequiredSkills());

        String jobText = buildJobText(job);
        embeddingAsyncService.generateAndSaveEmbedding(job.getId(), jobText);

        return mapToResponse(job);
    }

    @Transactional
    public Job createJobUtil(JobRequest request, User user){
        Job job = new Job();
        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setCompany(user.getCompany());
        job.setPostedBy(user);
        job.setJobType(request.getJobType());
        job.setLocation(request.getLocation());
        job.setMinExperienceRequired(request.getMinExperienceRequired());
        job.setMaxExperienceRequired(request.getMaxExperienceRequired());
        job.setRequiredSkills(request.getRequiredSkills());
        job.setStatus(JobStatus.ACTIVE);
        job.setCreatedAt(LocalDateTime.now());
        meterRegistry.counter("jobs.created").increment();
        log.info("event=create_job, job_id={}, posted_by={}", job.getId(), user.getId());
        return jobRepository.save(job);
    }

    @Cacheable(value = "jobs", key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()")
    @Override
    public Page<JobSummary> getAllJobs(Pageable pageable) {
        return jobRepository.findByStatus(JobStatus.ACTIVE, pageable);
    }

    private JobResponse mapToResponse(Job job) {
        return new JobResponse(
                job.getId(),
                job.getTitle(),
                job.getCompany().getName(),
                job.getLocation());
    }

    private String buildJobText(Job job) {
        String skillString = job.getRequiredSkills() != null
                ? String.join(" ", job.getRequiredSkills())
                : "";
        return job.getTitle() + " " +
                job.getDescription() + " " +
                skillString;
    }

    @Caching(evict = {
            @CacheEvict(value = "job", key = "#jobId"),
            @CacheEvict(value = "jobs", allEntries = true)
    })

    @Override
    @Transactional
    public JobResponse removeJob(UUID jobId, User user) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (user.getRole() != UserRole.RECRUITER || !job.getPostedBy().getId().equals(user.getId())) {
            throw new UnauthorizedException("Only the recruiter who posted the job can remove it");
        }

        job.setStatus(JobStatus.CLOSED);
        jobRepository.save(job);

        applicationRepository.rejectAllByJobId(jobId);

        log.info("event=remove_job, job_id={}, removed_by={}", job.getId(), user.getId());
        return mapToResponse(job);
    }

    @Cacheable(value = "job", key = "#jobId")
    @Override
    public Job getJobById(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
    }
}
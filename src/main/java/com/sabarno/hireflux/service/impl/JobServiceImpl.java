package com.sabarno.hireflux.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.sabarno.hireflux.service.util.EmbeddingService;
import com.sabarno.hireflux.utility.ApplicationStatus;
import com.sabarno.hireflux.utility.JobStatus;
import com.sabarno.hireflux.utility.UserRole;

import jakarta.transaction.Transactional;


@Service
public class JobServiceImpl implements JobService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobApplicationRepository applicationRepository;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SkillGraphService skillGraphService;

    @Override
    @Transactional
    public JobResponse createJob(JobRequest request, User user) throws BadRequestException {

        if (user.getRole() != UserRole.RECRUITER) {
            throw new UnauthorizedException("Only recruiters can post jobs");
        }

        if (user.getCompany() == null) {
            throw new UnauthorizedException("Recruiter must belong to a company");
        }

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
        skillGraphService.updateGraph(request.getRequiredSkills());

        String jobText = buildJobText(job);
        List<Double> embedding = embeddingService.createEmbedding(jobText);
        job.setEmbedding(toJson(embedding));

        jobRepository.save(job);

        return mapToResponse(job);
    }

    @Override
    public List<JobResponse> getAllJobs() {
        return jobRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private JobResponse mapToResponse(Job job) {
        return new JobResponse(
                job.getId(),
                job.getTitle(),
                job.getCompany().getName(),
                job.getLocation());
    }

    private String buildJobText(Job job) {
        return job.getTitle() + " " +
                job.getDescription() + " " +
                String.join(" ", job.getRequiredSkills());
    }

    private String toJson(List<Double> embedding) throws BadRequestException {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (Exception e) {
            throw new BadRequestException("Error serializing embedding");
        }
    }

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

        applicationRepository.findByJobId(jobId)
                .forEach(app -> {
                    app.setStatus(ApplicationStatus.REJECTED);
                    applicationRepository.save(app);
                });

        return mapToResponse(job);
    }

    @Override
    public Job getJobById(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
    }
}
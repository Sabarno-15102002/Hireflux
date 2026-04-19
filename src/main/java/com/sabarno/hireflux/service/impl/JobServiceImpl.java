package com.sabarno.hireflux.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabarno.hireflux.dto.JobRequest;
import com.sabarno.hireflux.entity.Job;
import com.sabarno.hireflux.entity.JobApplication;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.exception.impl.ResourceNotFoundException;
import com.sabarno.hireflux.exception.impl.UnauthorizedException;
import com.sabarno.hireflux.repository.JobApplicationRepository;
import com.sabarno.hireflux.repository.JobRepository;
import com.sabarno.hireflux.response.JobResponse;
import com.sabarno.hireflux.service.JobService;
import com.sabarno.hireflux.service.util.EmbeddingService;
import com.sabarno.hireflux.utility.UserRole;


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

    @Override
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
        job.setCreatedAt(LocalDateTime.now());

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

    @Override
    public void applyToJob(UUID jobId, User user) {

        if (user.getRole() != UserRole.CANDIDATE) {
            throw new UnauthorizedException("Only candidates can apply");
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        JobApplication application = new JobApplication();
        application.setApplicant(user);
        application.setJob(job);
        application.setAppliedAt(LocalDateTime.now());

        applicationRepository.save(application);
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
}
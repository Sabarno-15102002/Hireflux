package com.sabarno.hireflux.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sabarno.hireflux.dto.ResumeParsedData;
import com.sabarno.hireflux.dto.request.ApplyJobRequest;
import com.sabarno.hireflux.dto.response.ApplicationResponse;
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
import com.sabarno.hireflux.service.JobApplicationService;
import com.sabarno.hireflux.service.UserService;
import com.sabarno.hireflux.service.util.JobMatchingAlgo;
import com.sabarno.hireflux.service.util.ResumeParsedDataExtraction;
import com.sabarno.hireflux.utility.ApplicationStatus;
import com.sabarno.hireflux.utility.JobStatus;
import com.sabarno.hireflux.utility.UserRole;

@Service
public class JobApplicationServiceImpl implements JobApplicationService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private JobApplicationRepository applicationRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private JobMatchingAlgo matchingAlgo;

    @Autowired
    private ResumeParsedDataExtraction dataExtraction;

    @Override
    public void applyToJob(UUID jobId, ApplyJobRequest request, User user) throws BadRequestException {

        if (user.getRole() != UserRole.CANDIDATE) {
            throw new UnauthorizedException("Only candidates can apply");
        }

        Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        // prevent duplicate
        applicationRepository.findByApplicantIdAndJobId(user.getId(), jobId)
                .ifPresent(a -> {
                    throw new ConflictException("Already applied to this job");
                });

        if (job.getStatus() != JobStatus.ACTIVE) {
            throw new ConflictException("Job is not open for applications");

        }
        Resume resume;

        if (request.getResumeId() != null) {
            resume = resumeRepository.findById(request.getResumeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Resume not found"));
        } else {
            resume = user.getResume().stream()
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("No resume found"));
        }

        // ✅ Ownership validation
        if (!resume.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Invalid resume");
        }

        ResumeParsedData parsedData = dataExtraction.getParsedData(resume);
        List<String> skills = parsedData.getSkills() != null ? parsedData.getSkills() : new ArrayList<>();
        int experience = dataExtraction.calculateTotalExperience(parsedData.getExperience());
        String location = dataExtraction.extractLocation(parsedData.getExperience());
        

        double embeddingScore = matchingAlgo.calculateEmbeddingScore(resume, job);
        double experienceScore = matchingAlgo.experienceScore(experience, job.getMinExperienceRequired(),
                job.getMaxExperienceRequired());
        double skillsScore = matchingAlgo.skillScore(skills, job.getRequiredSkills());
        double locationScore = matchingAlgo.locationScore(location, job.getLocation());

        // Weighted average
        double score = embeddingScore * 0.5 + experienceScore * 0.2 + skillsScore * 0.2 + locationScore * 0.1;

        JobApplication application = new JobApplication();
        application.setApplicant(user);
        application.setJob(job);
        application.setResume(resume);
        application.setStatus(ApplicationStatus.APPLIED);
        application.setAppliedAt(LocalDateTime.now());
        application.setMatchScore(score);

        applicationRepository.save(application);

        if (user.getResume().stream().noneMatch(r -> r.getId().equals(resume.getId()))) {
            // if resume is not from user profile, we need to add it to user's resume list
            userService.addResume(resume);
        }
    }

    @Override
    public List<ApplicationResponse> getMyApplications(User user) {

        return applicationRepository.findByApplicantId(user.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<ApplicationResponse> getApplicationsForJob(UUID jobId, User user) {

        if (user.getRole() != UserRole.RECRUITER) {
            throw new UnauthorizedException("Only recruiters can view applicants");
        }

        return applicationRepository.findByJobId(jobId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void updateStatus(UUID applicationId, ApplicationStatus status, User user) {

        if (user.getRole() != UserRole.RECRUITER) {
            throw new UnauthorizedException("Only recruiters can update status");
        }

        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        application.setStatus(status);
        application.setUpdatedAt(LocalDateTime.now());

        applicationRepository.save(application);
    }

    private ApplicationResponse mapToResponse(JobApplication app) {
        return new ApplicationResponse(
                app.getId(),
                app.getJob().getId(),
                app.getJob().getTitle(),
                app.getJob().getCompany().getName(),
                app.getStatus(),
                app.getAppliedAt());
    }

    @Override
    public List<JobApplication> getRankedCandidates(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        return applicationRepository.findByJobOrderByMatchScoreDesc(job);
    }
}
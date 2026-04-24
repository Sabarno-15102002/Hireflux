package com.sabarno.hireflux.service.impl;

import java.time.LocalDateTime;
import java.util.UUID;

import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
import com.sabarno.hireflux.service.JobApplicationService;
import com.sabarno.hireflux.service.UserService;
import com.sabarno.hireflux.service.util.JobMatchingAlgo;
import com.sabarno.hireflux.utility.enums.ApplicationStatus;
import com.sabarno.hireflux.utility.enums.JobStatus;
import com.sabarno.hireflux.utility.enums.UserRole;
import com.sabarno.hireflux.utility.projection.ApplicationSummary;

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
            resume = user.getResumes().stream()
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("No resume found"));
        }

        // ✅ Ownership validation
        if (!resume.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Invalid resume");
        }

        JobApplication application = new JobApplication();
        application.setApplicant(user);
        application.setJob(job);
        application.setResume(resume);
        application.setStatus(ApplicationStatus.APPLIED);
        application.setAppliedAt(LocalDateTime.now());
        applicationRepository.save(application);

        if (user.getResumes().stream().noneMatch(r -> r.getId().equals(resume.getId()))) {
            // if resume is not from user profile, we need to add it to user's resume list
            userService.addResume(resume);
        }

        matchingAlgo.calculateScore(resume, job);
        userService.addApplication(application);
    }

    @Override
    public Page<ApplicationSummary> getMyApplications(User user, Pageable pageable) {

        return applicationRepository.findByApplicantId(user.getId(), pageable);
    }

    @Override
    public Page<ApplicationSummary> getApplicationsForJob(UUID jobId, User user, Pageable pageable) {

        if (user.getRole() != UserRole.RECRUITER) {
            throw new UnauthorizedException("Only recruiters can view applicants");
        }

        Job job = jobRepository.findById(jobId).orElseThrow(() -> new ResourceNotFoundException("No Job available with the id:" + jobId));
        if(!job.getPostedBy().getId().equals(user.getId())){
            throw new UnauthorizedException("Not allowed");
        }

        return applicationRepository.findByJobId(jobId, pageable);
    }

    @Override
    public void updateStatus(UUID applicationId, ApplicationStatus status, User user) {

        if (user.getRole() != UserRole.RECRUITER) {
            throw new UnauthorizedException("Only recruiters can update status");
        }
    
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        
        if(!application.getJob().getPostedBy().getId().equals(user.getId())){
            throw new UnauthorizedException("Not allowed");
        }

        application.setStatus(status);
        application.setUpdatedAt(LocalDateTime.now());

        applicationRepository.save(application);
    }

    @Override
    public Page<ApplicationSummary> getRankedCandidates(UUID jobId, Pageable pageable) {

        return applicationRepository.findTopCandidates(jobId, pageable);
    }
}
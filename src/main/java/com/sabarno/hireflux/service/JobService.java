package com.sabarno.hireflux.service;

import java.util.UUID;

import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sabarno.hireflux.dto.request.JobRequest;
import com.sabarno.hireflux.dto.response.JobResponse;
import com.sabarno.hireflux.entity.Job;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.utility.projection.JobSummary;

public interface JobService {
    JobResponse createJob(JobRequest request, User user) throws BadRequestException;
    Page<JobSummary> getAllJobs(Pageable pageable);
    JobResponse removeJob(UUID jobId, User user);
    Job getJobById(UUID jobId);
}
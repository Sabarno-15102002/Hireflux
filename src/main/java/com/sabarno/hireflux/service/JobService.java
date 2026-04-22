package com.sabarno.hireflux.service;

import java.util.List;
import java.util.UUID;

import org.apache.coyote.BadRequestException;

import com.sabarno.hireflux.dto.request.JobRequest;
import com.sabarno.hireflux.dto.response.JobResponse;
import com.sabarno.hireflux.entity.Job;
import com.sabarno.hireflux.entity.User;

public interface JobService {
    JobResponse createJob(JobRequest request, User user) throws BadRequestException;
    List<JobResponse> getAllJobs();
    JobResponse removeJob(UUID jobId, User user);
    Job getJobById(UUID jobId);
}
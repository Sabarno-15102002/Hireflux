package com.sabarno.hireflux.service;

import java.util.List;
import java.util.UUID;

import com.sabarno.hireflux.dto.JobRequest;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.response.JobResponse;

public interface JobService {
    JobResponse createJob(JobRequest request, User user);
    List<JobResponse> getAllJobs();
    void applyToJob(UUID jobId, User user);
}
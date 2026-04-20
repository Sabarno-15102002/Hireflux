package com.sabarno.hireflux.service;

import java.util.List;
import java.util.UUID;

import org.apache.coyote.BadRequestException;

import com.sabarno.hireflux.dto.request.ApplyJobRequest;
import com.sabarno.hireflux.dto.response.ApplicationResponse;
import com.sabarno.hireflux.entity.JobApplication;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.utility.ApplicationStatus;

public interface JobApplicationService {

    void applyToJob(UUID jobId, ApplyJobRequest request, User user) throws BadRequestException;
    List<ApplicationResponse> getMyApplications(User user);
    List<ApplicationResponse> getApplicationsForJob(UUID jobId, User user);
    void updateStatus(UUID applicationId, ApplicationStatus status, User user);
    List<JobApplication> getRankedCandidates(UUID jobId);
}

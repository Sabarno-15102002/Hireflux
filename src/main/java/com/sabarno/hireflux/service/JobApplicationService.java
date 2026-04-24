package com.sabarno.hireflux.service;

import java.util.UUID;

import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sabarno.hireflux.dto.request.ApplyJobRequest;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.utility.enums.ApplicationStatus;
import com.sabarno.hireflux.utility.projection.ApplicationSummary;

public interface JobApplicationService {

    void applyToJob(UUID jobId, ApplyJobRequest request, User user) throws BadRequestException;
    Page<ApplicationSummary> getMyApplications(User user, Pageable pageable);
    Page<ApplicationSummary> getApplicationsForJob(UUID jobId, User user, Pageable pageable);
    void updateStatus(UUID applicationId, ApplicationStatus status, User user);
    Page<ApplicationSummary> getRankedCandidates(UUID jobId, Pageable pageable);
}

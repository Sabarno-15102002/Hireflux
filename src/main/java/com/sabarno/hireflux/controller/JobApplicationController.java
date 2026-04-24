package com.sabarno.hireflux.controller;

import java.util.UUID;

import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sabarno.hireflux.dto.request.ApplyJobRequest;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.exception.impl.UnauthorizedException;
import com.sabarno.hireflux.service.JobApplicationService;
import com.sabarno.hireflux.service.UserService;
import com.sabarno.hireflux.utility.enums.ApplicationStatus;
import com.sabarno.hireflux.utility.enums.UserRole;
import com.sabarno.hireflux.utility.projection.ApplicationSummary;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/applications")
@Tag(name = "Job Application Controller", description = "APIs for managing job applications, viewing applicants, and updating application status")
public class JobApplicationController {

    @Autowired
    private JobApplicationService applicationService;

    @Autowired
    private UserService userService;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return userService.findUserByEmail(email);
    }

    @Operation(summary = "Apply to a job", description = "Allows a user to apply to a specific job. Requires authentication.")
    @PostMapping("/jobs/{jobId}")
    public ResponseEntity<Void> apply(
        @PathVariable UUID jobId,
        @Valid @RequestBody ApplyJobRequest request
    ) throws BadRequestException {

        User user = getCurrentUser();
        applicationService.applyToJob(jobId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Get my applications", description = "Retrieves a list of all job applications submitted by the authenticated user.")
    @GetMapping("/me")
    public ResponseEntity<Page<ApplicationSummary>> getMyApplications(
        @PageableDefault(size = 20, sort = "appliedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        User user = getCurrentUser();
        return ResponseEntity.ok(applicationService.getMyApplications(user, pageable));
    }

    @Operation(summary = "Get applicants for a job", description = "Retrieves a list of all applicants for a specific job. Requires authentication and must be the job poster or a recruiter.")
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<Page<ApplicationSummary>> getApplicants(
        @PathVariable UUID jobId,
        @PageableDefault(size = 20) Pageable pageable
    ) {

        User user = getCurrentUser();
        return ResponseEntity.ok(applicationService.getApplicationsForJob(jobId, user, pageable));
    }

    @Operation(summary = "Update application status", description = "Allows a recruiter or job poster to update the status of a specific job application.")
    @PatchMapping("/{applicationId}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable UUID applicationId,
            @RequestParam ApplicationStatus status
    ) {
        User user = getCurrentUser();
        applicationService.updateStatus(applicationId, status, user);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get ranked candidates for a job", description = "Retrieves a ranked list of candidates for a specific job based on their application scores. Requires authentication and must be a recruiter.")
    @GetMapping("/jobs/{jobId}/ranked")
    public ResponseEntity<Page<ApplicationSummary>> getRankedCandidates(
            @PathVariable UUID jobId,
            @PageableDefault(size = 40) Pageable pageable
    ) {

        User user = getCurrentUser();
        if(user.getRole() != UserRole.RECRUITER) {
            throw new UnauthorizedException("Only recruiters can view ranked candidates");
        }
        return ResponseEntity.ok(applicationService.getRankedCandidates(jobId, pageable));
    }
}
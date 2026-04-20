package com.sabarno.hireflux.controller;

import java.util.List;
import java.util.UUID;

import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sabarno.hireflux.dto.request.ApplyJobRequest;
import com.sabarno.hireflux.dto.response.ApplicationResponse;
import com.sabarno.hireflux.entity.JobApplication;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.exception.impl.UnauthorizedException;
import com.sabarno.hireflux.service.JobApplicationService;
import com.sabarno.hireflux.service.UserService;
import com.sabarno.hireflux.utility.ApplicationStatus;
import com.sabarno.hireflux.utility.UserRole;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/applications")
@Tag(name = "Job Application Controller", description = "APIs for managing job applications, viewing applicants, and updating application status")
public class JobApplicationController {

    @Autowired
    private JobApplicationService applicationService;

    @Autowired
    private UserService userService;

    @Operation(summary = "Apply to a job", description = "Allows a user to apply to a specific job. Requires authentication.")
    @PostMapping("/jobs/{jobId}/apply")
    public ResponseEntity<Void> apply(
            @PathVariable UUID jobId,
            @RequestBody ApplyJobRequest request,
            @RequestHeader("Authorization") String token) throws BadRequestException {

        User user = userService.findUserFromToken(token);
        applicationService.applyToJob(jobId, request, user);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get my applications", description = "Retrieves a list of all job applications submitted by the authenticated user.")
    @GetMapping("/me")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications(
            @RequestHeader("Authorization") String token) {

        User user = userService.findUserFromToken(token);
        return ResponseEntity.ok(applicationService.getMyApplications(user));
    }

    @Operation(summary = "Get applicants for a job", description = "Retrieves a list of all applicants for a specific job. Requires authentication and must be the job poster or a recruiter.")
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<List<ApplicationResponse>> getApplicants(
            @PathVariable UUID jobId,
            @RequestHeader("Authorization") String token) {

        User user = userService.findUserFromToken(token);
        return ResponseEntity.ok(applicationService.getApplicationsForJob(jobId, user));
    }

    @Operation(summary = "Update application status", description = "Allows a recruiter or job poster to update the status of a specific job application.")
    @PatchMapping("/{applicationId}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable UUID applicationId,
            @RequestParam ApplicationStatus status,
            @RequestHeader("Authorization") String token) {

        User user = userService.findUserFromToken(token);
        applicationService.updateStatus(applicationId, status, user);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get ranked candidates for a job", description = "Retrieves a ranked list of candidates for a specific job based on their application scores. Requires authentication and must be a recruiter.")
    @GetMapping("/jobs/{jobId}/ranked")
    public List<JobApplication> getRankedCandidates(
            @PathVariable UUID jobId,
            @RequestHeader("Authorization") String token) {

        User user = userService.findUserFromToken(token);
        if(user.getRole() != UserRole.RECRUITER) {
            throw new UnauthorizedException("Only recruiters can view ranked candidates");
        }
        return applicationService.getRankedCandidates(jobId);
    }
}
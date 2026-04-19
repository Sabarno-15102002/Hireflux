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

import com.sabarno.hireflux.dto.ApplyJobRequest;
import com.sabarno.hireflux.entity.JobApplication;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.exception.impl.UnauthorizedException;
import com.sabarno.hireflux.response.ApplicationResponse;
import com.sabarno.hireflux.service.JobApplicationService;
import com.sabarno.hireflux.service.UserService;
import com.sabarno.hireflux.utility.ApplicationStatus;
import com.sabarno.hireflux.utility.UserRole;

@RestController
@RequestMapping("/api/applications")
public class JobApplicationController {

    @Autowired
    private JobApplicationService applicationService;

    @Autowired
    private UserService userService;

    @PostMapping("/jobs/{jobId}")
    public ResponseEntity<Void> apply(
            @PathVariable UUID jobId,
            @RequestBody ApplyJobRequest request,
            @RequestHeader("Authorization") String token) throws BadRequestException {

        User user = userService.findUserFromToken(token);
        applicationService.applyToJob(jobId, request, user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications(
            @RequestHeader("Authorization") String token) {

        User user = userService.findUserFromToken(token);
        return ResponseEntity.ok(applicationService.getMyApplications(user));
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<List<ApplicationResponse>> getApplicants(
            @PathVariable UUID jobId,
            @RequestHeader("Authorization") String token) {

        User user = userService.findUserFromToken(token);
        return ResponseEntity.ok(applicationService.getApplicationsForJob(jobId, user));
    }

    @PatchMapping("/{applicationId}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable UUID applicationId,
            @RequestParam ApplicationStatus status,
            @RequestHeader("Authorization") String token) {

        User user = userService.findUserFromToken(token);
        applicationService.updateStatus(applicationId, status, user);
        return ResponseEntity.ok().build();
    }

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
package com.sabarno.hireflux.controller;

import java.util.UUID;

import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sabarno.hireflux.dto.request.JobRequest;
import com.sabarno.hireflux.dto.response.JobResponse;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.service.JobService;
import com.sabarno.hireflux.service.UserService;
import com.sabarno.hireflux.utility.projection.JobSummary;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/jobs")
@Tag(name = "Job Controller", description = "APIs for managing job postings, viewing jobs, and removing jobs")
public class JobController {

    @Autowired
    private JobService jobService;

    @Autowired
    private UserService userService;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return userService.findUserByEmail(email);
    }

    @Operation(summary = "Create a new job", description = "Creates a new job posting. Requires authentication.")
    @PostMapping
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody JobRequest request) throws BadRequestException 
    {
        User user = getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobService.createJob(request, user));
    }

    @Operation(summary = "Get all jobs", description = "Retrieves a list of all available job postings. Requires authentication.")
    @GetMapping
    public ResponseEntity<Page<JobSummary>> getAllJobs(
        @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(jobService.getAllJobs(pageable));
    }

    @Operation(summary = "Remove a job", description = "Removes a specific job posting. Requires authentication and must be the job poster or a recruiter.")
    @DeleteMapping("/{jobId}")
    public ResponseEntity<JobResponse> removeJob(
        @PathVariable UUID jobId
    ) {
        User user = getCurrentUser();
        return ResponseEntity.ok(jobService.removeJob(jobId, user));    
    }
}
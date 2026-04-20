package com.sabarno.hireflux.controller;

import java.util.List;
import java.util.UUID;

import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sabarno.hireflux.dto.request.JobRequest;
import com.sabarno.hireflux.dto.response.JobResponse;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.service.JobService;
import com.sabarno.hireflux.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/jobs")
@Tag(name = "Job Controller", description = "APIs for managing job postings, viewing jobs, and removing jobs")
public class JobController {

    @Autowired
    private JobService jobService;

    @Autowired
    private UserService userService;

    @Operation(summary = "Create a new job", description = "Creates a new job posting. Requires authentication.")
    @PostMapping("/create")
    public ResponseEntity<JobResponse> createJob(
            @RequestBody JobRequest request,
            @RequestParam("Authorization") String token) throws BadRequestException {
        
        User user = userService.findUserFromToken(token);
        return ResponseEntity.ok(jobService.createJob(request, user));
    }

    @Operation(summary = "Get all jobs", description = "Retrieves a list of all available job postings. Requires authentication.")
    @GetMapping
    public ResponseEntity<List<JobResponse>> getJobs(
        @RequestHeader("Authorization") String token
    ) {
        return ResponseEntity.ok(jobService.getAllJobs());
    }

    @Operation(summary = "Remove a job", description = "Removes a specific job posting. Requires authentication and must be the job poster or a recruiter.")
    @PostMapping("/{jobId}/remove")
    public ResponseEntity<JobResponse> removeJob(
        @PathVariable UUID jobId,
        @RequestHeader("Authorization") String token
    ) {
        User user = userService.findUserFromToken(token);
        return ResponseEntity.ok(jobService.removeJob(jobId, user));    
    }
}
package com.sabarno.hireflux.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sabarno.hireflux.dto.JobRequest;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.response.JobResponse;
import com.sabarno.hireflux.service.JobService;
import com.sabarno.hireflux.service.UserService;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    @Autowired
    private JobService jobService;

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<JobResponse> createJob(
            @RequestBody JobRequest request,
            @RequestParam("Authorization") String token) {
        
        User user = userService.findUserFromToken(token);
        return ResponseEntity.ok(jobService.createJob(request, user));
    }

    @GetMapping
    public ResponseEntity<List<JobResponse>> getJobs(
        
    ) {
        return ResponseEntity.ok(jobService.getAllJobs());
    }

    @PostMapping("/{jobId}/apply")
    public ResponseEntity<Void> apply(
            @PathVariable UUID jobId,
            @RequestParam("Authorization") String token) {
        User user = userService.findUserFromToken(token);
        jobService.applyToJob(jobId, user);
        return ResponseEntity.ok().build();
    }
}
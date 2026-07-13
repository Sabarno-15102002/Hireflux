package com.sabarno.hireflux.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sabarno.hireflux.dto.request.AdminInviteRequest;
import com.sabarno.hireflux.dto.request.CompleteInviteRequest;
import com.sabarno.hireflux.dto.response.AppResponse;
import com.sabarno.hireflux.dto.response.DashboardAnalyticsResponse;
import com.sabarno.hireflux.dto.response.KafkaMetricsResponse;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.service.AdminService;
import com.sabarno.hireflux.service.JobApplicationService;
import com.sabarno.hireflux.service.JobService;
import com.sabarno.hireflux.service.MetricsService;
import com.sabarno.hireflux.service.UserService;
import com.sabarno.hireflux.utility.projection.ApplicationSummary;
import com.sabarno.hireflux.utility.projection.JobSummary;
import com.sabarno.hireflux.utility.projection.SkillAnalyticsProjection;
import com.sabarno.hireflux.utility.projection.UserSummary;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Controller", description = "APIs for administrative tasks such as user management, job oversight, and analytics")
public class AdminController {

    private final AdminService adminService;

    private final UserService userService;

    private final JobService jobService;

    private final MetricsService metricsService;

    private final JobApplicationService applicationService;

    @Operation (summary = "Get user profile summary", description = "Retrieves a summary of the user's profile information based on their user ID")
    @GetMapping("/user/{userId}")
    public ResponseEntity<UserSummary> getUserSummary(
            @PathVariable UUID userId
    ) {
        
        UserSummary summary = userService.getProfile(userId);
        return ResponseEntity.ok(summary);
    }

    @Operation (summary = "Send invite to new admin", description = "Sends an invitation to a new admin user")
    @PostMapping("/invite")
    public ResponseEntity<AppResponse> sendInviteToNewAdmin(
        @Valid AdminInviteRequest request
    ){
        String email = SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getName();

        User admin = userService.findUserByEmail(email);
        if (admin == null) {
            return ResponseEntity.status(401).body(new AppResponse("Unauthorized"));
        }
        adminService.inviteUser(request, admin);
        AppResponse response = new AppResponse("Invitation sent successfully");
        return ResponseEntity.ok(response);
    }

    @Operation (summary = "Complete admin invite", description = "Completes the admin invitation process by accepting the invite token and setting a password")
    @PostMapping("/auth/invite/complete")
    public ResponseEntity<Void> completeInvite(
            @Valid
            @RequestBody CompleteInviteRequest request
    ) {

        adminService.completeInvite(request);

        return ResponseEntity.ok().build();
    }

    @Operation (summary = "Get all users", description = "Retrieves a list of all users with pagination")
    @GetMapping("/users")
    public ResponseEntity<Page<UserSummary>> getAllUsers(
        @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @Operation (summary = "Update user role", description = "Updates the role of a specific user")
    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<AppResponse> updateUserRole(
        @PathVariable UUID userId,
        @RequestBody String newRole
    ) {
        if(newRole == null || newRole.isEmpty()) {
            return ResponseEntity.badRequest().body(new AppResponse("New role must be provided"));
        }
        if(newRole != null && !newRole.equalsIgnoreCase("ADMIN") && !newRole.equalsIgnoreCase("RECRUITER") && !newRole.equalsIgnoreCase("CANDIDATE")) {
            return ResponseEntity.badRequest().body(new AppResponse("Invalid role: " + newRole));
        }
        adminService.updateUserRole(userId, newRole);
        return ResponseEntity.ok(new AppResponse("User role updated successfully"));
    }

    @Operation (summary = "Get all jobs", description = "Retrieves a list of all job postings with pagination")
    @GetMapping("/jobs")
    public ResponseEntity<Page<JobSummary>> getAllJobs(
        @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(jobService.getAllJobs(pageable));
    }

    @Operation (summary = "Delete job", description = "Deletes a job posting by its ID")
    @DeleteMapping("/jobs/{jobId}")
    public ResponseEntity<AppResponse> deleteJob(
        @PathVariable UUID jobId
    ) {
        User user = userService.findUserByEmail(SecurityContextHolder.getContext().getAuthentication().getName());
        jobService.removeJob(jobId, user);
        return ResponseEntity.ok(new AppResponse("Job deleted successfully"));
    }

    @Operation (summary = "Get all applications", description = "Retrieves a list of all job applications with pagination")
    @GetMapping("/applications")
    public ResponseEntity<Page<ApplicationSummary>> getAllApplications(
        @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(applicationService.getAllApplications(pageable));
    }

    @Operation (summary = "Get dashboard statistics", description = "Retrieves analytics data for the admin dashboard")
    @GetMapping("/analytics/dashboard")
    public ResponseEntity<DashboardAnalyticsResponse> getDashboardStats() {

        return ResponseEntity.ok(
                adminService.getDashboardStats()
        );
    }

    @Operation (summary = "Get top skills", description = "Retrieves a list of the most requested skills")
    @GetMapping("/analytics/skills")
    public ResponseEntity<List<SkillAnalyticsProjection>> getTopSkills() {
        List<SkillAnalyticsProjection> topSkills = adminService.getTopSkills();
        return ResponseEntity.ok(topSkills);
    }

    @Operation (summary = "Get Kafka metrics", description = "Retrieves metrics for the Kafka message broker")
    @GetMapping("/analytics/kafka")
    public ResponseEntity<KafkaMetricsResponse> getKafkaMetrics() {
        KafkaMetricsResponse metrics = metricsService.getKafkaMetrics();
        return ResponseEntity.ok(metrics);
    }

}

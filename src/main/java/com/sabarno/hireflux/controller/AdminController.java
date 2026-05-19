package com.sabarno.hireflux.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.service.AdminService;
import com.sabarno.hireflux.service.JobApplicationService;
import com.sabarno.hireflux.service.JobService;
import com.sabarno.hireflux.service.UserService;
import com.sabarno.hireflux.utility.projection.ApplicationSummary;
import com.sabarno.hireflux.utility.projection.JobSummary;
import com.sabarno.hireflux.utility.projection.SkillAnalyticsProjection;
import com.sabarno.hireflux.utility.projection.UserSummary;

import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserService userService;

    @Autowired
    private JobService jobService;


    @Autowired
    private JobApplicationService applicationService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<UserSummary> getUserSummary(
            @PathVariable UUID userId
    ) {
        
        UserSummary summary = userService.getProfile(userId);
        return ResponseEntity.ok(summary);
    }

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

    @PostMapping("/auth/invite/complete")
    public ResponseEntity<Void> completeInvite(
            @Valid
            @RequestBody CompleteInviteRequest request
    ) {

        adminService.completeInvite(request);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/users")
    public ResponseEntity<Page<UserSummary>> getAllUsers(
        @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<AppResponse> updateUserRole(
        @PathVariable UUID userId,
        @RequestBody String newRole
    ) {
        adminService.updateUserRole(userId, newRole);
        return ResponseEntity.ok(new AppResponse("User role updated successfully"));
    }


    @GetMapping("/jobs")
    public ResponseEntity<Page<JobSummary>> getAllJobs(
        @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(jobService.getAllJobs(pageable));
    }

    @DeleteMapping("/jobs/{jobId}")
    public ResponseEntity<AppResponse> deleteJob(
        @PathVariable UUID jobId
    ) {
        User user = userService.findUserByEmail(SecurityContextHolder.getContext().getAuthentication().getName());
        jobService.removeJob(jobId, user);
        return ResponseEntity.ok(new AppResponse("Job deleted successfully"));
    }

    @GetMapping("/applications")
    public ResponseEntity<Page<ApplicationSummary>> getAllApplications(
        @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(applicationService.getAllApplications(pageable));
    }

    @GetMapping("/analytics/dashboard")
    public ResponseEntity<DashboardAnalyticsResponse> getDashboardStats() {

        return ResponseEntity.ok(
                adminService.getDashboardStats()
        );
    }

    @GetMapping("/analytics/skills")
    public ResponseEntity<List<SkillAnalyticsProjection>> getTopSkills() {
        List<SkillAnalyticsProjection> topSkills = adminService.getTopSkills();
        return ResponseEntity.ok(topSkills);
    }

}

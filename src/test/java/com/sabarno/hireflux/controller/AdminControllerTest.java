package com.sabarno.hireflux.controller;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

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
import com.sabarno.hireflux.utility.enums.UserRole;
import com.sabarno.hireflux.utility.projection.ApplicationSummary;
import com.sabarno.hireflux.utility.projection.JobSummary;
import com.sabarno.hireflux.utility.projection.SkillAnalyticsProjection;
import com.sabarno.hireflux.utility.projection.UserSummary;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {
 
    @Mock
    private AdminService adminService;
 
    @Mock
    private UserService userService;
 
    @Mock
    private JobService jobService;
 
    @Mock
    private MetricsService metricsService;
 
    @Mock
    private JobApplicationService applicationService;
 
    private AdminController controller;
 
    @BeforeEach
    void setUp() {
        controller = new AdminController(
                adminService, userService, jobService, metricsService, applicationService);
    }
 
    @Test
    void testGetUserSummary_shouldReturnOkWithUserSummary() {
        UUID userId = UUID.randomUUID();
        UserSummary summary = mock(UserSummary.class);
        when(userService.getProfile(userId)).thenReturn(summary);
 
        ResponseEntity<UserSummary> response = controller.getUserSummary(userId);
 
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(summary, response.getBody());
    }
 
    @Test
    void testSendInviteToNewAdmin_shouldReturnOk_whenAdminIsFound() {
        AdminInviteRequest request = new AdminInviteRequest();
        User admin = new User();
        admin.setRole(UserRole.ADMIN);
 
        try (MockedStatic<SecurityContextHolder> mocked = mockAuthenticatedAs("admin@hireflux.com")) {
            when(userService.findUserByEmail("admin@hireflux.com")).thenReturn(admin);
 
            ResponseEntity<AppResponse> response = controller.sendInviteToNewAdmin(request);
 
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Invitation sent successfully", response.getBody().getMessage());
            verify(adminService).inviteUser(request, admin);
        }
    }
 
    @Test
    void testSendInviteToNewAdmin_shouldReturn401_whenAuthenticatedUserNotFoundInDatabase() {
        AdminInviteRequest request = new AdminInviteRequest();
 
        try (MockedStatic<SecurityContextHolder> mocked = mockAuthenticatedAs("ghost@hireflux.com")) {
            when(userService.findUserByEmail("ghost@hireflux.com")).thenReturn(null);
 
            ResponseEntity<AppResponse> response = controller.sendInviteToNewAdmin(request);
 
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertEquals("Unauthorized", response.getBody().getMessage());
            verifyNoInteractions(adminService);
        }
    }
 
    @Test
    void testCompleteInvite_shouldReturnOk_andDelegateToAdminService() {
        CompleteInviteRequest request = new CompleteInviteRequest();
 
        ResponseEntity<Void> response = controller.completeInvite(request);
 
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(adminService).completeInvite(request);
    }
 
    @Test
    void testGetAllUsers_shouldDelegateToUserService_andReturnOk() {
        Pageable pageable = PageRequest.of(0, 50);
        Page<UserSummary> expectedPage = new PageImpl<>(List.of());
        when(userService.getAllUsers(pageable)).thenReturn(expectedPage);
 
        ResponseEntity<Page<UserSummary>> response = controller.getAllUsers(pageable);
 
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expectedPage, response.getBody());
    }
 
    @Test
    void testUpdateUserRole_shouldReturnBadRequest_whenNewRoleIsNull() {
        UUID userId = UUID.randomUUID();
 
        ResponseEntity<AppResponse> response = controller.updateUserRole(userId, null);
 
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("New role must be provided", response.getBody().getMessage());
        verifyNoInteractions(adminService);
    }
 
    @Test
    void testUpdateUserRole_shouldReturnBadRequest_whenNewRoleIsEmpty() {
        UUID userId = UUID.randomUUID();
 
        ResponseEntity<AppResponse> response = controller.updateUserRole(userId, "");
 
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("New role must be provided", response.getBody().getMessage());
        verifyNoInteractions(adminService);
    }
 
    @Test
    void testUpdateUserRole_shouldReturnBadRequest_whenNewRoleIsInvalid() {
        UUID userId = UUID.randomUUID();
 
        ResponseEntity<AppResponse> response = controller.updateUserRole(userId, "SUPERUSER");
 
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid role: SUPERUSER", response.getBody().getMessage());
        verifyNoInteractions(adminService);
    }
 
    @Test
    void testUpdateUserRole_shouldSucceed_whenNewRoleIsValid() {
        UUID userId = UUID.randomUUID();
 
        ResponseEntity<AppResponse> response = controller.updateUserRole(userId, "RECRUITER");
 
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User role updated successfully", response.getBody().getMessage());
        verify(adminService).updateUserRole(userId, "RECRUITER");
    }

    @Test
    void testUpdateUserRole_shouldSucceed_whenNewRoleIsCandidate() {
        UUID userId = UUID.randomUUID();

        ResponseEntity<AppResponse> response = controller.updateUserRole(userId, "CANDIDATE");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User role updated successfully", response.getBody().getMessage());
        verify(adminService).updateUserRole(userId, "CANDIDATE");
    }
 
    @Test
    void testUpdateUserRole_shouldAcceptRoleRegardlessOfCase() {
        UUID userId = UUID.randomUUID();
 
        ResponseEntity<AppResponse> response = controller.updateUserRole(userId, "admin");
 
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(adminService).updateUserRole(userId, "admin");
    }
 
    @Test
    void testGetAllJobs_shouldDelegateToJobService_andReturnOk() {
        Pageable pageable = PageRequest.of(0, 25);
        Page<JobSummary> expectedPage = new PageImpl<>(List.of());
        when(jobService.getAllJobs(pageable)).thenReturn(expectedPage);
 
        ResponseEntity<Page<JobSummary>> response = controller.getAllJobs(pageable);
 
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expectedPage, response.getBody());
    }
 
    @Test
    void testDeleteJob_shouldResolveCurrentUser_andDelegateToJobService() {
        UUID jobId = UUID.randomUUID();
        User admin = new User();
        admin.setRole(UserRole.ADMIN);
 
        try (MockedStatic<SecurityContextHolder> mocked = mockAuthenticatedAs("admin@hireflux.com")) {
            when(userService.findUserByEmail("admin@hireflux.com")).thenReturn(admin);
 
            ResponseEntity<AppResponse> response = controller.deleteJob(jobId);
 
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Job deleted successfully", response.getBody().getMessage());
            verify(jobService).removeJob(jobId, admin);
        }
    }
 
    @Test
    void testGetAllApplications_shouldDelegateToApplicationService_andReturnOk() {
        Pageable pageable = PageRequest.of(0, 25);
        Page<ApplicationSummary> expectedPage = new PageImpl<>(List.of());
        when(applicationService.getAllApplications(pageable)).thenReturn(expectedPage);
 
        ResponseEntity<Page<ApplicationSummary>> response = controller.getAllApplications(pageable);
 
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expectedPage, response.getBody());
    }
 
    @Test
    void testGetDashboardStats_shouldDelegateToAdminService_andReturnOk() {
        DashboardAnalyticsResponse stats = new DashboardAnalyticsResponse(10L, 5L, 20L, 3L);
        when(adminService.getDashboardStats()).thenReturn(stats);
 
        ResponseEntity<DashboardAnalyticsResponse> response = controller.getDashboardStats();
 
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(stats, response.getBody());
    }
 
    @Test
    void testGetTopSkills_shouldDelegateToAdminService_andReturnOk() {
        List<SkillAnalyticsProjection> skills = List.of();
        when(adminService.getTopSkills()).thenReturn(skills);
 
        ResponseEntity<List<SkillAnalyticsProjection>> response = controller.getTopSkills();
 
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(skills, response.getBody());
    }
 
    @Test
    void testGetKafkaMetrics_shouldDelegateToMetricsService_andReturnOk() {
        KafkaMetricsResponse metrics = new KafkaMetricsResponse(100L, 5L, 2L, 1L);
        when(metricsService.getKafkaMetrics()).thenReturn(metrics);
 
        ResponseEntity<KafkaMetricsResponse> response = controller.getKafkaMetrics();
 
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(metrics, response.getBody());
    }
 
    private MockedStatic<SecurityContextHolder> mockAuthenticatedAs(String email) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(email);
 
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
 
        MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class);
        mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        return mockedStatic;
    }
}
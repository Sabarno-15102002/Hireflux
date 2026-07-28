package com.sabarno.hireflux.controller;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

import com.sabarno.hireflux.dto.request.ApplyJobRequest;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.exception.impl.ConflictException;
import com.sabarno.hireflux.exception.impl.RateLimitExceededException;
import com.sabarno.hireflux.exception.impl.UnauthorizedException;
import com.sabarno.hireflux.service.JobApplicationService;
import com.sabarno.hireflux.service.UserService;
import com.sabarno.hireflux.service.util.RateLimitService;
import com.sabarno.hireflux.utility.enums.ApplicationStatus;
import com.sabarno.hireflux.utility.enums.UserRole;
import com.sabarno.hireflux.utility.projection.ApplicationSummary;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobApplicationControllerTest {
 
    @Mock
    private JobApplicationService applicationService;
 
    @Mock
    private UserService userService;
 
    @Mock
    private RateLimitService rateLimitService;
 
    private JobApplicationController controller;
 
    private User currentUser;
 
    @BeforeEach
    void setUp() {
        controller = new JobApplicationController(applicationService, userService, rateLimitService);
 
        currentUser = new User();
        currentUser.setId(UUID.randomUUID());
        currentUser.setRole(UserRole.CANDIDATE);
    }
 
    private Bucket buildBucketWithCapacity(long capacity) {
        Bandwidth limit = Bandwidth.builder().capacity(capacity).refillGreedy(capacity, Duration.ofHours(1)).build();
        return Bucket.builder().addLimit(limit).build();
    }
 
    private Bucket buildExhaustedBucket() {
        Bucket bucket = buildBucketWithCapacity(1);
        bucket.tryConsume(1);
        return bucket;
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
 
    @Test
    void testApply_shouldReturn201_andDelegateToApplicationService_whenWithinRateLimit() throws Exception {
        UUID jobId = UUID.randomUUID();
        ApplyJobRequest request = new ApplyJobRequest();
        Bucket bucket = buildBucketWithCapacity(20);
 
        try (MockedStatic<SecurityContextHolder> mocked = mockAuthenticatedAs("candidate@hireflux.com")) {
            when(userService.findUserByEmail("candidate@hireflux.com")).thenReturn(currentUser);
            when(rateLimitService.resolveBucket(
                    "job-apply:" + currentUser.getId(), 20, Duration.ofHours(1)))
                    .thenReturn(bucket);
 
            ResponseEntity<Void> response = controller.apply(jobId, request);
 
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            verify(applicationService).applyToJob(jobId, request, currentUser);
        }
    }
 
    @Test
    void testApply_shouldResolveBucket_withUserSpecificKey() throws Exception {
        UUID jobId = UUID.randomUUID();
        ApplyJobRequest request = new ApplyJobRequest();
        Bucket bucket = buildBucketWithCapacity(20);
 
        try (MockedStatic<SecurityContextHolder> mocked = mockAuthenticatedAs("candidate@hireflux.com")) {
            when(userService.findUserByEmail("candidate@hireflux.com")).thenReturn(currentUser);
            when(rateLimitService.resolveBucket(anyString(), anyLong(), any(Duration.class)))
                    .thenReturn(bucket);
 
            controller.apply(jobId, request);
 
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(rateLimitService).resolveBucket(keyCaptor.capture(), eq(20L), eq(Duration.ofHours(1)));
            assertEquals("job-apply:" + currentUser.getId(), keyCaptor.getValue());
        }
    }
 
    @Test
    void testApply_shouldThrowRateLimitExceededException_andNeverCallApplicationService_whenBucketIsExhausted() throws Exception {
        UUID jobId = UUID.randomUUID();
        ApplyJobRequest request = new ApplyJobRequest();
        Bucket exhaustedBucket = buildExhaustedBucket(); // denies every request immediately
 
        try (MockedStatic<SecurityContextHolder> mocked = mockAuthenticatedAs("candidate@hireflux.com")) {
            when(userService.findUserByEmail("candidate@hireflux.com")).thenReturn(currentUser);
            when(rateLimitService.resolveBucket(anyString(), anyLong(), any(Duration.class)))
                    .thenReturn(exhaustedBucket);
 
            RateLimitExceededException exception = assertThrows(
                    RateLimitExceededException.class,
                    () -> controller.apply(jobId, request)
            );
 
            assertEquals("Too many job applications", exception.getMessage());
            verifyNoInteractions(applicationService);
        }
    }
 
    @Test
    void testApply_shouldPropagateException_whenApplicationServiceRejectsRequest() throws Exception {
        UUID jobId = UUID.randomUUID();
        ApplyJobRequest request = new ApplyJobRequest();
        Bucket bucket = buildBucketWithCapacity(20);
 
        try (MockedStatic<SecurityContextHolder> mocked = mockAuthenticatedAs("candidate@hireflux.com")) {
            when(userService.findUserByEmail("candidate@hireflux.com")).thenReturn(currentUser);
            when(rateLimitService.resolveBucket(anyString(), anyLong(), any(Duration.class)))
                    .thenReturn(bucket);
            doThrow(new ConflictException("Already applied to this job"))
                    .when(applicationService).applyToJob(jobId, request, currentUser);
 
            assertThrows(ConflictException.class, () -> controller.apply(jobId, request));
        }
    }
 
    @Test
    void testGetMyApplications_shouldResolveCurrentUser_andDelegateToApplicationService() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<ApplicationSummary> expectedPage = new PageImpl<>(List.of());
 
        try (MockedStatic<SecurityContextHolder> mocked = mockAuthenticatedAs("candidate@hireflux.com")) {
            when(userService.findUserByEmail("candidate@hireflux.com")).thenReturn(currentUser);
            when(applicationService.getMyApplications(currentUser, pageable)).thenReturn(expectedPage);
 
            ResponseEntity<Page<ApplicationSummary>> response = controller.getMyApplications(pageable);
 
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertSame(expectedPage, response.getBody());
        }
    }
 
    @Test
    void testGetApplicants_shouldResolveCurrentUser_andDelegateToApplicationService() {
        UUID jobId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        Page<ApplicationSummary> expectedPage = new PageImpl<>(List.of());
        User recruiter = new User();
        recruiter.setRole(UserRole.RECRUITER);
 
        try (MockedStatic<SecurityContextHolder> mocked = mockAuthenticatedAs("recruiter@hireflux.com")) {
            when(userService.findUserByEmail("recruiter@hireflux.com")).thenReturn(recruiter);
            when(applicationService.getApplicationsForJob(jobId, recruiter, pageable)).thenReturn(expectedPage);
 
            ResponseEntity<Page<ApplicationSummary>> response = controller.getApplicants(jobId, pageable);
 
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertSame(expectedPage, response.getBody());
        }
    }
 
    @Test
    void testGetApplicants_shouldPropagateException_whenServiceRejectsNonPostingRecruiter() {
        UUID jobId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
 
        try (MockedStatic<SecurityContextHolder> mocked = mockAuthenticatedAs("candidate@hireflux.com")) {
            when(userService.findUserByEmail("candidate@hireflux.com")).thenReturn(currentUser);
            when(applicationService.getApplicationsForJob(jobId, currentUser, pageable))
                    .thenThrow(new UnauthorizedException("Only recruiters can view applicants"));
 
            assertThrows(UnauthorizedException.class, () -> controller.getApplicants(jobId, pageable));
        }
    }
 
    @Test
    void testUpdateStatus_shouldResolveCurrentUser_andDelegateToApplicationService() {
        UUID applicationId = UUID.randomUUID();
        User recruiter = new User();
        recruiter.setRole(UserRole.RECRUITER);
 
        try (MockedStatic<SecurityContextHolder> mocked = mockAuthenticatedAs("recruiter@hireflux.com")) {
            when(userService.findUserByEmail("recruiter@hireflux.com")).thenReturn(recruiter);
 
            ResponseEntity<Void> response = controller.updateStatus(applicationId, ApplicationStatus.SHORTLISTED);
 
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(applicationService).updateStatus(applicationId, ApplicationStatus.SHORTLISTED, recruiter);
        }
    }
 
    @Test
    void testUpdateStatus_shouldPropagateException_whenServiceRejectsUnauthorizedUser() {
        UUID applicationId = UUID.randomUUID();
 
        try (MockedStatic<SecurityContextHolder> mocked = mockAuthenticatedAs("candidate@hireflux.com")) {
            when(userService.findUserByEmail("candidate@hireflux.com")).thenReturn(currentUser);
            doThrow(new UnauthorizedException("Only recruiters can update status"))
                    .when(applicationService)
                    .updateStatus(applicationId, ApplicationStatus.REJECTED, currentUser);
 
            assertThrows(UnauthorizedException.class,
                    () -> controller.updateStatus(applicationId, ApplicationStatus.REJECTED));
        }
    }
 
    @Test
    void testGetRankedCandidates_shouldDelegateToApplicationService_whenUserIsRecruiter() {
        UUID jobId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 40);
        Page<ApplicationSummary> expectedPage = new PageImpl<>(List.of());
        User recruiter = new User();
        recruiter.setRole(UserRole.RECRUITER);
 
        try (MockedStatic<SecurityContextHolder> mocked = mockAuthenticatedAs("recruiter@hireflux.com")) {
            when(userService.findUserByEmail("recruiter@hireflux.com")).thenReturn(recruiter);
            when(applicationService.getRankedCandidates(jobId, pageable)).thenReturn(expectedPage);
 
            ResponseEntity<Page<ApplicationSummary>> response = controller.getRankedCandidates(jobId, pageable);
 
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertSame(expectedPage, response.getBody());
        }
    }
 
    @Test
    void testGetRankedCandidates_shouldThrowUnauthorizedException_whenUserIsNotRecruiter() {
        UUID jobId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 40);
 
        try (MockedStatic<SecurityContextHolder> mocked = mockAuthenticatedAs("candidate@hireflux.com")) {
            when(userService.findUserByEmail("candidate@hireflux.com")).thenReturn(currentUser);
 
            UnauthorizedException exception = assertThrows(
                    UnauthorizedException.class,
                    () -> controller.getRankedCandidates(jobId, pageable)
            );
 
            assertEquals("Only recruiters can view ranked candidates", exception.getMessage());
            verifyNoInteractions(applicationService);
        }
    }
 
    @Test
    void testGetRankedCandidates_shouldThrowNullPointerException_whenAuthenticatedEmailNotFoundInDatabase() {
        UUID jobId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 40);
 
        try (MockedStatic<SecurityContextHolder> mocked = mockAuthenticatedAs("ghost@hireflux.com")) {
            when(userService.findUserByEmail("ghost@hireflux.com")).thenReturn(null);
 
            assertThrows(NullPointerException.class,
                    () -> controller.getRankedCandidates(jobId, pageable));
        }
    }
}
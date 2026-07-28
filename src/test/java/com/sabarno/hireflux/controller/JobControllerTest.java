package com.sabarno.hireflux.controller;

import java.time.LocalDateTime;
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

import com.sabarno.hireflux.dto.request.JobRequest;
import com.sabarno.hireflux.dto.request.JobSearchRequest;
import com.sabarno.hireflux.dto.response.JobResponse;
import com.sabarno.hireflux.dto.response.JobSearchResponse;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.entity.es.JobDocument;
import com.sabarno.hireflux.exception.impl.UnauthorizedException;
import com.sabarno.hireflux.service.JobService;
import com.sabarno.hireflux.service.UserService;
import com.sabarno.hireflux.service.impl.es.JobSearchService;
import com.sabarno.hireflux.utility.enums.UserRole;
import com.sabarno.hireflux.utility.projection.JobSummary;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobControllerTest {
 
    @Mock
    private JobService jobService;
 
    @Mock
    private JobSearchService jobSearchService;
 
    @Mock
    private UserService userService;
 
    private JobController controller;
 
    private User recruiter;
 
    @BeforeEach
    void setUp() {
        controller = new JobController(jobService, jobSearchService, userService);
 
        recruiter = new User();
        recruiter.setId(UUID.randomUUID());
        recruiter.setRole(UserRole.RECRUITER);
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
    void testCreateJob_shouldReturn201_andDelegateToJobService() throws Exception {
        JobRequest request = new JobRequest();
        request.setTitle("Backend Engineer");
        JobResponse expectedResponse = new JobResponse(UUID.randomUUID(), "Backend Engineer", "Acme", "Remote");
 
        try (MockedStatic<SecurityContextHolder> mocked = mockAuthenticatedAs("recruiter@hireflux.com")) {
            when(userService.findUserByEmail("recruiter@hireflux.com")).thenReturn(recruiter);
            when(jobService.createJob(request, recruiter)).thenReturn(expectedResponse);
 
            ResponseEntity<JobResponse> response = controller.createJob(request);
 
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertSame(expectedResponse, response.getBody());
            verify(jobService).createJob(request, recruiter);
        }
    }
 
    @Test
    void testCreateJob_shouldPropagateException_whenJobServiceRejectsRequest() throws Exception {
        JobRequest request = new JobRequest();
 
        try (MockedStatic<SecurityContextHolder> mocked = mockAuthenticatedAs("recruiter@hireflux.com")) {
            when(userService.findUserByEmail("recruiter@hireflux.com")).thenReturn(recruiter);
            when(jobService.createJob(request, recruiter))
                    .thenThrow(new UnauthorizedException("Recruiter must belong to a company"));
 
            assertThrows(UnauthorizedException.class, () -> controller.createJob(request));
        }
    }
 
    @Test
    void testCreateJob_shouldThrowNullPointerException_whenAuthenticatedEmailNotFoundInDatabase() throws Exception {
        JobRequest request = new JobRequest();
 
        try (MockedStatic<SecurityContextHolder> mocked = mockAuthenticatedAs("ghost@hireflux.com")) {
            when(userService.findUserByEmail("ghost@hireflux.com")).thenReturn(null);
            when(jobService.createJob(eq(request), isNull())).thenThrow(new NullPointerException());
 
            assertThrows(NullPointerException.class, () -> controller.createJob(request));
        }
    }
 
    @Test
    void testGetAllJobs_shouldDelegateToJobService_withoutRequiringCurrentUser() {
        Pageable pageable = PageRequest.of(0, 25);
        Page<JobSummary> expectedPage = new PageImpl<>(List.of());
        when(jobService.getAllJobs(pageable)).thenReturn(expectedPage);
 
        ResponseEntity<Page<JobSummary>> response = controller.getAllJobs(pageable);
 
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expectedPage, response.getBody());
        verifyNoInteractions(userService);
    }
 
    @Test
    void testRemoveJob_shouldResolveCurrentUser_andDelegateToJobService() {
        UUID jobId = UUID.randomUUID();
        JobResponse expectedResponse = new JobResponse(jobId, "Backend Engineer", "Acme", "Remote");
 
        try (MockedStatic<SecurityContextHolder> mocked = mockAuthenticatedAs("recruiter@hireflux.com")) {
            when(userService.findUserByEmail("recruiter@hireflux.com")).thenReturn(recruiter);
            when(jobService.removeJob(jobId, recruiter)).thenReturn(expectedResponse);
 
            ResponseEntity<JobResponse> response = controller.removeJob(jobId);
 
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertSame(expectedResponse, response.getBody());
        }
    }
 
    @Test
    void testRemoveJob_shouldPropagateException_whenJobServiceRejectsUnauthorizedUser() {
        UUID jobId = UUID.randomUUID();
 
        try (MockedStatic<SecurityContextHolder> mocked = mockAuthenticatedAs("recruiter@hireflux.com")) {
            when(userService.findUserByEmail("recruiter@hireflux.com")).thenReturn(recruiter);
            when(jobService.removeJob(jobId, recruiter))
                    .thenThrow(new UnauthorizedException("Only the recruiter who posted the job can remove it"));
 
            assertThrows(UnauthorizedException.class, () -> controller.removeJob(jobId));
        }
    }
 
    @Test
    void testSearchJobs_shouldReturnOk_withoutRequiringCurrentUser() {
        JobSearchRequest request = new JobSearchRequest();
        Pageable pageable = PageRequest.of(0, 20);
        Page<JobDocument> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(jobSearchService.search(request, pageable)).thenReturn(emptyPage);
 
        ResponseEntity<Page<JobSearchResponse>> response = controller.searchJobs(request, pageable);
 
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getContent().isEmpty());
        verifyNoInteractions(userService);
    }
 
    @Test
    void testSearchJobs_shouldMapEveryJobDocumentField_toJobSearchResponse() {
        JobSearchRequest request = new JobSearchRequest();
        Pageable pageable = PageRequest.of(0, 20);
 
        JobDocument doc = new JobDocument();
        UUID jobId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();
        doc.setId(jobId);
        doc.setTitle("Backend Engineer");
        doc.setCompanyName("Acme Corp");
        doc.setLocation("Remote");
        doc.setJobType("FULL_TIME");
        doc.setRequiredSkills(List.of("Java", "Spring"));
        doc.setCreatedAt(createdAt);
 
        Page<JobDocument> searchResults = new PageImpl<>(List.of(doc), pageable, 1);
        when(jobSearchService.search(request, pageable)).thenReturn(searchResults);
 
        ResponseEntity<Page<JobSearchResponse>> response = controller.searchJobs(request, pageable);
 
        JobSearchResponse mapped = response.getBody().getContent().get(0);
        assertEquals(jobId, mapped.getId());
        assertEquals("Backend Engineer", mapped.getTitle());
        assertEquals("Acme Corp", mapped.getCompanyName());
        assertEquals("Remote", mapped.getLocation());
        assertEquals("FULL_TIME", mapped.getJobType());
        assertEquals(List.of("Java", "Spring"), mapped.getRequiredSkills());
        assertEquals(createdAt, mapped.getCreatedAt());
    }
 
    @Test
    void testSearchJobs_shouldMapMultipleResults_inOriginalOrder() {
        JobSearchRequest request = new JobSearchRequest();
        Pageable pageable = PageRequest.of(0, 20);
 
        JobDocument doc1 = new JobDocument();
        doc1.setId(UUID.randomUUID());
        doc1.setTitle("Backend Engineer");
 
        JobDocument doc2 = new JobDocument();
        doc2.setId(UUID.randomUUID());
        doc2.setTitle("Frontend Engineer");
 
        Page<JobDocument> searchResults = new PageImpl<>(List.of(doc1, doc2), pageable, 2);
        when(jobSearchService.search(request, pageable)).thenReturn(searchResults);
 
        ResponseEntity<Page<JobSearchResponse>> response = controller.searchJobs(request, pageable);
 
        List<JobSearchResponse> content = response.getBody().getContent();
        assertEquals(2, content.size());
        assertEquals("Backend Engineer", content.get(0).getTitle());
        assertEquals("Frontend Engineer", content.get(1).getTitle());
    }
 
    @Test
    void testSearchJobs_shouldPreserveTotalElementCount_fromOriginalSearchPage() {
        JobSearchRequest request = new JobSearchRequest();
        Pageable pageable = PageRequest.of(0, 1);
 
        JobDocument doc = new JobDocument();
        doc.setId(UUID.randomUUID());
        
        Page<JobDocument> searchResults = new PageImpl<>(List.of(doc), pageable, 50);
        when(jobSearchService.search(request, pageable)).thenReturn(searchResults);
 
        ResponseEntity<Page<JobSearchResponse>> response = controller.searchJobs(request, pageable);
 
        assertEquals(50, response.getBody().getTotalElements());
        assertEquals(1, response.getBody().getContent().size());
    }
 
    @Test
    void testSearchJobs_shouldPropagateException_whenSearchServiceFails() {
        JobSearchRequest request = new JobSearchRequest();
        Pageable pageable = PageRequest.of(0, 20);
        when(jobSearchService.search(request, pageable))
                .thenThrow(new RuntimeException("Elasticsearch unavailable"));
 
        assertThrows(RuntimeException.class, () -> controller.searchJobs(request, pageable));
    }
}
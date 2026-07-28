package com.sabarno.hireflux.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sabarno.hireflux.dto.response.AppResponse;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.exception.impl.ResourceNotFoundException;
import com.sabarno.hireflux.service.UserService;
import com.sabarno.hireflux.utility.projection.UserSummary;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {
 
    @Mock
    private UserService userService;
 
    private UserController controller;
 
    @BeforeEach
    void setUp() {
        controller = new UserController(userService);
    }
 
    @Test
    void testSaveJob_shouldResolveCurrentUser_andDelegateToUserService() {
        UUID jobId = UUID.randomUUID();
        User currentUser = new User();
        currentUser.setId(UUID.randomUUID());
 
        try (MockedStatic<SecurityContextHolder> mocked = mockAuthenticatedAs("jane@hireflux.com")) {
            when(userService.findUserByEmail("jane@hireflux.com")).thenReturn(currentUser);
 
            ResponseEntity<AppResponse> response = controller.saveJob(jobId);
 
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Saved the job successfully", response.getBody().getMessage());
            verify(userService).saveJob(jobId, currentUser);
        }
    }
 
    @Test
    void testSaveJob_shouldThrowNullPointerException_whenAuthenticatedEmailNotFoundInDatabase() {
        UUID jobId = UUID.randomUUID();
 
        try (MockedStatic<SecurityContextHolder> mocked = mockAuthenticatedAs("ghost@hireflux.com")) {
            when(userService.findUserByEmail("ghost@hireflux.com")).thenReturn(null);
            doThrow(new NullPointerException()).when(userService).saveJob(jobId, null);
 
            assertThrows(NullPointerException.class, () -> controller.saveJob(jobId));
        }
    }
 
    @Test
    void testSaveJob_shouldPropagateException_whenUserServiceRejectsRequest() {
        UUID jobId = UUID.randomUUID();
        User currentUser = new User();
 
        try (MockedStatic<SecurityContextHolder> mocked = mockAuthenticatedAs("jane@hireflux.com")) {
            when(userService.findUserByEmail("jane@hireflux.com")).thenReturn(currentUser);
            doThrow(new ResourceNotFoundException("Job not found"))
                    .when(userService).saveJob(jobId, currentUser);
 
            assertThrows(ResourceNotFoundException.class, () -> controller.saveJob(jobId));
        }
    }
 
    @Test
    void testGetProfile_shouldResolveCurrentUser_andReturnTheirProfileSummary() {
        User currentUser = new User();
        UUID userId = UUID.randomUUID();
        currentUser.setId(userId);
 
        UserSummary summary = mock(UserSummary.class);
 
        try (MockedStatic<SecurityContextHolder> mocked = mockAuthenticatedAs("jane@hireflux.com")) {
            when(userService.findUserByEmail("jane@hireflux.com")).thenReturn(currentUser);
            when(userService.getProfile(userId)).thenReturn(summary);
 
            ResponseEntity<UserSummary> response = controller.getProfile();
 
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertSame(summary, response.getBody());
            verify(userService).getProfile(userId);
        }
    }
 
    @Test
    void testGetProfile_shouldThrowNullPointerException_whenAuthenticatedEmailNotFoundInDatabase() {
        try (MockedStatic<SecurityContextHolder> mocked = mockAuthenticatedAs("ghost@hireflux.com")) {
            when(userService.findUserByEmail("ghost@hireflux.com")).thenReturn(null);
 
            assertThrows(NullPointerException.class, () -> controller.getProfile());
 
            verify(userService, never()).getProfile(any());
        }
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
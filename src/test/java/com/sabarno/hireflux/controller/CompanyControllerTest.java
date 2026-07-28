package com.sabarno.hireflux.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
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

import com.sabarno.hireflux.dto.request.CompanyRequest;
import com.sabarno.hireflux.dto.response.CompanyResponse;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.exception.impl.UnauthorizedException;
import com.sabarno.hireflux.service.CompanyService;
import com.sabarno.hireflux.service.UserService;
import com.sabarno.hireflux.utility.enums.UserRole;

@ExtendWith(MockitoExtension.class)
class CompanyControllerTest {
 
    @Mock
    private CompanyService companyService;
 
    @Mock
    private UserService userService;
 
    private CompanyController controller;
 
    @BeforeEach
    void setUp() {
        controller = new CompanyController(companyService, userService);
    }
 
    @Test
    void testCreateCompany_shouldResolveCurrentUser_andDelegateToCompanyService() {
        CompanyRequest request = new CompanyRequest();
        request.setName("Acme Corp");
 
        User currentUser = new User();
        currentUser.setRole(UserRole.RECRUITER);
 
        CompanyResponse expectedResponse = new CompanyResponse(
                UUID.randomUUID(), "Acme Corp", "https://acme.example.com", "We build widgets.");
 
        try (MockedStatic<SecurityContextHolder> mocked = mockAuthenticatedAs("recruiter@hireflux.com")) {
            when(userService.findUserByEmail("recruiter@hireflux.com")).thenReturn(currentUser);
            when(companyService.createCompany(request, currentUser)).thenReturn(expectedResponse);
 
            ResponseEntity<CompanyResponse> response = controller.createCompany(request);
 
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertSame(expectedResponse, response.getBody());
            verify(companyService).createCompany(request, currentUser);
        }
    }
 
    @Test
    void testCreateCompany_shouldPassNullUser_toCompanyService_whenAuthenticatedEmailNotFoundInDatabase() {
        CompanyRequest request = new CompanyRequest();
 
        try (MockedStatic<SecurityContextHolder> mocked = mockAuthenticatedAs("ghost@hireflux.com")) {
            when(userService.findUserByEmail("ghost@hireflux.com")).thenReturn(null);
            when(companyService.createCompany(eq(request), isNull()))
                    .thenThrow(new NullPointerException());
 
            assertThrows(NullPointerException.class, () -> controller.createCompany(request));
        }
    }
 
    @Test
    void testCreateCompany_shouldPropagateException_whenCompanyServiceRejectsUser() {
        CompanyRequest request = new CompanyRequest();
        User candidate = new User();
        candidate.setRole(UserRole.CANDIDATE);
 
        try (MockedStatic<SecurityContextHolder> mocked = mockAuthenticatedAs("candidate@hireflux.com")) {
            when(userService.findUserByEmail("candidate@hireflux.com")).thenReturn(candidate);
            when(companyService.createCompany(request, candidate))
                    .thenThrow(new UnauthorizedException("Only recruiters can create companies"));
 
            assertThrows(UnauthorizedException.class, () -> controller.createCompany(request));
        }
    }
 
    @Test
    void testGetCompany_shouldReturnOk_withCompanyResponse() {
        UUID companyId = UUID.randomUUID();
        CompanyResponse expectedResponse = new CompanyResponse(
                companyId, "Acme Corp", "https://acme.example.com", "We build widgets.");
        when(companyService.getCompany(companyId)).thenReturn(expectedResponse);
 
        ResponseEntity<CompanyResponse> response = controller.getCompany(companyId);
 
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expectedResponse, response.getBody());
    }
 
    @Test
    void testGetCompany_shouldPropagateException_whenCompanyNotFound() {
        UUID companyId = UUID.randomUUID();
        when(companyService.getCompany(companyId)).thenThrow(new RuntimeException("Company not found"));
 
        assertThrows(RuntimeException.class, () -> controller.getCompany(companyId));
    }
 
    @Test
    void testGetCompany_shouldNotRequireAuthentication() {
        UUID companyId = UUID.randomUUID();
        when(companyService.getCompany(companyId)).thenReturn(
                new CompanyResponse(companyId, "Acme", "https://acme.example.com", "desc"));
 
        controller.getCompany(companyId);
 
        verifyNoInteractions(userService);
    }
 
    @Test
    void testGetAllCompanies_shouldReturnOk_withListOfCompanies() {
        List<CompanyResponse> companies = List.of(
                new CompanyResponse(UUID.randomUUID(), "Acme", "https://acme.example.com", "desc1"),
                new CompanyResponse(UUID.randomUUID(), "Globex", "https://globex.example.com", "desc2")
        );
        when(companyService.getAllCompanies()).thenReturn(companies);
 
        ResponseEntity<List<CompanyResponse>> response = controller.getAllCompanies();
 
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        assertSame(companies, response.getBody());
    }
 
    @Test
    void testGetAllCompanies_shouldReturnOk_withEmptyList_whenNoneExist() {
        when(companyService.getAllCompanies()).thenReturn(List.of());
 
        ResponseEntity<List<CompanyResponse>> response = controller.getAllCompanies();
 
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
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
package com.sabarno.hireflux.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_shouldLogForAuthenticatedUser()
            throws ServletException, IOException {

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "test@test.com",
                        null,
                        Collections.emptyList()));

        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getStatus()).thenReturn(200);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        verify(request).getMethod();
        verify(request).getRequestURI();
        verify(response).getStatus();
    }

    @Test
    void doFilterInternal_shouldLogForAnonymousUser()
            throws ServletException, IOException {

        SecurityContextHolder.clearContext();

        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/login");
        when(response.getStatus()).thenReturn(401);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        verify(request).getMethod();
        verify(request).getRequestURI();
        verify(response).getStatus();
    }

    @Test
    void doFilterInternal_shouldExecuteFinally_whenFilterChainThrowsException()
            throws ServletException, IOException {

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "test@test.com",
                        null,
                        Collections.emptyList()));

        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getStatus()).thenReturn(500);

        doThrow(new ServletException("Filter error"))
                .when(filterChain)
                .doFilter(request, response);

        assertThrows(
                ServletException.class,
                () -> filter.doFilterInternal(
                        request,
                        response,
                        filterChain));

        verify(request).getMethod();
        verify(request).getRequestURI();
        verify(response).getStatus();
    }
}
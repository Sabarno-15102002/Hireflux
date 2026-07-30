package com.sabarno.hireflux.config;

import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @InjectMocks
    private OAuth2SuccessHandler successHandler;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oauth2User;

    private StringWriter stringWriter;

    @BeforeEach
    void setUp() throws Exception {

        stringWriter = new StringWriter();

        when(response.getWriter())
                .thenReturn(new PrintWriter(stringWriter));

        when(authentication.getPrincipal())
                .thenReturn(oauth2User);

        when(oauth2User.getAttribute("email"))
                .thenReturn("test@test.com");

        when(oauth2User.getAttribute("name"))
                .thenReturn("Test User");

        when(oauth2User.getAttribute("picture"))
                .thenReturn("profile-url");
    }

    @Test
    void onAuthenticationSuccess_shouldReturnTempToken_whenUserHasNoRole()
            throws Exception {

        User user = new User();
        user.setEmail("test@test.com");
        user.setRole(null);

        when(userService.findUserByEmail("test@test.com"))
                .thenReturn(user);

        when(jwtProvider.generateTempToken("test@test.com"))
                .thenReturn("temp-token");

        successHandler.onAuthenticationSuccess(
                request,
                response,
                authentication);

        verify(response)
                .setContentType("application/json");

        assertTrue(
                stringWriter.toString().contains("temp-token"));

        verify(jwtProvider)
                .generateTempToken("test@test.com");

        verify(jwtProvider, never())
                .generateTokenForOAuth(anyString(), anyString());
    }

    @Test
    void onAuthenticationSuccess_shouldCreateOAuthUser_whenUserDoesNotExist()
            throws Exception {

        User user = new User();
        user.setEmail("test@test.com");
        user.setRole(null);

        when(userService.findUserByEmail("test@test.com"))
                .thenReturn(null);

        when(userService.createOAuthUser(
                "test@test.com",
                "Test User",
                "profile-url"))
                .thenReturn(user);

        when(jwtProvider.generateTempToken("test@test.com"))
                .thenReturn("temp-token");

        successHandler.onAuthenticationSuccess(
                request,
                response,
                authentication);

        verify(userService)
                .createOAuthUser(
                        "test@test.com",
                        "Test User",
                        "profile-url");

        assertTrue(
                stringWriter.toString().contains("temp-token"));
    }

    @Test
    void onAuthenticationSuccess_shouldReturnJwt_whenUserHasRole()
            throws Exception {

        User user = new User();
        user.setEmail("test@test.com");
        user.setRole(com.sabarno.hireflux.utility.enums.UserRole.CANDIDATE);

        when(userService.findUserByEmail("test@test.com"))
                .thenReturn(user);

        when(jwtProvider.generateTokenForOAuth(
                "test@test.com",
                "CANDIDATE"))
                .thenReturn("jwt-token");

        successHandler.onAuthenticationSuccess(
                request,
                response,
                authentication);

        verify(response)
                .setStatus(HttpServletResponse.SC_OK);

        verify(response)
                .setContentType(MediaType.APPLICATION_JSON_VALUE);

        verify(jwtProvider)
                .generateTokenForOAuth(
                        "test@test.com",
                        "CANDIDATE");

        assertTrue(
                stringWriter.toString().contains("Generated token successfully"));
    }
}
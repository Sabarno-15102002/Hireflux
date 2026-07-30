package com.sabarno.hireflux.controller;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.sabarno.hireflux.config.JwtProvider;
import com.sabarno.hireflux.dto.request.LoginRequest;
import com.sabarno.hireflux.dto.request.RefreshTokenRequest;
import com.sabarno.hireflux.dto.request.RegisterRequest;
import com.sabarno.hireflux.dto.request.RoleRequestDTO;
import com.sabarno.hireflux.dto.response.AppResponse;
import com.sabarno.hireflux.dto.response.AuthResponse;
import com.sabarno.hireflux.entity.RefreshToken;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.exception.impl.BadRequestException;
import com.sabarno.hireflux.exception.impl.ConflictException;
import com.sabarno.hireflux.exception.impl.ResourceNotFoundException;
import com.sabarno.hireflux.service.CustomUserService;
import com.sabarno.hireflux.service.RefreshTokenService;
import com.sabarno.hireflux.service.UserService;
import com.sabarno.hireflux.service.util.RateLimitService;
import com.sabarno.hireflux.utility.RateLimitUtil;
import com.sabarno.hireflux.utility.enums.UserRole;

import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

        @InjectMocks
        private AuthController authController;

        @Mock
        private UserService userService;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Mock
        private JwtProvider jwtProvider;

        @Mock
        private CustomUserService customUserDetailsService;

        @Mock
        private RateLimitService rateLimitService;

        @Mock
        private RefreshTokenService refreshTokenService;

        @Mock
        private Bucket bucket;

        @Mock
        private HttpServletRequest request;

        private User user;

        private UserDetails userDetails;

        private RefreshToken refreshToken;

        @BeforeEach
        void setup() {

                user = new User();

                user.setId(UUID.randomUUID());
                user.setEmail("test@test.com");
                user.setName("Test User");

                userDetails = new org.springframework.security.core.userdetails.User(
                                "test@test.com",
                                "encodedPassword",
                                Collections.emptyList());

                refreshToken = new RefreshToken();

                refreshToken.setToken("refresh-token");

                refreshToken.setUser(user);
        }

        @AfterEach
        void cleanup() {

                SecurityContextHolder.clearContext();
        }

        // ---------------- REGISTER ----------------

        @Test
        void testCreateUser_shouldRegisterSuccessfully() {

                RegisterRequest request = new RegisterRequest();

                request.setEmail("test@test.com");
                request.setName("Test User");
                request.setPassword("password");
                request.setRole(UserRole.RECRUITER);

                when(userService.findUserByEmail("test@test.com"))
                                .thenReturn(null);

                when(passwordEncoder.encode("password"))
                                .thenReturn("encodedPassword");

                when(customUserDetailsService.loadUserByUsername("test@test.com"))
                                .thenReturn(userDetails);

                when(jwtProvider.generateToken(any(Authentication.class)))
                                .thenReturn("access-token");

                when(refreshTokenService.createRefreshToken(any(User.class)))
                                .thenReturn(refreshToken);

                ResponseEntity<AuthResponse> response = authController.createUserHandler(request);

                assertEquals(
                                HttpStatus.CREATED,
                                response.getStatusCode());

                assertTrue(response.getBody().getIsAuth());

                assertEquals(
                                "access-token",
                                response.getBody().getAccessToken());

                assertEquals(
                                "refresh-token",
                                response.getBody().getRefreshToken());

                verify(userService)
                                .createUser(any(User.class));
        }

        @Test
        void testCreateUser_shouldRegisterSuccessfully_withNoRole() {

                RegisterRequest request = new RegisterRequest();

                request.setEmail("test@test.com");
                request.setName("Test User");
                request.setPassword("password");

                when(userService.findUserByEmail("test@test.com"))
                                .thenReturn(null);

                when(passwordEncoder.encode("password"))
                                .thenReturn("encodedPassword");

                when(customUserDetailsService.loadUserByUsername("test@test.com"))
                                .thenReturn(userDetails);

                when(jwtProvider.generateToken(any(Authentication.class)))
                                .thenReturn("access-token");

                when(refreshTokenService.createRefreshToken(any(User.class)))
                                .thenReturn(refreshToken);

                ResponseEntity<AuthResponse> response = authController.createUserHandler(request);

                assertEquals(
                                HttpStatus.CREATED,
                                response.getStatusCode());

                assertTrue(response.getBody().getIsAuth());

                assertEquals(
                                "access-token",
                                response.getBody().getAccessToken());

                assertEquals(
                                "refresh-token",
                                response.getBody().getRefreshToken());

                verify(userService)
                                .createUser(any(User.class));
        }

        @Test
        void testCreateUser_shouldFail_whenEmailAlreadyExists() {

                RegisterRequest request = new RegisterRequest();

                request.setEmail("test@test.com");
                request.setPassword("password");

                when(userService.findUserByEmail("test@test.com"))
                                .thenReturn(user);

                assertThrows(
                                BadRequestException.class,
                                () -> authController.createUserHandler(request));

                verify(userService, never())
                                .createUser(any());
        }

        @Test
        void testCreateUser_shouldFail_whenAdminRoleProvided() {

                RegisterRequest request = new RegisterRequest();

                request.setEmail("test@test.com");
                request.setName("Admin Attempt");
                request.setPassword("password");
                request.setRole(UserRole.ADMIN);

                assertThrows(
                                BadRequestException.class,
                                () -> authController.createUserHandler(request));

                verifyNoInteractions(
                                userService,
                                passwordEncoder,
                                customUserDetailsService,
                                jwtProvider,
                                refreshTokenService);
        }

        // ---------------- LOGIN ----------------

        @Test
        void testLogin_shouldReturnToken_whenCredentialsValid() {

                LoginRequest requestDto = new LoginRequest();

                requestDto.setEmail("test@test.com");

                requestDto.setPassword("password");

                when(request.getRemoteAddr())
                                .thenReturn("127.0.0.1");

                when(rateLimitService.resolveBucket(
                                anyString(),
                                anyLong(),
                                any(Duration.class)))
                                .thenReturn(bucket);

                when(customUserDetailsService
                                .loadUserByUsername("test@test.com"))
                                .thenReturn(userDetails);

                when(passwordEncoder.matches(
                                "password",
                                "encodedPassword"))
                                .thenReturn(true);

                when(jwtProvider.generateToken(any(Authentication.class)))
                                .thenReturn("access-token");

                when(userService.findUserByEmail("test@test.com"))
                                .thenReturn(user);

                when(refreshTokenService.createRefreshToken(user))
                                .thenReturn(refreshToken);

                try (MockedStatic<RateLimitUtil> rateLimitMock = Mockito.mockStatic(RateLimitUtil.class)) {

                        ResponseEntity<AuthResponse> response = authController.loginUserHandler(
                                        requestDto,
                                        request);

                        assertEquals(
                                        HttpStatus.ACCEPTED,
                                        response.getStatusCode());

                        assertTrue(response.getBody().getIsAuth());

                }
        }

        @Test
        void testLogin_shouldReturnUnauthorized_whenPasswordInvalid() {

                LoginRequest requestDto = new LoginRequest();

                requestDto.setEmail("test@test.com");

                requestDto.setPassword("wrong");

                when(request.getRemoteAddr())
                                .thenReturn("127.0.0.1");

                when(rateLimitService.resolveBucket(
                                anyString(),
                                anyLong(),
                                any(Duration.class)))
                                .thenReturn(bucket);

                when(customUserDetailsService
                                .loadUserByUsername(anyString()))
                                .thenReturn(userDetails);

                when(passwordEncoder.matches(
                                anyString(),
                                anyString()))
                                .thenReturn(false);

                try (MockedStatic<RateLimitUtil> rateLimitMock = Mockito.mockStatic(RateLimitUtil.class)) {

                        ResponseEntity<AuthResponse> response = authController.loginUserHandler(
                                        requestDto,
                                        request);

                        assertEquals(
                                        HttpStatus.UNAUTHORIZED,
                                        response.getStatusCode());

                        assertFalse(
                                        response.getBody().getIsAuth());
                }
        }

        @Test
        void testLogin_shouldUseXForwardedForHeader_whenAvailable() {

                LoginRequest requestDto = new LoginRequest();

                requestDto.setEmail("test@test.com");
                requestDto.setPassword("password");

                when(request.getHeader("X-Forwarded-For"))
                                .thenReturn("10.0.0.1, 20.0.0.1");

                when(rateLimitService.resolveBucket(
                                anyString(),
                                anyLong(),
                                any(Duration.class)))
                                .thenReturn(bucket);

                when(customUserDetailsService
                                .loadUserByUsername("test@test.com"))
                                .thenReturn(userDetails);

                when(passwordEncoder.matches(
                                "password",
                                "encodedPassword"))
                                .thenReturn(true);

                when(jwtProvider.generateToken(any(Authentication.class)))
                                .thenReturn("access-token");

                when(userService.findUserByEmail("test@test.com"))
                                .thenReturn(user);

                when(refreshTokenService.createRefreshToken(user))
                                .thenReturn(refreshToken);

                try (MockedStatic<RateLimitUtil> rateLimitMock = Mockito.mockStatic(RateLimitUtil.class)) {

                        ResponseEntity<AuthResponse> response = authController.loginUserHandler(
                                        requestDto,
                                        request);

                        assertEquals(
                                        HttpStatus.ACCEPTED,
                                        response.getStatusCode());

                        verify(rateLimitService)
                                        .resolveBucket(
                                                        "login:10.0.0.1",
                                                        5,
                                                        Duration.ofMinutes(1));
                }
        }

        @Test
        void testLogin_shouldReturnUnauthorized_whenUserDoesNotExist() {

                LoginRequest requestDto = new LoginRequest();

                requestDto.setEmail("unknown@test.com");
                requestDto.setPassword("password");

                when(request.getRemoteAddr())
                                .thenReturn("127.0.0.1");

                when(rateLimitService.resolveBucket(
                                anyString(),
                                anyLong(),
                                any(Duration.class)))
                                .thenReturn(bucket);

                when(customUserDetailsService
                                .loadUserByUsername("unknown@test.com"))
                                .thenReturn(null);

                try (MockedStatic<RateLimitUtil> rateLimitMock = Mockito.mockStatic(RateLimitUtil.class)) {

                        ResponseEntity<AuthResponse> response = authController.loginUserHandler(
                                        requestDto,
                                        request);

                        assertEquals(
                                        HttpStatus.UNAUTHORIZED,
                                        response.getStatusCode());

                        assertFalse(
                                        response.getBody().getIsAuth());

                        assertEquals(
                                        "Invalid email or password",
                                        response.getBody().getMessage());
                }
        }

        // ---------------- SET ROLE ----------------

        @Test
        void testSetRole_shouldAssignRoleSuccessfully() {

                RoleRequestDTO dto = new RoleRequestDTO();

                dto.setRole("candidate");

                when(jwtProvider.getEmailFromTempToken("token"))
                                .thenReturn("test@test.com");

                when(userService.findUserByEmail("test@test.com"))
                                .thenReturn(user);

                when(jwtProvider.generateTokenForOAuth(
                                anyString(),
                                anyString()))
                                .thenReturn("oauth-token");

                when(refreshTokenService.createRefreshToken(user))
                                .thenReturn(refreshToken);

                ResponseEntity<AuthResponse> response = authController.setRole(
                                "token",
                                dto);

                assertEquals(
                                HttpStatus.OK,
                                response.getStatusCode());

                assertTrue(
                                response.getBody().getIsAuth());

                verify(userService)
                                .createUser(user);
        }

        @Test
        void testSetRole_shouldFail_whenUserNotFound() {

                RoleRequestDTO dto = new RoleRequestDTO();

                dto.setRole("candidate");

                when(jwtProvider.getEmailFromTempToken("token"))
                                .thenReturn("test@test.com");

                when(userService.findUserByEmail(anyString()))
                                .thenReturn(null);

                assertThrows(
                                ResourceNotFoundException.class,
                                () -> authController.setRole(
                                                "token",
                                                dto));
        }

        @Test
        void testSetRole_shouldFail_whenRoleAlreadyAssigned() {

                RoleRequestDTO dto = new RoleRequestDTO();

                dto.setRole("candidate");

                user.setRole(UserRole.CANDIDATE);

                when(jwtProvider.getEmailFromTempToken("token"))
                                .thenReturn(user.getEmail());

                when(userService.findUserByEmail(user.getEmail()))
                                .thenReturn(user);

                assertThrows(
                                ConflictException.class,
                                () -> authController.setRole(
                                                "token",
                                                dto));
        }

        @Test
        void testSetRole_shouldFail_whenInvalidRoleProvided() {

                RoleRequestDTO dto = new RoleRequestDTO();

                dto.setRole("invalid_role");

                when(jwtProvider.getEmailFromTempToken("token"))
                                .thenReturn("test@test.com");

                when(userService.findUserByEmail("test@test.com"))
                                .thenReturn(user);

                assertThrows(
                                BadRequestException.class,
                                () -> authController.setRole(
                                                "token",
                                                dto));

                verify(userService, never())
                                .createUser(any(User.class));

                verify(jwtProvider, never())
                                .generateTokenForOAuth(anyString(), anyString());

                verify(refreshTokenService, never())
                                .createRefreshToken(any(User.class));
        }
        // ---------------- REFRESH TOKEN ----------------

        @Test
        void testRefreshToken_shouldGenerateNewTokens() {

                RefreshTokenRequest request = new RefreshTokenRequest();

                request.setRefreshToken("old-token");

                when(refreshTokenService.verifyRefreshToken(
                                "old-token"))
                                .thenReturn(refreshToken);

                when(customUserDetailsService
                                .loadUserByUsername(user.getEmail()))
                                .thenReturn(userDetails);

                when(jwtProvider.generateToken(any(Authentication.class)))
                                .thenReturn("new-access-token");

                when(refreshTokenService.createRefreshToken(user))
                                .thenReturn(refreshToken);

                ResponseEntity<AuthResponse> response = authController.refreshAccessToken(request);

                assertEquals(
                                HttpStatus.OK,
                                response.getStatusCode());

                assertEquals(
                                "new-access-token",
                                response.getBody().getAccessToken());
        }

        @Test
        void testRefreshToken_shouldFail_whenTokenInvalid() {

                RefreshTokenRequest request = new RefreshTokenRequest();

                request.setRefreshToken("bad-token");

                when(refreshTokenService.verifyRefreshToken(
                                "bad-token"))
                                .thenReturn(null);

                assertThrows(
                                BadCredentialsException.class,
                                () -> authController.refreshAccessToken(request));
        }

        // ---------------- LOGOUT ----------------

        @Test
        void testLogout_shouldRevokeTokens() {

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                                "test@test.com",
                                null);

                SecurityContextHolder.getContext()
                                .setAuthentication(authentication);

                when(userService.findUserByEmail(
                                "test@test.com"))
                                .thenReturn(user);

                ResponseEntity<AppResponse> response = authController.logout();

                assertEquals(
                                HttpStatus.OK,
                                response.getStatusCode());

                verify(refreshTokenService)
                                .revokeUserTokens(user);
        }
}
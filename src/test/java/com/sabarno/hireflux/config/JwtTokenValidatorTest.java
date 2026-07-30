package com.sabarno.hireflux.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenValidatorTest {

        @Mock
        private HttpServletRequest request;

        @Mock
        private HttpServletResponse response;

        @Mock
        private FilterChain filterChain;

        private final JwtTokenValidator validator = new JwtTokenValidator();

        @AfterEach
        void cleanup() {
                SecurityContextHolder.clearContext();
        }

        @Test
        void testdoFilterInternal_shouldAuthenticate_whenValidJwtProvided()
                        throws IOException, ServletException {

                String jwt = generateValidToken();

                when(request.getHeader(JwtConstant.JWT_HEADER))
                                .thenReturn("Bearer " + jwt);

                validator.doFilterInternal(
                                request,
                                response,
                                filterChain);

                Authentication authentication = SecurityContextHolder
                                .getContext()
                                .getAuthentication();

                assertNotNull(authentication);

                assertEquals(
                                "test@test.com",
                                authentication.getPrincipal());

                assertTrue(
                                authentication.getAuthorities()
                                                .stream()
                                                .anyMatch(
                                                                a -> a.getAuthority()
                                                                                .equals("ROLE_USER")));

                verify(filterChain)
                                .doFilter(request, response);
        }

        @Test
        void testdoFilterInternal_shouldContinue_whenJwtHeaderMissing()
                        throws IOException, ServletException {

                when(request.getHeader(JwtConstant.JWT_HEADER))
                                .thenReturn(null);

                validator.doFilterInternal(
                                request,
                                response,
                                filterChain);

                assertNull(
                                SecurityContextHolder
                                                .getContext()
                                                .getAuthentication());

                verify(filterChain)
                                .doFilter(request, response);
        }

        @Test
        void testdoFilterInternal_shouldThrowException_whenJwtInvalid()
                        throws IOException, ServletException {

                when(request.getHeader(JwtConstant.JWT_HEADER))
                                .thenReturn("Bearer invalid-token");

                assertThrows(
                                BadCredentialsException.class,
                                () -> validator.doFilterInternal(
                                                request,
                                                response,
                                                filterChain));

                verify(filterChain, never())
                                .doFilter(request, response);
        }

        @Test
        void testdoFilterInternal_shouldThrowException_whenHeaderDoesNotContainBearer()
                        throws IOException, ServletException {

                when(request.getHeader(JwtConstant.JWT_HEADER))
                                .thenReturn("invalid-header");

                assertThrows(
                                BadCredentialsException.class,
                                () -> validator.doFilterInternal(
                                                request,
                                                response,
                                                filterChain));
        }

        @SuppressWarnings("deprecation")
        private String generateValidToken() {

                return Jwts.builder()
                        .claim(
                        "email",
                        "test@test.com")

                        .claim(
                        "authorities",
                        "ROLE_USER")

                        .setIssuedAt(new Date())

                        .setExpiration(new Date(System.currentTimeMillis() + 3600000))

                        .signWith(
                                SignatureAlgorithm.HS256,
                                JwtConstant.SECRET_KEY.getBytes())
                        .compact();
        }
}

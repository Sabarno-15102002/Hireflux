package com.sabarno.hireflux.config;

import com.sabarno.hireflux.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import org.junit.jupiter.api.Test;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtProviderTest {

    private final JwtProvider jwtProvider = new JwtProvider();

    @Test
    void testgenerateToken_shouldCreateValidAccessToken() {

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "test@test.com",
                null,
                List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_USER")));

        String token = jwtProvider.generateToken(authentication);

        assertNotNull(token);

        Claims claims = parseToken(token);

        assertEquals(
                "HireFlux",
                claims.getIssuer());

        assertEquals(
                "test@test.com",
                claims.get("email"));

        assertEquals(
                "ROLE_USER",
                claims.get("authorities"));
    }

    @Test
    void testgenerateRefreshToken_shouldCreateRefreshToken() {

        User user = new User();

        user.setEmail("test@test.com");

        String token = jwtProvider.generateRefreshToken(user);

        assertNotNull(token);

        Claims claims = parseToken(token);

        assertEquals(
                "HireFlux",
                claims.getIssuer());

        assertEquals(
                "test@test.com",
                claims.get("email"));

        assertEquals(
                "REFRESH",
                claims.get("type"));
    }

    @Test
    void testgenerateTempToken_shouldCreateTempToken() {

        String token = jwtProvider.generateTempToken(
                "test@test.com");

        assertNotNull(token);

        Claims claims = parseToken(token);

        assertEquals(
                "TEMP",
                claims.getSubject());

        assertEquals(
                "test@test.com",
                claims.get("email"));
    }

    @Test
    void testgetEmailFromJwtToken_shouldReturnEmail() {

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "test@test.com",
                null,
                List.of());

        String token = jwtProvider.generateToken(authentication);

        String email = jwtProvider.getEmailFromJwtToken(
                "Bearer " + token);

        assertEquals(
                "test@test.com",
                email);
    }

    @Test
    void testgetEmailFromTempToken_shouldReturnEmail() {

        String token = jwtProvider.generateTempToken(
                "test@test.com");

        String email = jwtProvider.getEmailFromTempToken(
                "Bearer " + token);

        assertEquals(
                "test@test.com",
                email);
    }

    @Test
    void testgetEmailFromTempToken_shouldThrowException_whenTokenTypeInvalid() {

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "test@test.com",
                null,
                List.of());

        String token = jwtProvider.generateToken(authentication);

        assertThrows(
                RuntimeException.class,
                () -> jwtProvider.getEmailFromTempToken(
                        "Bearer " + token));
    }

    @Test
    void testgenerateTokenForOAuth_shouldCreateOAuthToken() {

        String token = jwtProvider.generateTokenForOAuth(
                "test@test.com",
                "CANDIDATE");

        assertNotNull(token);

        Claims claims = parseToken(token);

        assertEquals(
                "test@test.com",
                claims.get("email"));

        assertEquals(
                "ROLE_CANDIDATE",
                claims.get("authorities"));

        assertEquals(
                "HireFlux",
                claims.getIssuer());
    }

    private Claims parseToken(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(
                        JwtConstant.SECRET_KEY.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
package com.sabarno.hireflux.config;

import java.util.Date;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.sabarno.hireflux.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtProvider {

    private static final long ACCESS_TOKEN_EXPIRATION = 1000L * 60 * 15; // 15 min

    private static final long REFRESH_TOKEN_EXPIRATION = 1000L * 60 * 60 * 24 * 7; // 7 days

    private SecretKey key = Keys.hmacShaKeyFor(JwtConstant.SECRET_KEY.getBytes());

    public String generateToken(Authentication authentication){

        String authorities = authentication.getAuthorities()
            .stream()
            .map(auth -> auth.getAuthority())
            .collect(Collectors.joining(","));

        return Jwts.builder().setIssuer("HireFlux")
                    .setIssuedAt(new Date()).setExpiration(new Date(new Date().getTime() + ACCESS_TOKEN_EXPIRATION))
                    .claim("email", authentication.getName())
                    .claim("authorities", authorities)
                    .signWith(key)
                    .compact();
    }

    public String generateRefreshToken(User user) {

        return Jwts.builder()
                .setIssuer("HireFlux")
                .claim("email", user.getEmail())
                .claim("type", "REFRESH")
                .setIssuedAt(new Date())
                .setExpiration(
                        new Date(System.currentTimeMillis()
                                + REFRESH_TOKEN_EXPIRATION)
                )
                .signWith(key)
                .compact();
    }

    public String generateTempToken(String email) {
        return Jwts.builder().setIssuer("HireFlux")
                    .setSubject("TEMP")
                    .setIssuedAt(new Date()).setExpiration(new Date(new Date().getTime() + 300000))
                    .claim("email", email)
                    .signWith(key)
                    .compact();
    }

    public String getEmailFromJwtToken(String token) {
        token = token.substring(7);
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();

        return String.valueOf(claims.get("email"));
    }

    public String getEmailFromTempToken(String token){
        token = token.substring(7);
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        if (!"TEMP".equals(claims.getSubject())) {
            throw new RuntimeException("Invalid token type");
        }

        return claims.get("email", String.class);
    }

    public String generateTokenForOAuth(String email, String role) {
        return Jwts.builder().setIssuer("HireFlux")
                    .setIssuedAt(new Date()).setExpiration(new Date(new Date().getTime() + 86400000))
                    .claim("email", email)
                    .claim("authorities", "ROLE_" + role)
                    .signWith(key)
                    .compact();
    }
}
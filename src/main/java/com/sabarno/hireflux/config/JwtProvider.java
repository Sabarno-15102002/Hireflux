package com.sabarno.hireflux.config;

import java.util.Date;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtProvider {

    private SecretKey key = Keys.hmacShaKeyFor(JwtConstant.SECRET_KEY.getBytes());

    public String generateToken(Authentication authentication){

        String authorities = authentication.getAuthorities()
            .stream()
            .map(auth -> auth.getAuthority())
            .collect(Collectors.joining(","));

        String jwt = Jwts.builder().setIssuer("HireFlux")
                    .setIssuedAt(new Date()).setExpiration(new Date(new Date().getTime() + 86400000))
                    .claim("email", authentication.getName())
                    .claim("authorities", authorities)
                    .signWith(key)
                    .compact();
        return jwt;
    }

    public String generateTempToken(String email) {
        String jwt = Jwts.builder().setIssuer("HireFlux")
                    .setSubject("TEMP")
                    .setIssuedAt(new Date()).setExpiration(new Date(new Date().getTime() + 300000))
                    .claim("email", email)
                    .signWith(key)
                    .compact();
        return jwt;
    }

    public String getEmailFromJwtToken(String token) {
        token = token.substring(7);
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();

        String email = String.valueOf(claims.get("email"));
        return email;
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
        String jwt = Jwts.builder().setIssuer("HireFlux")
                    .setIssuedAt(new Date()).setExpiration(new Date(new Date().getTime() + 86400000))
                    .claim("email", email)
                    .claim("authorities", "ROLE_" + role)
                    .signWith(key)
                    .compact();
        return jwt;
    }
}
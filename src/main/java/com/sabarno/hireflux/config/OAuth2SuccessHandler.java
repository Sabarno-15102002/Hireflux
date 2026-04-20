package com.sabarno.hireflux.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.sabarno.hireflux.dto.response.AuthResponse;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private UserService userService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        String profilePicture = oauthUser.getAttribute("picture");

        User user = userService.findUserByEmail(email);
        if (user == null) {
            user = userService.createOAuthUser(email, name, profilePicture);
        }

        if (user.getRole() == null) {
            String tempToken = jwtProvider.generateTempToken(email);
            // response.sendRedirect("http://localhost:3000/select-role?token=" + tempToken);
            response.setContentType("application/json");
            response.getWriter().write("{\"token\": \"" + tempToken + "\"}");
            return;
        }

        String jwt = jwtProvider.generateTokenForOAuth(user.getEmail(), user.getRole().name());
        System.out.println("OAuth handler triggered");
        System.out.println("Email: " + email);
        System.out.println("Role: " + user.getRole());


        ResponseEntity<AuthResponse> authResponse = ResponseEntity.ok(new AuthResponse(jwt, true, "Generated token successfully"));

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(authResponse.getBody().toString());
    }
}
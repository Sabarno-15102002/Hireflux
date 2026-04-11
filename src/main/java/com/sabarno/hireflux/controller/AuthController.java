package com.sabarno.hireflux.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sabarno.hireflux.config.JwtProvider;
import com.sabarno.hireflux.dto.RoleRequestDTO;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.exception.impl.BadRequestException;
import com.sabarno.hireflux.exception.impl.ConflictException;
import com.sabarno.hireflux.exception.impl.ResourceNotFoundException;
import com.sabarno.hireflux.response.AuthResponse;
import com.sabarno.hireflux.service.CustomUserService;
import com.sabarno.hireflux.service.UserService;
import com.sabarno.hireflux.utility.AuthProvider;
import com.sabarno.hireflux.utility.UserRole;

import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private CustomUserService customUserDetailsService;

    @Operation(summary = "Register a new user", description = "Creates a new user account and returns a JWT token upon successful registration")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> createUserHandler(@RequestBody User user) {
        String email = user.getEmail();
        String name = user.getName();
        String password = user.getPassword();

        User existingUser = userService.findUserByEmail(email);
        if (existingUser != null) {
            throw new BadRequestException("User already exists with email: " + email);
        }

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setName(name);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setAuthProvider(AuthProvider.EMAIL);
        newUser.setRole(user.getRole());
        userService.createUser(newUser);

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtProvider.generateToken(authentication);

        AuthResponse res = new AuthResponse();
        res.setToken(token);
        res.setIsAuth(true);
        res.setMessage("Token generated successfully");
        log.info("User registered: {}", email);
        log.info("res:"+ res.toString());
        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }

    @Operation(summary = "Login a user", description = "Authenticates a user and returns a JWT token upon successful login")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginUserHandler(@RequestBody User user) {
        String email = user.getEmail();
        String password = user.getPassword();

        try {
            Authentication authentication = authenticate(email, password);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String token = jwtProvider.generateToken(authentication);

            AuthResponse res = new AuthResponse();
            res.setToken(token);
            res.setIsAuth(true);
            res.setMessage("Token generated successfully");
            return new ResponseEntity<>(res, HttpStatus.ACCEPTED);
        } catch (BadCredentialsException e) {
            AuthResponse res = new AuthResponse();
            res.setToken(null);
            res.setIsAuth(false);
            res.setMessage("Invalid email or password");
            return new ResponseEntity<>(res, HttpStatus.UNAUTHORIZED);
        }

        
    }

    private Authentication authenticate(String email, String password) {

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        if (userDetails == null) {
            throw new BadCredentialsException("Invalid email");
        }

        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Operation(summary = "Set user role", description = "Sets the role for a user who logged in via OAuth for the first time using a temporary token")
    @PostMapping("/set-role")
    public ResponseEntity<AuthResponse> setRole(
            @RequestHeader("Authorization") String token,
            @RequestBody RoleRequestDTO role) {

        String email = jwtProvider.getEmailFromTempToken(token);

        User user = userService.findUserByEmail(email);

        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        if (user.getRole() != null) {
            throw new ConflictException("Role already assigned");
        }

        UserRole selectedRole;
        try {
            selectedRole = UserRole.valueOf(role.getRole().toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("Invalid role");
        }

        user.setRole(selectedRole);
        userService.createUser(user);

        String jwt = jwtProvider.generateTokenForOAuth(
                user.getEmail(),
                user.getRole().name());

        return ResponseEntity.ok(new AuthResponse(jwt, true, "Generated OAuth2 token"));
    }
}

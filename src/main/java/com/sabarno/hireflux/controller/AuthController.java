package com.sabarno.hireflux.controller;

import java.time.Duration;

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
import com.sabarno.hireflux.utility.enums.AuthProvider;
import com.sabarno.hireflux.utility.enums.UserRole;

import io.github.bucket4j.Bucket;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication Controller", description = "APIs for user registration, login, and role assignment")
@Slf4j
public class AuthController {

    private final UserService userService;

    private final PasswordEncoder passwordEncoder;

    private final JwtProvider jwtProvider;

    private final CustomUserService customUserDetailsService;

    private final RateLimitService rateLimitService;

    private final RefreshTokenService refreshTokenService;

    @Operation(summary = "Register a new user", description = "Creates a new user account and returns a JWT token upon successful registration")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> createUserHandler(@RequestBody RegisterRequest user) {
        String email = user.getEmail();
        String name = user.getName();
        String password = user.getPassword();

        User existingUser = userService.findUserByEmail(email);
        if (existingUser != null) {
            // Avoid user enumeration by returning a generic error message
            log.warn("Registration attempt for existing email: {}", email);
            throw new BadRequestException("Unable to register user");
        }

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setName(name);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setAuthProvider(AuthProvider.EMAIL);
        if(newUser.getRole() == null) {
            newUser.setRole(UserRole.CANDIDATE); // Default role
        }
        else if(newUser.getRole() == UserRole.ADMIN){
            throw new BadRequestException("Cannot assign ADMIN role during registration");
        } 
        else{
            newUser.setRole(user.getRole());
        }
        userService.createUser(newUser);

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtProvider.generateToken(authentication);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(newUser);

        AuthResponse res = new AuthResponse();
        res.setAccessToken(accessToken);
        res.setRefreshToken(refreshToken.getToken());
        res.setIsAuth(true);
        res.setMessage("Token generated successfully");
        log.info("User registered: {}", email);
        log.info("res:"+ res.toString());
        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }

    @Operation(summary = "Login a user", description = "Authenticates a user and returns a JWT token upon successful login")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginUserHandler(
        @RequestBody LoginRequest user,
        HttpServletRequest request
    ) {
        
        // Rate limiting
        String ip = getClientIp(request);

        Bucket bucket = rateLimitService.resolveBucket(
                "login:" + ip,
                5,
                Duration.ofMinutes(1)
        );

        RateLimitUtil.consume(
                bucket,
                "Too many login attempts"
        );

        // Existing login logic
        String email = user.getEmail();
        String password = user.getPassword();

        try {
            Authentication authentication = authenticate(email, password);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String accessToken = jwtProvider.generateToken(authentication);
            User userEntity = userService.findUserByEmail(email);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(userEntity);

            AuthResponse res = new AuthResponse();
            res.setAccessToken(accessToken);
            res.setRefreshToken(refreshToken.getToken());
            res.setIsAuth(true);
            res.setMessage("Token generated successfully");
            return new ResponseEntity<>(res, HttpStatus.ACCEPTED);
        } catch (BadCredentialsException e) {
            AuthResponse res = new AuthResponse();
            res.setAccessToken(null);
            res.setRefreshToken(null);
            res.setIsAuth(false);
            res.setMessage("Invalid email or password");
            return new ResponseEntity<>(res, HttpStatus.UNAUTHORIZED);
        }

        
    }

    private String getClientIp(HttpServletRequest request) {

        String xfHeader = request.getHeader("X-Forwarded-For");

        if (xfHeader == null) {
            return request.getRemoteAddr();
        }

        return xfHeader.split(",")[0];
    }

    private Authentication authenticate(String email, String password) {

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

        if (userDetails == null) {
            throw new BadCredentialsException("Invalid email");
        }

        if (password != null && !passwordEncoder.matches(password, userDetails.getPassword())) {
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

        String oAuthAccessToken = jwtProvider.generateTokenForOAuth(
                user.getEmail(),
                user.getRole().name());
        
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return ResponseEntity.ok(new AuthResponse(oAuthAccessToken, refreshToken.getToken(), true, "Generated OAuth2 token"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshAccessToken(@RequestBody RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(request.getRefreshToken());

        if (refreshToken == null) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        User user = refreshToken.getUser();
        Authentication authentication = authenticate(user.getEmail(), null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String accessToken = jwtProvider.generateToken(authentication);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);

        return ResponseEntity.ok(new AuthResponse(accessToken, newRefreshToken.getToken(), true, "Tokens refreshed successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<AppResponse> logout() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        User user = userService.findUserByEmail(email);

        refreshTokenService.revokeUserTokens(user);

        return ResponseEntity.ok(new AppResponse("Logged out successfully"));
    }
}

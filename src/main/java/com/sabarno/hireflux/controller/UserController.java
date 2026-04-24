package com.sabarno.hireflux.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sabarno.hireflux.dto.response.AppResponse;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/user")
@Tag(name = "User Controller", description = "APIs for managing user profiles and searching users")
public class UserController {

    @Autowired
    private UserService userService;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return userService.findUserByEmail(email);
    }

    @Operation(summary = "Save job for User", description = "Saves the job for a user.")
    @PostMapping("/save-job/{jobId}")
    public ResponseEntity<AppResponse> saveJob(
        @PathVariable UUID jobId
    ) {
        User user = getCurrentUser();
        userService.saveJob(jobId, user);
        AppResponse res = new AppResponse();
        res.setMessage("Saved the job successfully");
        return new ResponseEntity<>(res, HttpStatus.OK);
    }
}

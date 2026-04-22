package com.sabarno.hireflux.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.service.UserService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/user")
@Tag(name = "User Controller", description = "APIs for managing user profiles and searching users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/save-job/{jobId}")
    public ResponseEntity<User> saveJob(
        @RequestHeader("Authorization") String token,
        @PathVariable UUID jobId
    ) {
        User user = userService.saveJob(jobId, token);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }
}

package com.sabarno.hireflux.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sabarno.hireflux.dto.CompanyRequest;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.response.CompanyResponse;
import com.sabarno.hireflux.service.CompanyService;
import com.sabarno.hireflux.service.UserService;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<CompanyResponse> createCompany(
        @RequestBody CompanyRequest request,
        @RequestHeader("Authorization") String token) {
        User user = userService.findUserFromToken(token);
        return ResponseEntity.ok(companyService.createCompany(request, user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponse> getCompany(@PathVariable UUID id) {
        return ResponseEntity.ok(companyService.getCompany(id));
    }

    @GetMapping
    public ResponseEntity<List<CompanyResponse>> getAllCompanies() {
        return ResponseEntity.ok(companyService.getAllCompanies());
    }
}
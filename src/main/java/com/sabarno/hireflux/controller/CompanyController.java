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

import com.sabarno.hireflux.dto.request.CompanyRequest;
import com.sabarno.hireflux.dto.response.CompanyResponse;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.service.CompanyService;
import com.sabarno.hireflux.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/companies")
@Tag(name = "Company Controller", description = "APIs for managing company profiles and searching companies")
public class CompanyController {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private UserService userService;

    @Operation(summary = "Create a new company", description = "Creates a new company profile. Requires authentication.")
    @PostMapping
    public ResponseEntity<CompanyResponse> createCompany(
        @RequestBody CompanyRequest request,
        @RequestHeader("Authorization") String token) {
        User user = userService.findUserFromToken(token);
        return ResponseEntity.ok(companyService.createCompany(request, user));
    }

    @Operation(summary = "Get company details", description = "Retrieves details of a specific company by its ID.")
    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponse> getCompany(@PathVariable UUID id) {
        return ResponseEntity.ok(companyService.getCompany(id));
    }

    @Operation(summary = "Get all companies", description = "Retrieves a list of all registered companies.")
    @GetMapping
    public ResponseEntity<List<CompanyResponse>> getAllCompanies() {
        return ResponseEntity.ok(companyService.getAllCompanies());
    }
}
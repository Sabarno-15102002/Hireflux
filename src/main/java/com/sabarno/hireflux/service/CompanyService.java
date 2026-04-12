package com.sabarno.hireflux.service;

import java.util.List;
import java.util.UUID;

import com.sabarno.hireflux.dto.CompanyRequest;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.response.CompanyResponse;

public interface CompanyService {

    CompanyResponse createCompany(CompanyRequest request, User user);
    CompanyResponse getCompany(UUID id);
    List<CompanyResponse> getAllCompanies();
}
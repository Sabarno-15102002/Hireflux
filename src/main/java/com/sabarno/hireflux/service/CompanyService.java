package com.sabarno.hireflux.service;

import java.util.List;
import java.util.UUID;

import com.sabarno.hireflux.dto.request.CompanyRequest;
import com.sabarno.hireflux.dto.response.CompanyResponse;
import com.sabarno.hireflux.entity.User;

public interface CompanyService {

    CompanyResponse createCompany(CompanyRequest request, User user);
    CompanyResponse getCompany(UUID id);
    List<CompanyResponse> getAllCompanies();
}
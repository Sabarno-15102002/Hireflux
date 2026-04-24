package com.sabarno.hireflux.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sabarno.hireflux.dto.request.CompanyRequest;
import com.sabarno.hireflux.dto.response.CompanyResponse;
import com.sabarno.hireflux.entity.Company;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.exception.impl.UnauthorizedException;
import com.sabarno.hireflux.repository.CompanyRepository;
import com.sabarno.hireflux.repository.UserRepository;
import com.sabarno.hireflux.service.CompanyService;
import com.sabarno.hireflux.utility.enums.UserRole;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public CompanyResponse createCompany(CompanyRequest request, User user) {

        if (user.getRole() != UserRole.RECRUITER) {
            throw new UnauthorizedException("Only recruiters can create companies");
        }

        Company company = new Company();
        company.setName(request.getName());
        company.setWebsite(request.getWebsite());
        company.setDescription(request.getDescription());
        company.setCreatedAt(LocalDateTime.now());

        companyRepository.save(company);

        user.setCompany(company);
        userRepository.save(user);

        return mapToResponse(company);
    }

    @Override
    public CompanyResponse getCompany(UUID id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        return mapToResponse(company);
    }

    @Override
    public List<CompanyResponse> getAllCompanies() {
        return companyRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private CompanyResponse mapToResponse(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getWebsite(),
                company.getDescription()
        );
    }
}
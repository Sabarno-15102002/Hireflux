package com.sabarno.hireflux.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sabarno.hireflux.entity.Company;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {
    
}

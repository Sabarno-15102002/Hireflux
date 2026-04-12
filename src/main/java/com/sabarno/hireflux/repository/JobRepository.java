package com.sabarno.hireflux.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sabarno.hireflux.entity.Job;

public interface JobRepository extends JpaRepository<Job, UUID>{

}

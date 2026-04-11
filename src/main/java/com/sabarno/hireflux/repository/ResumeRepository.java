package com.sabarno.hireflux.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sabarno.hireflux.entity.Resume;
import com.sabarno.hireflux.entity.User;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    Resume findByUser(User user);
    Optional<Resume> findByFileKey(String fileKey);
}

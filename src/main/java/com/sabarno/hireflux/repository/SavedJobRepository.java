package com.sabarno.hireflux.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sabarno.hireflux.entity.SavedJob;

@Repository
public interface SavedJobRepository extends JpaRepository<SavedJob, UUID> {
    Page<SavedJob> findByUserId(UUID userId, Pageable pageable);
}

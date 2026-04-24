package com.sabarno.hireflux.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sabarno.hireflux.entity.Job;
import com.sabarno.hireflux.utility.enums.JobStatus;
import com.sabarno.hireflux.utility.projection.JobSummary;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {
    @EntityGraph(attributePaths = {"company"})
    Page<JobSummary> findByLocation(String location, Pageable pageable);

    @EntityGraph(attributePaths = {"company"})
    Page<JobSummary> findByStatus(JobStatus status, Pageable pageable);

    @Query("""
                SELECT j FROM Job j
                WHERE LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<Job> searchByTitle(String keyword, Pageable pageable);
}

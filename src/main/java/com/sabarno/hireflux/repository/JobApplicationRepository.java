package com.sabarno.hireflux.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sabarno.hireflux.entity.JobApplication;
import com.sabarno.hireflux.utility.projection.ApplicationSummary;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {

    Optional<JobApplication> findByApplicantIdAndJobId(UUID applicantId, UUID jobId);

    @EntityGraph(attributePaths = { "applicant", "job" })
    Page<ApplicationSummary> findByApplicantId(UUID applicantId, Pageable pageable);

    @EntityGraph(attributePaths = { "applicant", "job", "resume" })
    Page<ApplicationSummary> findByJobId(UUID jobId, Pageable pageable);

    @EntityGraph(attributePaths = { "applicant", "job" })
    @Query("""
        SELECT ja FROM JobApplication ja
        WHERE ja.job.id = :jobId
        ORDER BY ja.matchScore DESC
    """)
    Page<ApplicationSummary> findTopCandidates(UUID jobId, Pageable pageable);

    @Modifying
    @Query("""
        UPDATE JobApplication ja
        SET ja.status = 'REJECTED'
        WHERE ja.job.id = :jobId
    """)
    void rejectAllByJobId(UUID jobId);

}

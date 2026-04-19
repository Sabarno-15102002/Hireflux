package com.sabarno.hireflux.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sabarno.hireflux.entity.Job;
import com.sabarno.hireflux.entity.JobApplication;

public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {
    
    Optional<JobApplication> findByApplicantIdAndJobId(UUID applicantId, UUID jobId);
    List<JobApplication> findByApplicantId(UUID applicantId);
    List<JobApplication> findByJobId(UUID jobId);
    List<JobApplication> findByJobOrderByMatchScoreDesc(Job job);

}

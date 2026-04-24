package com.sabarno.hireflux.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.sabarno.hireflux.utility.enums.JobStatus;
import com.sabarno.hireflux.utility.enums.JobType;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.Data;

@Entity
@Data
@Table(
    indexes = {
        @Index(name = "idx_job_location", columnList = "location"),
        @Index(name = "idx_job_type", columnList = "jobType"),
        @Index(name = "idx_job_status", columnList = "status"),
        @Index(name = "idx_job_company", columnList = "company_id"),
        @Index(name = "idx_job_created_at", columnList = "createdAt")
    }
)
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    private String location;

    @Enumerated(EnumType.STRING)
    private JobType jobType;

    @Column(nullable = true)
    private Integer minExperienceRequired;

    @Column(nullable = true)
    private Integer maxExperienceRequired;

    @ElementCollection
    private List<String> requiredSkills;

    @ManyToOne
    @JoinColumn(name = "posted_by")
    private User postedBy;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String embedding;

    private LocalDateTime createdAt;
}
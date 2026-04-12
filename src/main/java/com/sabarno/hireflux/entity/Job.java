package com.sabarno.hireflux.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.sabarno.hireflux.utility.JobStatus;
import com.sabarno.hireflux.utility.JobType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity
@Data
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    private String companyName;

    private String location;

    @Enumerated(EnumType.STRING)
    private JobType jobType;

    @Column(nullable = true)
    private Integer minExperienceRequired;

    @Column(nullable = true)
    private Integer maxExperienceRequired;

    private List<String> requiredSkills;

    @ManyToOne
    @JoinColumn(name = "posted_by")
    private User postedBy;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    private LocalDateTime createdAt;
}
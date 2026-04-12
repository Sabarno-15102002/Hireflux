package com.sabarno.hireflux.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sabarno.hireflux.utility.ApplicationStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Entity
@Data
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"applicant_id", "job_id"})
})
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "applicant_id")
    private User applicant;

    @ManyToOne
    @JoinColumn(name = "job_id")
    private Job job;

    @ManyToOne
    @JoinColumn(name = "resume_id")
    private Resume resume;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status; 

    private LocalDateTime appliedAt;

    private LocalDateTime updatedAt;
}

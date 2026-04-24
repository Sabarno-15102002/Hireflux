package com.sabarno.hireflux.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sabarno.hireflux.utility.enums.ResumeUploadStatus;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
        @Index(name = "idx_resume_user", columnList = "user_id")
    }
)
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String fileName;

    @Column(unique = true, nullable = false)
    private String fileKey;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(columnDefinition = "TEXT")
    private String parsedData;

    @Enumerated(EnumType.STRING)
    private ResumeUploadStatus uploadStatus;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String embedding; // store as JSON array

    private LocalDateTime uploadedAt;

    private String errorMessage;
}

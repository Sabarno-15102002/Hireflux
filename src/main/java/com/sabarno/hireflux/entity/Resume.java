package com.sabarno.hireflux.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sabarno.hireflux.utility.ResumeUploadStatus;

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
    @Column(columnDefinition = "TEXT")
    private String parsedData;

    @Enumerated(EnumType.STRING)
    private ResumeUploadStatus uploadStatus;

    private LocalDateTime uploadedAt;

    private String errorMessage;
}

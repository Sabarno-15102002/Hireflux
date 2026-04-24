package com.sabarno.hireflux.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sabarno.hireflux.utility.enums.ResumeUploadStatus;

import lombok.Data;

@Data
public class ResumeResponse {
    private UUID id;
    private String fileName;
    private ResumeUploadStatus uploadStatus;
    private LocalDateTime uploadedAt;
}
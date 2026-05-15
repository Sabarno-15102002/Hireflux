package com.sabarno.hireflux.dto.event;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResumeUploadedEvent {
    private UUID resumeId;
    private String fileKey;
}

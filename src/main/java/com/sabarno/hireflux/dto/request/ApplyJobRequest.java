package com.sabarno.hireflux.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApplyJobRequest {

    @NotBlank(message = "Resume ID is required")
    private UUID resumeId;
}
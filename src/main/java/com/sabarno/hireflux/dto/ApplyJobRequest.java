package com.sabarno.hireflux.dto;

import java.util.UUID;

import lombok.Data;

@Data
public class ApplyJobRequest {
    private UUID resumeId;
}
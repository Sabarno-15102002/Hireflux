package com.sabarno.hireflux.dto.request;

import java.util.UUID;

import lombok.Data;

@Data
public class ApplyJobRequest {
    private UUID resumeId;
}
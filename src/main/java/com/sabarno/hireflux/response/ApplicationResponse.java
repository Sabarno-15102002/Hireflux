package com.sabarno.hireflux.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.sabarno.hireflux.utility.ApplicationStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ApplicationResponse {

    private UUID id;
    private UUID jobId;
    private String jobTitle;
    private String companyName;
    private ApplicationStatus status;
    private LocalDateTime appliedAt;
}
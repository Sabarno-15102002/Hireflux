package com.sabarno.hireflux.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class JobSearchResponse {

    private UUID id;
    private String title;
    private String companyName;
    private String location;
    private String jobType;
    private List<String> requiredSkills;
    private LocalDateTime createdAt;
}
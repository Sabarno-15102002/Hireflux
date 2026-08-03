package com.sabarno.hireflux.dto.request;

import java.util.List;

import com.sabarno.hireflux.utility.enums.JobType;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JobRequest {

    @NotBlank(message = "Job title is required")
    private String title;

    @NotBlank(message = "Job description is required")
    private String description;

    @NotBlank(message = "Job location is required")
    private String location;

    @NotBlank(message = "Job type is required")
    private JobType jobType;

    private Integer minExperienceRequired;
    private Integer maxExperienceRequired;
    private List<String> requiredSkills;
}

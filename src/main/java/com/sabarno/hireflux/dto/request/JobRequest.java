package com.sabarno.hireflux.dto.request;

import java.util.List;

import com.sabarno.hireflux.entity.Company;
import com.sabarno.hireflux.utility.enums.JobType;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JobRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    private String location;
    private JobType jobType;
    private Integer minExperienceRequired;
    private Integer maxExperienceRequired;
    private List<String> requiredSkills;
    private Company company;
}

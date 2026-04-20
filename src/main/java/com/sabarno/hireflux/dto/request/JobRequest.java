package com.sabarno.hireflux.dto.request;

import java.util.List;

import com.sabarno.hireflux.entity.Company;
import com.sabarno.hireflux.utility.JobType;

import lombok.Data;

@Data
public class JobRequest {
    private String title;
    private String description;
    private String companyName;
    private String location;
    private JobType jobType;
    private Integer minExperienceRequired;
    private Integer maxExperienceRequired;
    private List<String> requiredSkills;
    private Company company;
}

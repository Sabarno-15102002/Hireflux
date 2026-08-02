package com.sabarno.hireflux.dto.request;

import java.util.List;

import com.sabarno.hireflux.utility.enums.JobType;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JobSearchRequest {


    private String keyword;
    private String location;
    private List<String> skills;

    @NotBlank(message = "Job type is required")
    private JobType jobType;
}
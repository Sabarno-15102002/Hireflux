package com.sabarno.hireflux.dto.request;

import java.util.List;

import com.sabarno.hireflux.utility.enums.JobType;

import lombok.Data;

@Data
public class JobSearchRequest {

    private String keyword;
    private String location;
    private List<String> skills;
    private JobType jobType;
}
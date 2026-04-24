package com.sabarno.hireflux.utility.projection;

import java.util.UUID;

import com.sabarno.hireflux.utility.enums.JobType;

public interface JobSummary {

    UUID getId();
    String getTitle();
    String getLocation();
    JobType getJobType();
    
    CompanySummary getCompany();

}

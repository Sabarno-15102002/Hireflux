package com.sabarno.hireflux.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardAnalyticsResponse {

    private long totalUsers;

    private long totalJobs;

    private long totalApplications;

    private long activeRecruiters;
}
package com.sabarno.hireflux.service;

import java.util.List;
import java.util.UUID;

import com.sabarno.hireflux.dto.request.AdminInviteRequest;
import com.sabarno.hireflux.dto.request.CompleteInviteRequest;
import com.sabarno.hireflux.dto.response.DashboardAnalyticsResponse;
import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.utility.projection.SkillAnalyticsProjection;

public interface AdminService {
    void inviteUser(AdminInviteRequest request, User admin);
    void completeInvite(CompleteInviteRequest request);
    void updateUserRole(UUID userId, String newRole);
    DashboardAnalyticsResponse getDashboardStats();
    List<SkillAnalyticsProjection> getTopSkills();

}

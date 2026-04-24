package com.sabarno.hireflux.utility.projection;

import java.util.UUID;

import com.sabarno.hireflux.utility.enums.UserRole;

public interface UserSummary {

    UUID getId();
    String getName();
    String getEmail();
    String getProfilePicture();
    UserRole getRole();

    CompanyMini getCompany();
}
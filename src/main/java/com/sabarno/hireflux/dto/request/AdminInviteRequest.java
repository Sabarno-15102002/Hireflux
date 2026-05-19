package com.sabarno.hireflux.dto.request;

import com.sabarno.hireflux.utility.enums.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminInviteRequest {
    @Email
    private String email;

    @NotNull
    private UserRole role;
}

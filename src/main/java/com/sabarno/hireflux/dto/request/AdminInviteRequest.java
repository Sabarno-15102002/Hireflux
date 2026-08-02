package com.sabarno.hireflux.dto.request;

import com.sabarno.hireflux.utility.enums.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminInviteRequest {
    
    @NotBlank(message = "Email is required")
    @Email
    private String email;

    @NotBlank(message = "Role is required")
    private UserRole role;
}

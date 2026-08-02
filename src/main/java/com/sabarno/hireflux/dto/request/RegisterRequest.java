package com.sabarno.hireflux.dto.request;

import com.sabarno.hireflux.utility.enums.UserRole;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
    
    private UserRole role;
}

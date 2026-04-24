package com.sabarno.hireflux.dto.request;

import com.sabarno.hireflux.utility.enums.UserRole;

import lombok.Data;

@Data
public class RegisterRequest {
    private String name;
    private String email;
    private String password;
    private UserRole role;
}

package com.sabarno.hireflux.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompleteInviteRequest {
    @NotBlank
    private String token;

    @NotBlank
    private String name;

    @NotBlank
    private String password;

}

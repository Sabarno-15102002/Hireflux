package com.sabarno.hireflux.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompanyRequest {

    @NotBlank(message = "Company name is required")
    private String name;

    @NotBlank(message = "Company website is required")
    private String website;

    @NotBlank(message = "Company description is required")
    private String description;
}
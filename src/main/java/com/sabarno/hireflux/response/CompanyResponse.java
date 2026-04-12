package com.sabarno.hireflux.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CompanyResponse {
    private UUID id;
    private String name;
    private String website;
    private String description;
}
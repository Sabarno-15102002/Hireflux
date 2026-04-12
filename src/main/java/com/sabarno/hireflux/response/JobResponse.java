package com.sabarno.hireflux.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JobResponse {
    private UUID id;
    private String title;
    private String companyName;
    private String location;
}

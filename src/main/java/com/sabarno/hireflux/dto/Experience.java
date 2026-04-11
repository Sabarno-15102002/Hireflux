package com.sabarno.hireflux.dto;

import lombok.Data;

@Data
public class Experience {
    private String role;
    private String company;
    private String location;
    private String description;
    private String from;
    private String to;
}

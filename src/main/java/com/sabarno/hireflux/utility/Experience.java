package com.sabarno.hireflux.utility;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Experience {
    private String role;
    private String company;
    private String location;
    private String description;
    private LocalDateTime from;
    private LocalDateTime to;
}

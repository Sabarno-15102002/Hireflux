package com.sabarno.hireflux.utility;

import java.util.List;

import lombok.Data;

@Data
public class Project {
    private String title;
    private String description;
    private List<String> technologies;
    private String link;
    private String from;
    private String to;
}

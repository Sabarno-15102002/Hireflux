package com.sabarno.hireflux.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ResumeParsedData {

    private String fileName = "";

    private List<String> skills = new ArrayList<>();

    private List<Education> education = new ArrayList<>();

    private List<Experience> experience = new ArrayList<>();

    private List<Project> projects = new ArrayList<>();
    
}

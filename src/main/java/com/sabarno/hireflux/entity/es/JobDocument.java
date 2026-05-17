package com.sabarno.hireflux.entity.es;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.elasticsearch.annotations.Document;

import jakarta.persistence.Id;
import lombok.Data;

@Data
@Document(indexName = "jobs")
public class JobDocument {

    @Id
    private UUID id;
    private String title;
    private String description;
    private String companyName;
    private String location;
    private String jobType;
    private List<String> requiredSkills;
    private Integer minExperienceRequired;
    private Integer maxExperienceRequired;
    private String embedding;
    private LocalDateTime createdAt;
}
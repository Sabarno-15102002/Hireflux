package com.sabarno.hireflux.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.Data;

@Entity
@Data
@Table(
    indexes = {
        @Index(name = "idx_company_name", columnList = "name"),
        @Index(name = "idx_company_industry", columnList = "industry"),
        @Index(name = "idx_company_location", columnList = "location")
    }
)
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String name;

    private String website;

    private String logoUrl;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    private String industry;

    private String location;

    private Integer size;

    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    private List<Job> jobs;

    private LocalDateTime createdAt;
}
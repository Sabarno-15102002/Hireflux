package com.sabarno.hireflux.entity;

import java.time.LocalDateTime;

import com.sabarno.hireflux.utility.SkillEdgeId;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

@Entity
@Table(name = "skill_edges")
@Data
@IdClass(SkillEdgeId.class)
public class SkillEdge {

    @Id
    private String skillA;

    @Id
    private String skillB;

    private double weight;
    private int coOccurrence;

    private LocalDateTime updatedAt;

    @Version
    private Long version;
}
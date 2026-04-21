package com.sabarno.hireflux.utility;

import java.io.Serializable;

import lombok.Data;

@Data
public class SkillEdgeId implements Serializable {
    private String skillA;
    private String skillB;
}
package com.sabarno.hireflux.service;

import java.util.List;

public interface SkillGraphService {
    void updateGraph(List<String> skills);
    double getSimilarity(String a, String b);
}

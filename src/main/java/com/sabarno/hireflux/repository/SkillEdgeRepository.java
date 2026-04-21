package com.sabarno.hireflux.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sabarno.hireflux.entity.SkillEdge;
import com.sabarno.hireflux.utility.SkillEdgeId;

@Repository
public interface SkillEdgeRepository extends JpaRepository<SkillEdge, SkillEdgeId> {

    Optional<SkillEdge> findBySkillAAndSkillB(String skillA, String skillB);
}
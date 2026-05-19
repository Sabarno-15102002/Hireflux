package com.sabarno.hireflux.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sabarno.hireflux.entity.User;
import com.sabarno.hireflux.utility.projection.SkillAnalyticsProjection;
import com.sabarno.hireflux.utility.projection.UserSummary;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {"company"})
    Optional<UserSummary> findProfileById(UUID id);

    Page<UserSummary> findAllProjectedBy(Pageable pageable);

    @Query("""
        SELECT COUNT(u)
        FROM User u
        WHERE u.role = 'RECRUITER'
    """)
    long countRecruiters();

    @Query(value = """
        SELECT skill AS skill, COUNT(*) AS count
        FROM (
            SELECT unnest(skills) AS skill
            FROM user_skills
        ) s
        GROUP BY skill
        ORDER BY count DESC
        LIMIT 20
    """, nativeQuery = true)
    List<SkillAnalyticsProjection> getTopSkills();
}

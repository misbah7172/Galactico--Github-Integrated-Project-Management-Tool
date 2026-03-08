package com.autotrack.repository;

import com.autotrack.model.Team;
import com.autotrack.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Team entity.
 */
@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    
    @Query("SELECT DISTINCT t FROM Team t LEFT JOIN FETCH t.members LEFT JOIN FETCH t.projects JOIN t.members m WHERE m = :user AND t.deletedAt IS NULL")
    List<Team> findTeamsByUser(@Param("user") User user);

    @Query("SELECT t FROM Team t LEFT JOIN FETCH t.members LEFT JOIN FETCH t.projects WHERE t.id = :id")
    java.util.Optional<Team> findByIdWithDetails(@Param("id") Long id);
}

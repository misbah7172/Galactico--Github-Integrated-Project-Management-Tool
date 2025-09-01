package com.autotrack.repository;

import com.autotrack.model.Team;
import com.autotrack.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Team entity.
 */
@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    
    @Query("SELECT t FROM Team t JOIN t.members m WHERE m = :user")
    List<Team> findTeamsByUser(User user);
}

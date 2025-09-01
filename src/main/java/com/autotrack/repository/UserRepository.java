package com.autotrack.repository;

import com.autotrack.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for User entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByGitHubId(String gitHubId);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByNickname(String nickname);
    
    // Alias for username lookup (nickname is the username)
    default Optional<User> findByUsername(String username) {
        return findByNickname(username);
    }
    
    /**
     * Find users who are members of teams that have access to a specific project
     */
    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN u.teams t " +
           "JOIN t.projects p " +
           "WHERE p.id = :projectId")
    List<User> findByTeams_Projects_Id(@Param("projectId") Long projectId);
}

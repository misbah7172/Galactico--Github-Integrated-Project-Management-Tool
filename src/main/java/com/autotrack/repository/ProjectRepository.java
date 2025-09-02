package com.autotrack.repository;

import com.autotrack.model.Project;
import com.autotrack.model.Team;
import com.autotrack.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Project entity.
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    
    // Only find non-deleted projects
    List<Project> findByTeamAndDeletedAtIsNull(Team team);
    
    @Query("SELECT p FROM Project p WHERE p.owner.id = :ownerId AND p.deletedAt IS NULL")
    List<Project> findByOwnerAndDeletedAtIsNull(@Param("ownerId") Long ownerId);
    
    @Query("SELECT p FROM Project p WHERE p.deletedAt IS NULL")
    List<Project> findAllByDeletedAtIsNull();
    
    Optional<Project> findByGitHubRepoIdAndDeletedAtIsNull(String gitHubRepoId);
    
    // Find non-deleted project by ID
    @Query("SELECT p FROM Project p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Project> findByIdAndDeletedAtIsNull(@Param("id") Long id);
    
    @Query("SELECT p FROM Project p WHERE p.team = :team AND p.owner = :createdBy AND p.deletedAt IS NULL")
    List<Project> findByTeamAndCreatedByAndDeletedAtIsNull(@Param("team") Team team, @Param("createdBy") User createdBy);
    
    // Legacy methods for backward compatibility (now filter deleted)
    default List<Project> findByTeam(Team team) {
        return findByTeamAndDeletedAtIsNull(team);
    }
    
    default List<Project> findByOwner(Long ownerId) {
        return findByOwnerAndDeletedAtIsNull(ownerId);
    }
    
    default Optional<Project> findByGitHubRepoId(String gitHubRepoId) {
        return findByGitHubRepoIdAndDeletedAtIsNull(gitHubRepoId);
    }
}

package com.autotrack.repository;

import com.autotrack.model.Project;
import com.autotrack.model.Sprint;
import com.autotrack.model.SprintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Sprint entity operations.
 */
@Repository
public interface SprintRepository extends JpaRepository<Sprint, Long> {
    
    /**
     * Find all sprints by project
     */
    List<Sprint> findByProjectOrderByStartDateDesc(Project project);
    
    /**
     * Find all sprints by project with pagination
     */
    Page<Sprint> findByProjectOrderByStartDateDesc(Project project, Pageable pageable);
    
    /**
     * Find all sprints by project ID
     */
    @Query("SELECT s FROM Sprint s WHERE s.project.id = :projectId ORDER BY s.startDate DESC")
    List<Sprint> findByProjectIdOrderByStartDateDesc(@Param("projectId") Long projectId);
    
    /**
     * Find sprints by status
     */
    List<Sprint> findByStatusOrderByStartDateDesc(SprintStatus status);
    
    /**
     * Find sprints by project and status
     */
    List<Sprint> findByProjectAndStatusOrderByStartDateDesc(Project project, SprintStatus status);
    
    /**
     * Find active sprint for a project (should be only one)
     */
    @Query("SELECT s FROM Sprint s WHERE s.project.id = :projectId AND s.status = 'ACTIVE'")
    Optional<Sprint> findActiveSprintByProjectId(@Param("projectId") Long projectId);
    
    /**
     * Find sprints that should be automatically activated (status=UPCOMING and start date has passed)
     */
    @Query("SELECT s FROM Sprint s WHERE s.status = 'UPCOMING' AND s.startDate <= :currentTime")
    List<Sprint> findSprintsToActivate(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find sprints that should be automatically completed (status=ACTIVE and end date has passed)
     */
    @Query("SELECT s FROM Sprint s WHERE s.status = 'ACTIVE' AND s.endDate <= :currentTime")
    List<Sprint> findSprintsToComplete(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find sprints ending within specified days with incomplete tasks
     */
    @Query("SELECT DISTINCT s FROM Sprint s " +
           "LEFT JOIN s.tasks t " +
           "WHERE s.status = 'ACTIVE' " +
           "AND s.endDate BETWEEN :now AND :endTime " +
           "AND EXISTS (SELECT 1 FROM Task task WHERE task.sprint = s AND task.status != 'DONE')")
    List<Sprint> findActiveSprintsEndingSoonWithIncompleteTasks(
            @Param("now") LocalDateTime now, 
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * Find sprint by name and project (for validation)
     */
    Optional<Sprint> findByNameAndProject(String name, Project project);
    
    /**
     * Check if there are overlapping sprints for a project
     */
    @Query("SELECT COUNT(s) FROM Sprint s WHERE s.project.id = :projectId " +
           "AND s.id != :sprintId " +
           "AND s.status != 'CANCELLED' " +
           "AND ((s.startDate <= :startDate AND s.endDate >= :startDate) " +
           "OR (s.startDate <= :endDate AND s.endDate >= :endDate) " +
           "OR (s.startDate >= :startDate AND s.endDate <= :endDate))")
    long countOverlappingSprints(@Param("projectId") Long projectId,
                                @Param("sprintId") Long sprintId,
                                @Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get sprint statistics for dashboard
     */
    @Query("SELECT s.status, COUNT(s) FROM Sprint s WHERE s.project.id = :projectId GROUP BY s.status")
    List<Object[]> getSprintStatsByProject(@Param("projectId") Long projectId);
}

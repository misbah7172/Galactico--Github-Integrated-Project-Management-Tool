package com.autotrack.repository;

import com.autotrack.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Repository interface for TaskHistory entities.
 * Provides data access methods for task timeline and history functionality.
 */
@Repository
public interface TaskHistoryRepository extends JpaRepository<TaskHistory, Long> {
    
    /**
     * Find task history by task with pagination
     */
    Page<TaskHistory> findByTaskOrderByTimestampDesc(Task task, Pageable pageable);
    
    /**
     * Find task history by project with pagination
     */
    Page<TaskHistory> findByProjectOrderByTimestampDesc(Project project, Pageable pageable);
    
    /**
     * Find task history by sprint with pagination
     */
    @Query("SELECT th FROM TaskHistory th WHERE th.sprint.id = :sprintId ORDER BY th.timestamp DESC")
    Page<TaskHistory> findBySprintIdOrderByTimestampDesc(@Param("sprintId") Long sprintId, Pageable pageable);
    
    /**
     * Find task history by user with pagination
     */
    Page<TaskHistory> findByUserOrderByTimestampDesc(User user, Pageable pageable);
    
    /**
     * Find task history by user and project with pagination
     */
    Page<TaskHistory> findByUserAndProjectOrderByTimestampDesc(User user, Project project, Pageable pageable);
    
    /**
     * Find task history with filters
     */
    @Query("SELECT th FROM TaskHistory th WHERE th.project = :project " +
           "AND (:actionType IS NULL OR th.actionType = :actionType) " +
           "AND (:userId IS NULL OR th.user.id = :userId) " +
           "AND (:startDate IS NULL OR th.timestamp >= :startDate) " +
           "AND (:endDate IS NULL OR th.timestamp <= :endDate) " +
           "ORDER BY th.timestamp DESC")
    Page<TaskHistory> findByProjectWithFilters(
        @Param("project") Project project,
        @Param("actionType") HistoryActionType actionType,
        @Param("userId") Long userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable);
    
    /**
     * Get activity counts by day for the last N days
     */
    @Query("SELECT DATE(th.timestamp) as date, COUNT(th) as count " +
           "FROM TaskHistory th " +
           "WHERE th.project.id = :projectId " +
           "AND th.timestamp >= :startDate " +
           "GROUP BY DATE(th.timestamp) " +
           "ORDER BY DATE(th.timestamp) DESC")
    List<Object[]> getActivityCountsByDay(@Param("projectId") Long projectId, 
                                         @Param("startDate") LocalDateTime startDate);
    
    /**
     * Get activity counts by type for the last N days
     */
    @Query("SELECT th.actionType as actionType, COUNT(th) as count " +
           "FROM TaskHistory th " +
           "WHERE th.project.id = :projectId " +
           "AND th.timestamp >= :startDate " +
           "GROUP BY th.actionType " +
           "ORDER BY COUNT(th) DESC")
    List<Object[]> getActivityCountsByType(@Param("projectId") Long projectId, 
                                          @Param("startDate") LocalDateTime startDate);
    
    /**
     * Get most active users for the last N days
     */
    @Query("SELECT th.user.id as userId, th.user.nickname as username, " +
           "th.user.avatarUrl as avatarUrl, COUNT(th) as activityCount " +
           "FROM TaskHistory th " +
           "WHERE th.project.id = :projectId " +
           "AND th.timestamp >= :startDate " +
           "GROUP BY th.user.id, th.user.nickname, th.user.avatarUrl " +
           "ORDER BY COUNT(th) DESC")
    List<Object[]> getMostActiveUsers(@Param("projectId") Long projectId, 
                                     @Param("startDate") LocalDateTime startDate,
                                     Pageable pageable);
    
    /**
     * Get recent critical activities
     */
    @Query("SELECT th FROM TaskHistory th " +
           "WHERE th.project.id = :projectId " +
           "AND th.actionType IN ('TASK_BLOCKED', 'PRIORITY_CHANGED', 'STATUS_CHANGED', 'SPRINT_CHANGED') " +
           "ORDER BY th.timestamp DESC")
    List<TaskHistory> getRecentCriticalActivities(@Param("projectId") Long projectId, Pageable pageable);
    
    /**
     * Count activities for a project in a date range
     */
    @Query("SELECT COUNT(th) FROM TaskHistory th " +
           "WHERE th.project.id = :projectId " +
           "AND th.timestamp BETWEEN :startDate AND :endDate")
    Long countActivitiesInRange(@Param("projectId") Long projectId,
                               @Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find recent activities for a user
     */
    @Query("SELECT th FROM TaskHistory th WHERE th.user.id = :userId " +
           "ORDER BY th.timestamp DESC")
    Page<TaskHistory> findByUserIdOrderByTimestampDesc(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Find user activities for a specific project
     */
    @Query("SELECT th FROM TaskHistory th WHERE th.user.id = :userId " +
           "AND th.project.id = :projectId " +
           "ORDER BY th.timestamp DESC")
    Page<TaskHistory> findByUserIdAndProjectIdOrderByTimestampDesc(
        @Param("userId") Long userId, 
        @Param("projectId") Long projectId, 
        Pageable pageable);
}
package com.autotrack.repository;

import com.autotrack.model.BacklogItem;
import com.autotrack.model.BacklogStatus;
import com.autotrack.model.PriorityLevel;
import com.autotrack.model.Project;
import com.autotrack.model.Sprint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for BacklogItem entity operations.
 */
@Repository
public interface BacklogItemRepository extends JpaRepository<BacklogItem, Long> {

    /**
     * Find all backlog items for a specific project ordered by priority.
     */
    @Query("SELECT b FROM BacklogItem b WHERE b.project = :project ORDER BY b.priorityLevel DESC, b.priorityRank ASC")
    List<BacklogItem> findByProjectOrderByPriority(@Param("project") Project project);

    /**
     * Find backlog items by project and status.
     */
    List<BacklogItem> findByProjectAndBacklogStatusOrderByPriorityLevelDescPriorityRankAsc(Project project, BacklogStatus status);

    /**
     * Find backlog items assigned to a specific sprint.
     */
    List<BacklogItem> findBySprintOrderByPriorityLevelDescPriorityRankAsc(Sprint sprint);
    
    /**
     * Find backlog items by sprint with pagination
     */
    Page<BacklogItem> findBySprintOrderByPriorityRankAsc(Sprint sprint, Pageable pageable);

    /**
     * Find backlog items by project and priority level.
     */
    List<BacklogItem> findByProjectAndPriorityLevelOrderByPriorityRankAsc(Project project, PriorityLevel priorityLevel);

    /**
     * Find product backlog items (not assigned to any sprint) for a project.
     */
    @Query("SELECT b FROM BacklogItem b WHERE b.project = :project AND b.backlogStatus = 'PRODUCT_BACKLOG' ORDER BY b.priorityLevel DESC, b.priorityRank ASC")
    List<BacklogItem> findProductBacklogByProject(@Param("project") Project project);

    /**
     * Find backlog items by epic name.
     */
    List<BacklogItem> findByProjectAndEpicNameOrderByPriorityRankAsc(Project project, String epicName);

    /**
     * Find backlog item by title and project (for duplicate checking).
     */
    Optional<BacklogItem> findByProjectAndTitle(Project project, String title);

    /**
     * Count backlog items by status for a project.
     */
    @Query("SELECT COUNT(b) FROM BacklogItem b WHERE b.project = :project AND b.backlogStatus = :status")
    long countByProjectAndStatus(@Param("project") Project project, @Param("status") BacklogStatus status);

    /**
     * Get total story points for a project by status.
     */
    @Query("SELECT COALESCE(SUM(b.storyPoints), 0) FROM BacklogItem b WHERE b.project = :project AND b.backlogStatus = :status")
    Integer sumStoryPointsByProjectAndStatus(@Param("project") Project project, @Param("status") BacklogStatus status);

    /**
     * Get total story points for a sprint.
     */
    @Query("SELECT COALESCE(SUM(b.storyPoints), 0) FROM BacklogItem b WHERE b.sprint = :sprint")
    Integer sumStoryPointsBySprint(@Param("sprint") Sprint sprint);

    /**
     * Find completed backlog items for a sprint.
     */
    @Query("SELECT b FROM BacklogItem b WHERE b.sprint = :sprint AND b.backlogStatus = 'COMPLETED' ORDER BY b.completedAt DESC")
    List<BacklogItem> findCompletedBySprintOrderByCompletedAtDesc(@Param("sprint") Sprint sprint);

    /**
     * Find backlog items that are blocked (have blocking dependencies).
     */
    @Query("SELECT b FROM BacklogItem b WHERE b.project = :project AND b.backlogStatus IN ('SPRINT_BACKLOG', 'IN_PROGRESS') ORDER BY b.priorityLevel DESC")
    List<BacklogItem> findActiveItemsByProject(@Param("project") Project project);

    /**
     * Get backlog items created within a date range.
     */
    @Query("SELECT b FROM BacklogItem b WHERE b.project = :project AND b.createdAt >= :startDate AND b.createdAt <= :endDate ORDER BY b.createdAt DESC")
    List<BacklogItem> findByProjectAndDateRange(@Param("project") Project project, 
                                               @Param("startDate") java.time.LocalDateTime startDate,
                                               @Param("endDate") java.time.LocalDateTime endDate);

    /**
     * Find backlog items assigned to a specific user.
     */
    List<BacklogItem> findByAssignedToOrderByPriorityLevelDescPriorityRankAsc(com.autotrack.model.User assignedTo);

    /**
     * Get backlog velocity data for analytics.
     */
    @Query("SELECT b.sprint.id, COUNT(b), SUM(b.storyPoints) FROM BacklogItem b " +
           "WHERE b.project = :project AND b.backlogStatus = 'COMPLETED' " +
           "GROUP BY b.sprint.id ORDER BY b.sprint.startDate DESC")
    List<Object[]> getVelocityDataByProject(@Param("project") Project project);

    /**
     * Find backlog items by project and priority level (simplified version).
     */
    List<BacklogItem> findByProjectAndPriorityLevel(Project project, PriorityLevel priorityLevel);

    /**
     * Find backlog items by project and status (simplified version).
     */
    List<BacklogItem> findByProjectAndBacklogStatus(Project project, BacklogStatus status);

    /**
     * Find backlog items by project ordered by priority rank (for top priority items).
     */
    List<BacklogItem> findByProjectOrderByPriorityRankAsc(Project project);

    /**
     * Search backlog items by title or description.
     */
    List<BacklogItem> findByProjectAndTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
        Project project, String titleQuery, String descriptionQuery);
}
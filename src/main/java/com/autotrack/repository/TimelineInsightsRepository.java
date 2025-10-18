package com.autotrack.repository;

import com.autotrack.model.TimelineInsights;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TimelineInsights entity operations.
 */
@Repository
public interface TimelineInsightsRepository extends JpaRepository<TimelineInsights, Long> {

    /**
     * Find timeline insights by entity type and ID.
     */
    Optional<TimelineInsights> findByEntityTypeAndEntityId(TimelineInsights.EntityType entityType, Long entityId);

    /**
     * Find all timeline insights for a specific entity type.
     */
    List<TimelineInsights> findByEntityTypeOrderByUpdatedAtDesc(TimelineInsights.EntityType entityType);

    /**
     * Find overdue timeline insights.
     */
    @Query("SELECT t FROM TimelineInsights t WHERE t.estimatedEndDate < :currentDate AND t.actualEndDate IS NULL ORDER BY t.estimatedEndDate ASC")
    List<TimelineInsights> findOverdueItems(@Param("currentDate") LocalDate currentDate);

    /**
     * Find timeline insights by risk level.
     */
    List<TimelineInsights> findByTimelineRiskLevelOrderByEstimatedEndDateAsc(com.autotrack.model.PriorityLevel riskLevel);

    /**
     * Find timeline insights ending within a date range.
     */
    @Query("SELECT t FROM TimelineInsights t WHERE t.estimatedEndDate BETWEEN :startDate AND :endDate ORDER BY t.estimatedEndDate ASC")
    List<TimelineInsights> findByEstimatedEndDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find completed timeline insights within a date range.
     */
    @Query("SELECT t FROM TimelineInsights t WHERE t.actualEndDate BETWEEN :startDate AND :endDate ORDER BY t.actualEndDate DESC")
    List<TimelineInsights> findCompletedBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Get timeline insights for items with high risk.
     */
    @Query("SELECT t FROM TimelineInsights t WHERE t.timelineRiskLevel IN ('HIGH', 'CRITICAL') ORDER BY t.timelineRiskLevel DESC, t.estimatedEndDate ASC")
    List<TimelineInsights> findHighRiskItems();

    /**
     * Get progress statistics for entity type.
     */
    @Query("SELECT AVG(t.progressPercentage), COUNT(t) FROM TimelineInsights t WHERE t.entityType = :entityType AND t.actualEndDate IS NULL")
    Object[] getProgressStatsByEntityType(@Param("entityType") TimelineInsights.EntityType entityType);

    /**
     * Find timeline insights that are behind schedule.
     */
    @Query("SELECT t FROM TimelineInsights t WHERE t.predictedEndDate > t.estimatedEndDate AND t.actualEndDate IS NULL ORDER BY t.predictedEndDate DESC")
    List<TimelineInsights> findBehindScheduleItems();

    /**
     * Get estimation accuracy data (completed items only).
     */
    @Query("SELECT t.estimatedDurationDays, t.actualDurationDays FROM TimelineInsights t WHERE t.actualEndDate IS NOT NULL AND t.estimatedDurationDays > 0")
    List<Object[]> getEstimationAccuracyData();
}
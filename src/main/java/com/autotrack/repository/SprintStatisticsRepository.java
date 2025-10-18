package com.autotrack.repository;

import com.autotrack.model.SprintStatistics;
import com.autotrack.model.Sprint;
import com.autotrack.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for SprintStatistics entity operations.
 */
@Repository
public interface SprintStatisticsRepository extends JpaRepository<SprintStatistics, Long> {

    /**
     * Find statistics by sprint.
     */
    Optional<SprintStatistics> findBySprint(Sprint sprint);

    /**
     * Find statistics for all sprints in a project.
     */
    @Query("SELECT ss FROM SprintStatistics ss JOIN ss.sprint s WHERE s.project = :project ORDER BY s.startDate DESC")
    List<SprintStatistics> findByProject(@Param("project") Project project);

    /**
     * Find statistics for active sprints in a project.
     */
    @Query("SELECT ss FROM SprintStatistics ss JOIN ss.sprint s WHERE s.project = :project AND s.status = 'ACTIVE' ORDER BY s.startDate DESC")
    List<SprintStatistics> findByProjectActiveSprintsOnly(@Param("project") Project project);

    /**
     * Get velocity trend data for a project (last N sprints).
     */
    @Query("SELECT ss FROM SprintStatistics ss JOIN ss.sprint s WHERE s.project = :project AND s.status = 'COMPLETED' ORDER BY s.endDate DESC LIMIT :limit")
    List<SprintStatistics> findVelocityTrendByProject(@Param("project") Project project, @Param("limit") int limit);

    /**
     * Get average velocity for completed sprints in a project.
     */
    @Query("SELECT AVG(ss.completedStoryPoints) FROM SprintStatistics ss JOIN ss.sprint s WHERE s.project = :project AND s.status = 'COMPLETED'")
    Double getAverageVelocityByProject(@Param("project") Project project);

    /**
     * Get sprint health statistics for a project.
     */
    @Query("SELECT ss.sprintHealth, COUNT(ss) FROM SprintStatistics ss JOIN ss.sprint s WHERE s.project = :project GROUP BY ss.sprintHealth")
    List<Object[]> getSprintHealthStatsByProject(@Param("project") Project project);

    /**
     * Find sprints at risk or off track.
     */
    @Query("SELECT ss FROM SprintStatistics ss JOIN ss.sprint s WHERE s.project = :project AND ss.sprintHealth IN ('AT_RISK', 'OFF_TRACK') ORDER BY s.startDate DESC")
    List<SprintStatistics> findRiskySprintsByProject(@Param("project") Project project);

    /**
     * Get total story points committed vs completed for a project.
     */
    @Query("SELECT SUM(ss.plannedStoryPoints), SUM(ss.completedStoryPoints) FROM SprintStatistics ss JOIN ss.sprint s WHERE s.project = :project AND s.status = 'COMPLETED'")
    Object[] getTotalStoryPointsByProject(@Param("project") Project project);

    /**
     * Get commit statistics for completed sprints.
     */
    @Query("SELECT SUM(ss.totalCommits), SUM(ss.totalLinesAdded), SUM(ss.totalLinesModified), SUM(ss.totalLinesDeleted) " +
           "FROM SprintStatistics ss JOIN ss.sprint s WHERE s.project = :project AND s.status = 'COMPLETED'")
    Object[] getCodeStatsByProject(@Param("project") Project project);
}
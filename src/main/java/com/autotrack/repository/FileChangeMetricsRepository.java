package com.autotrack.repository;

import com.autotrack.model.FileChangeMetrics;
import com.autotrack.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FileChangeMetricsRepository extends JpaRepository<FileChangeMetrics, Long> {

    List<FileChangeMetrics> findByProjectOrderByCreatedAtDesc(Project project);

    List<FileChangeMetrics> findByCommitId(Long commitId);

    @Query("SELECT fcm.fileExtension, COUNT(fcm), SUM(fcm.linesAdded + fcm.linesModified + fcm.linesDeleted) " +
           "FROM FileChangeMetrics fcm WHERE fcm.project = :project " +
           "GROUP BY fcm.fileExtension ORDER BY COUNT(fcm) DESC")
    List<Object[]> getFileTypeStatsByProject(@Param("project") Project project);

    @Query("SELECT fcm FROM FileChangeMetrics fcm WHERE fcm.project = :project " +
           "AND fcm.createdAt >= :startDate AND fcm.createdAt <= :endDate " +
           "ORDER BY fcm.createdAt DESC")
    List<FileChangeMetrics> findByProjectAndDateRange(@Param("project") Project project,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);

    @Query("SELECT fcm.filePath, COUNT(fcm) as changeCount " +
           "FROM FileChangeMetrics fcm WHERE fcm.project = :project " +
           "GROUP BY fcm.filePath ORDER BY changeCount DESC")
    List<Object[]> getMostChangedFilesByProject(@Param("project") Project project);

    @Query("SELECT DATE(fcm.createdAt) as changeDate, COUNT(fcm) as dailyChanges " +
           "FROM FileChangeMetrics fcm WHERE fcm.project = :project " +
           "AND fcm.createdAt >= :startDate " +
           "GROUP BY DATE(fcm.createdAt) ORDER BY changeDate")
    List<Object[]> getDailyFileChangesByProject(@Param("project") Project project, 
                                              @Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(DISTINCT fcm.filePath) FROM FileChangeMetrics fcm WHERE fcm.project = :project")
    Long getTotalUniqueFilesByProject(@Param("project") Project project);

    @Query("SELECT fcm.fileExtension, " +
           "SUM(fcm.linesAdded) as totalAdded, " +
           "SUM(fcm.linesModified) as totalModified, " +
           "SUM(fcm.linesDeleted) as totalDeleted " +
           "FROM FileChangeMetrics fcm WHERE fcm.project = :project " +
           "GROUP BY fcm.fileExtension")
    List<Object[]> getLineChangesByFileType(@Param("project") Project project);
}
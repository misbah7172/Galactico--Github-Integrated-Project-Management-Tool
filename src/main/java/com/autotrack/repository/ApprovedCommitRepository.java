package com.autotrack.repository;

import com.autotrack.model.ApprovedCommit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for ApprovedCommit entity operations.
 */
@Repository
public interface ApprovedCommitRepository extends JpaRepository<ApprovedCommit, Long> {

    /**
     * Find all approved commits by username.
     */
    List<ApprovedCommit> findByUsername(String username);

    /**
     * Find all approved commits by project ID.
     */
    @Query("SELECT ac FROM ApprovedCommit ac WHERE ac.project.id = :projectId")
    List<ApprovedCommit> findByProjectId(@Param("projectId") Long projectId);

    /**
     * Find all approved commits by approved by user.
     */
    @Query("SELECT ac FROM ApprovedCommit ac WHERE ac.approvedBy.nickname = :approvedBy")
    List<ApprovedCommit> findByApprovedBy(@Param("approvedBy") String approvedBy);

    /**
     * Find all approved commits within date range.
     */
    List<ApprovedCommit> findByApprovedTimeBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find all approved commits by task ID.
     */
    List<ApprovedCommit> findByTaskId(String taskId);

    /**
     * Find recent approved commits (last 30 days) ordered by approval time.
     */
    @Query("SELECT ac FROM ApprovedCommit ac WHERE ac.approvedTime >= :since ORDER BY ac.approvedTime DESC")
    List<ApprovedCommit> findRecentApproved(@Param("since") LocalDateTime since);

    /**
     * Count approved commits by user in date range.
     */
    @Query("SELECT COUNT(ac) FROM ApprovedCommit ac WHERE ac.username = :username AND ac.approvedTime BETWEEN :start AND :end")
    Long countByUsernameAndDateRange(@Param("username") String username, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}

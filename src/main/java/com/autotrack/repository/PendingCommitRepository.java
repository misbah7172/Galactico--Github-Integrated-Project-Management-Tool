package com.autotrack.repository;

import com.autotrack.model.PendingCommit;
import com.autotrack.model.CommitStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for PendingCommit entity operations.
 */
@Repository
public interface PendingCommitRepository extends JpaRepository<PendingCommit, Long> {

    /**
     * Find all pending commits by status.
     */
    List<PendingCommit> findByStatus(CommitStatus status);

    /**
     * Find all pending commits by username.
     */
    List<PendingCommit> findByUsername(String username);

    /**
     * Find all pending commits by project ID.
     */
    @Query("SELECT pc FROM PendingCommit pc WHERE pc.project.id = :projectId")
    List<PendingCommit> findByProjectId(@Param("projectId") Long projectId);

    /**
     * Find all pending commits by task ID.
     */
    List<PendingCommit> findByTaskId(String taskId);

    /**
     * Find all pending commits by status and project ID.
     */
    @Query("SELECT pc FROM PendingCommit pc WHERE pc.status = :status AND pc.project.id = :projectId")
    List<PendingCommit> findByStatusAndProjectId(@Param("status") CommitStatus status, @Param("projectId") Long projectId);

    /**
     * Find all pending commits for review by team leads.
     */
    @Query("SELECT pc FROM PendingCommit pc WHERE pc.status = 'PENDING_REVIEW' ORDER BY pc.commitTime ASC")
    List<PendingCommit> findPendingReviews();

    /**
     * Find all commits by username and status.
     */
    List<PendingCommit> findByUsernameAndStatus(String username, CommitStatus status);
}

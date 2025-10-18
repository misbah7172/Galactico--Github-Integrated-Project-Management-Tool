package com.autotrack.repository;

import com.autotrack.model.CommitReview;
import com.autotrack.model.Project;
import com.autotrack.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommitReviewRepository extends JpaRepository<CommitReview, Long> {

    List<CommitReview> findByProjectAndStatus(Project project, CommitReview.ReviewStatus status);

    List<CommitReview> findByReviewerAndStatus(User reviewer, CommitReview.ReviewStatus status);

    Page<CommitReview> findByProjectOrderByCreatedAtDesc(Project project, Pageable pageable);

    @Query("SELECT cr FROM CommitReview cr WHERE cr.project = :project AND cr.status = :status ORDER BY cr.createdAt ASC")
    List<CommitReview> findPendingReviewsByProject(@Param("project") Project project, @Param("status") CommitReview.ReviewStatus status);

    @Query("SELECT cr FROM CommitReview cr WHERE cr.commit.id = :commitId")
    Optional<CommitReview> findByCommitId(@Param("commitId") Long commitId);

    @Query("SELECT COUNT(cr) FROM CommitReview cr WHERE cr.project = :project AND cr.status = :status")
    Long countByProjectAndStatus(@Param("project") Project project, @Param("status") CommitReview.ReviewStatus status);

    @Query("SELECT COUNT(cr) FROM CommitReview cr WHERE cr.reviewer = :reviewer AND cr.reviewedAt >= :startDate")
    Long countReviewsByReviewerSince(@Param("reviewer") User reviewer, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT cr FROM CommitReview cr WHERE cr.project = :project AND cr.reviewedAt >= :startDate AND cr.reviewedAt <= :endDate")
    List<CommitReview> findByProjectAndDateRange(@Param("project") Project project, 
                                                @Param("startDate") LocalDateTime startDate, 
                                                @Param("endDate") LocalDateTime endDate);

    @Query("SELECT cr FROM CommitReview cr WHERE cr.commit.authorEmail = :authorEmail AND cr.project = :project ORDER BY cr.createdAt DESC")
    List<CommitReview> findByAuthorEmailAndProject(@Param("authorEmail") String authorEmail, @Param("project") Project project);

    @Query("SELECT DISTINCT cr.commit.authorEmail FROM CommitReview cr WHERE cr.project = :project")
    List<String> findDistinctAuthorEmailsByProject(@Param("project") Project project);

    @Query("SELECT cr FROM CommitReview cr WHERE cr.commit = :commit")
    Optional<CommitReview> findByCommit(@Param("commit") com.autotrack.model.Commit commit);

    @Query("SELECT cr FROM CommitReview cr WHERE cr.project.id = :projectId AND cr.status = :status")
    List<CommitReview> findByProjectIdAndStatus(@Param("projectId") Long projectId, @Param("status") CommitReview.ReviewStatus status);

    @Query("SELECT cr FROM CommitReview cr WHERE cr.commit.authorEmail = :authorEmail AND cr.project.id = :projectId ORDER BY cr.createdAt DESC")
    List<CommitReview> findByCommitAuthorEmailAndProjectId(@Param("authorEmail") String authorEmail, @Param("projectId") Long projectId);

    @Query("SELECT cr FROM CommitReview cr WHERE cr.project.id = :projectId AND cr.reviewedAt >= :startDate AND cr.reviewedAt <= :endDate ORDER BY cr.reviewedAt DESC")
    List<CommitReview> findByProjectIdAndReviewDateBetween(@Param("projectId") Long projectId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT cr FROM CommitReview cr WHERE cr.project.id = :projectId ORDER BY cr.reviewedAt DESC")
    List<CommitReview> findByProjectIdOrderByReviewDateDesc(@Param("projectId") Long projectId);

    @Query("SELECT COUNT(cr) FROM CommitReview cr WHERE cr.reviewer = :reviewer AND cr.status = :status")
    Long countByReviewerAndStatus(@Param("reviewer") User reviewer, @Param("status") CommitReview.ReviewStatus status);
}
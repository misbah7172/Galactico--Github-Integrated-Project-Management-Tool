package com.autotrack.service;

import com.autotrack.dto.CommitWebhookDTO;
import com.autotrack.dto.CommitReviewDTO;
import com.autotrack.model.*;
import com.autotrack.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing commit review workflow.
 */
@Service
@Transactional
public class CommitReviewService {

    @Autowired
    private PendingCommitRepository pendingCommitRepository;

    @Autowired
    private ApprovedCommitRepository approvedCommitRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommitReviewRepository commitReviewRepository;

    @Autowired
    private CommitRepository commitRepository;

    @Autowired
    private ContributorStatsRepository contributorStatsRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    /**
     * Process incoming commit from VS Code extension webhook.
     */
    public PendingCommit processIncomingCommit(CommitWebhookDTO commitDTO) {
        // Find project by ID
        Optional<Project> projectOpt = projectRepository.findById(Long.parseLong(commitDTO.getProjectId()));
        if (!projectOpt.isPresent()) {
            throw new RuntimeException("Project not found: " + commitDTO.getProjectId());
        }

        // Try to find user by username (optional)
        Optional<User> userOpt = userRepository.findByUsername(commitDTO.getUsername());

        // Create pending commit
        PendingCommit pendingCommit = new PendingCommit();
        pendingCommit.setUsername(commitDTO.getUsername());
        pendingCommit.setCommitMessage(commitDTO.getCommitMessage());
        pendingCommit.setBranch(commitDTO.getBranch());
        pendingCommit.setTaskId(commitDTO.getTaskId());
        pendingCommit.setCommitTime(commitDTO.getCommitTime());
        pendingCommit.setCommitUrl(commitDTO.getCommitUrl());
        pendingCommit.setCommitSha(commitDTO.getCommitSha());
        pendingCommit.setProject(projectOpt.get());
        pendingCommit.setUser(userOpt.orElse(null)); // User can be null if not found
        pendingCommit.setStatus(CommitStatus.PENDING_REVIEW);
        pendingCommit.setCreatedAt(LocalDateTime.now());

        return pendingCommitRepository.save(pendingCommit);
    }

    /**
     * Review a pending commit (approve/reject).
     */
    public Object reviewCommit(CommitReviewDTO reviewDTO) {
        Optional<PendingCommit> pendingOpt = pendingCommitRepository.findById(reviewDTO.getCommitId());
        if (!pendingOpt.isPresent()) {
            throw new RuntimeException("Pending commit not found: " + reviewDTO.getCommitId());
        }

        PendingCommit pendingCommit = pendingOpt.get();
        
        // Find reviewer user
        Optional<User> reviewerOpt = userRepository.findByUsername(reviewDTO.getReviewedBy());
        if (!reviewerOpt.isPresent()) {
            throw new RuntimeException("Reviewer not found: " + reviewDTO.getReviewedBy());
        }

        pendingCommit.setStatus(reviewDTO.getStatus());
        pendingCommit.setReviewedBy(reviewerOpt.get());
        pendingCommit.setReviewedAt(LocalDateTime.now());

        if (reviewDTO.getStatus() == CommitStatus.REJECTED) {
            pendingCommit.setRejectionReason(reviewDTO.getRejectionReason());
            return pendingCommitRepository.save(pendingCommit);
        } else if (reviewDTO.getStatus() == CommitStatus.APPROVED) {
            // Move to approved commits
            ApprovedCommit approvedCommit = new ApprovedCommit(pendingCommit);
            approvedCommit.setApprovedBy(reviewerOpt.get());
            approvedCommit.setApprovedTime(LocalDateTime.now());
            
            ApprovedCommit saved = approvedCommitRepository.save(approvedCommit);
            
            // Update pending commit status
            pendingCommitRepository.save(pendingCommit);
            
            return saved;
        }

        return pendingCommitRepository.save(pendingCommit);
    }

    /**
     * Get all pending commits for review.
     */
    public List<PendingCommit> getPendingReviews() {
        return pendingCommitRepository.findPendingReviews();
    }

    /**
     * Get pending commits by project.
     */
    public List<PendingCommit> getPendingCommitsByProject(Long projectId) {
        return pendingCommitRepository.findByStatusAndProjectId(CommitStatus.PENDING_REVIEW, projectId);
    }

    /**
     * Get user's pending commits.
     */
    public List<PendingCommit> getUserPendingCommits(String username) {
        return pendingCommitRepository.findByUsernameAndStatus(username, CommitStatus.PENDING_REVIEW);
    }

    /**
     * Get user's rejected commits.
     */
    public List<PendingCommit> getUserRejectedCommits(String username) {
        return pendingCommitRepository.findByUsernameAndStatus(username, CommitStatus.REJECTED);
    }

    /**
     * Get approved commits by project.
     */
    public List<ApprovedCommit> getApprovedCommitsByProject(Long projectId) {
        return approvedCommitRepository.findByProjectId(projectId);
    }

    /**
     * Get user's approved commits.
     */
    public List<ApprovedCommit> getUserApprovedCommits(String username) {
        return approvedCommitRepository.findByUsername(username);
    }

    /**
     * Get recent approved commits (last 30 days).
     */
    public List<ApprovedCommit> getRecentApprovedCommits() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return approvedCommitRepository.findRecentApproved(thirtyDaysAgo);
    }

    /**
     * Mark commit as merged.
     */
    public PendingCommit markCommitAsMerged(Long commitId) {
        Optional<PendingCommit> pendingOpt = pendingCommitRepository.findById(commitId);
        if (!pendingOpt.isPresent()) {
            throw new RuntimeException("Pending commit not found: " + commitId);
        }

        PendingCommit pendingCommit = pendingOpt.get();
        pendingCommit.setStatus(CommitStatus.MERGED);
        pendingCommit.setMergedAt(LocalDateTime.now());

        return pendingCommitRepository.save(pendingCommit);
    }

    /**
     * Get commit statistics for a user.
     */
    public CommitStats getUserCommitStats(String username) {
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = LocalDateTime.now();

        long approvedCount = approvedCommitRepository.countByUsernameAndDateRange(username, startOfMonth, endOfMonth);
        long pendingCount = pendingCommitRepository.findByUsernameAndStatus(username, CommitStatus.PENDING_REVIEW).size();
        long rejectedCount = pendingCommitRepository.findByUsernameAndStatus(username, CommitStatus.REJECTED).size();

        return new CommitStats(approvedCount, pendingCount, rejectedCount);
    }

    /**
     * Inner class for commit statistics.
     */
    public static class CommitStats {
        private final long approvedCount;
        private final long pendingCount;
        private final long rejectedCount;

        public CommitStats(long approvedCount, long pendingCount, long rejectedCount) {
            this.approvedCount = approvedCount;
            this.pendingCount = pendingCount;
            this.rejectedCount = rejectedCount;
        }

        public long getApprovedCount() { return approvedCount; }
        public long getPendingCount() { return pendingCount; }
        public long getRejectedCount() { return rejectedCount; }
        public long getTotalCount() { return approvedCount + pendingCount + rejectedCount; }
    }

    // New methods for CommitReview entity integration

    @Transactional
    public CommitReview submitCommitForReview(Long commitId, Long reviewerId, String comments) {
        Commit commit = commitRepository.findById(commitId)
                .orElseThrow(() -> new RuntimeException("Commit not found with id: " + commitId));
        
        User reviewer = userService.getUserById(reviewerId);
        if (reviewer == null) {
            throw new RuntimeException("Reviewer not found with id: " + reviewerId);
        }

        // Check if commit is already under review
        Optional<CommitReview> existingReview = commitReviewRepository.findByCommit(commit);
        if (existingReview.isPresent()) {
            throw new RuntimeException("Commit is already under review");
        }

        // Create commit review
        CommitReview commitReview = new CommitReview();
        commitReview.setCommit(commit);
        commitReview.setReviewer(reviewer);
        commitReview.setProject(commit.getProject());
        commitReview.setStatus(CommitReview.ReviewStatus.PENDING);
        commitReview.setComments(comments);

        commitReview = commitReviewRepository.save(commitReview);

        // Update contributor stats - increment pending commits
        updateContributorStatsForReview(commit, CommitReview.ReviewStatus.PENDING, null);

        // Send notification to reviewer
        createReviewNotification(commitReview, "New commit submitted for review");

        return commitReview;
    }

    @Transactional
    public CommitReview approveCommitReview(Long reviewId, String approvalComments) {
        CommitReview commitReview = commitReviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found with id: " + reviewId));

        if (commitReview.getStatus() != CommitReview.ReviewStatus.PENDING) {
            throw new RuntimeException("Review is not in pending status");
        }

        // Update review status
        CommitReview.ReviewStatus previousStatus = commitReview.getStatus();
        commitReview.setStatus(CommitReview.ReviewStatus.APPROVED);
        commitReview.setComments(appendComments(commitReview.getComments(), approvalComments));

        commitReview = commitReviewRepository.save(commitReview);

        // Update contributor stats - move from pending to approved
        updateContributorStatsForReview(commitReview.getCommit(), CommitReview.ReviewStatus.APPROVED, previousStatus);

        // Send notification to commit author
        createReviewNotification(commitReview, "Your commit has been approved");

        return commitReview;
    }

    @Transactional
    public CommitReview rejectCommitReview(Long reviewId, String rejectionComments) {
        CommitReview commitReview = commitReviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found with id: " + reviewId));

        if (commitReview.getStatus() != CommitReview.ReviewStatus.PENDING) {
            throw new RuntimeException("Review is not in pending status");
        }

        // Update review status
        CommitReview.ReviewStatus previousStatus = commitReview.getStatus();
        commitReview.setStatus(CommitReview.ReviewStatus.REJECTED);
        commitReview.setComments(appendComments(commitReview.getComments(), rejectionComments));

        commitReview = commitReviewRepository.save(commitReview);

        // Update contributor stats - move from pending to rejected
        updateContributorStatsForReview(commitReview.getCommit(), CommitReview.ReviewStatus.REJECTED, previousStatus);

        // Send notification to commit author
        createReviewNotification(commitReview, "Your commit has been rejected");

        return commitReview;
    }

    public List<CommitReview> getCommitReviewsForReviewer(Long reviewerId) {
        User reviewer = userService.getUserById(reviewerId);
        if (reviewer == null) {
            throw new RuntimeException("Reviewer not found with id: " + reviewerId);
        }

        return commitReviewRepository.findByReviewerAndStatus(reviewer, CommitReview.ReviewStatus.PENDING);
    }

    public List<CommitReview> getPendingCommitReviewsForProject(Long projectId) {
        return commitReviewRepository.findByProjectIdAndStatus(projectId, CommitReview.ReviewStatus.PENDING);
    }

    public List<CommitReview> getCommitReviewsByAuthor(String authorEmail, Long projectId) {
        return commitReviewRepository.findByCommitAuthorEmailAndProjectId(authorEmail, projectId);
    }

    public List<CommitReview> getCommitReviewHistoryForProject(Long projectId, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null) {
            return commitReviewRepository.findByProjectIdAndReviewDateBetween(projectId, startDate, endDate);
        } else {
            return commitReviewRepository.findByProjectIdOrderByReviewDateDesc(projectId);
        }
    }

    private void updateContributorStatsForReview(Commit commit, CommitReview.ReviewStatus newStatus, CommitReview.ReviewStatus previousStatus) {
        try {
            Optional<ContributorStats> statsOpt = contributorStatsRepository.findByProjectAndContributorEmail(
                    commit.getProject(), commit.getAuthorEmail());

            if (statsOpt.isPresent()) {
                ContributorStats stats = statsOpt.get();

                // Update counts based on status changes
                if (previousStatus == CommitReview.ReviewStatus.PENDING) {
                    stats.setPendingCommits(Math.max(0, stats.getPendingCommits() - 1));
                } else if (previousStatus == CommitReview.ReviewStatus.APPROVED) {
                    stats.setApprovedCommits(Math.max(0, stats.getApprovedCommits() - 1));
                } else if (previousStatus == CommitReview.ReviewStatus.REJECTED) {
                    stats.setRejectedCommits(Math.max(0, stats.getRejectedCommits() - 1));
                }

                if (newStatus == CommitReview.ReviewStatus.PENDING) {
                    stats.setPendingCommits(stats.getPendingCommits() + 1);
                } else if (newStatus == CommitReview.ReviewStatus.APPROVED) {
                    stats.setApprovedCommits(stats.getApprovedCommits() + 1);
                } else if (newStatus == CommitReview.ReviewStatus.REJECTED) {
                    stats.setRejectedCommits(stats.getRejectedCommits() + 1);
                }

                contributorStatsRepository.save(stats);
            }
        } catch (Exception e) {
            System.err.println("Error updating contributor stats for review: " + e.getMessage());
        }
    }

    private void createReviewNotification(CommitReview commitReview, String message) {
        try {
            String notificationMessage = String.format("%s - Commit: %s", message, 
                commitReview.getCommit().getMessage().substring(0, Math.min(50, commitReview.getCommit().getMessage().length())));
            
            // Send appropriate notification based on review status
            if (commitReview.getStatus() == CommitReview.ReviewStatus.APPROVED) {
                notificationService.notifyCommitApproved(commitReview);
            } else if (commitReview.getStatus() == CommitReview.ReviewStatus.REJECTED) {
                notificationService.notifyCommitRejected(commitReview);
            } else {
                // For pending or initial review submission
                notificationService.createCommitReviewNotification(commitReview, notificationMessage);
            }
            
        } catch (Exception e) {
            System.err.println("Error creating review notification: " + e.getMessage());
        }
    }

    private String appendComments(String existingComments, String newComments) {
        if (existingComments == null || existingComments.trim().isEmpty()) {
            return newComments;
        }
        if (newComments == null || newComments.trim().isEmpty()) {
            return existingComments;
        }
        return existingComments + "\n\n---\n\n" + newComments;
    }

    public Optional<CommitReview> getCommitReviewByCommit(Commit commit) {
        return commitReviewRepository.findByCommit(commit);
    }

    public long getPendingCommitReviewCount(Long reviewerId) {
        User reviewer = userService.getUserById(reviewerId);
        if (reviewer == null) {
            return 0;
        }
        return commitReviewRepository.countByReviewerAndStatus(reviewer, CommitReview.ReviewStatus.PENDING);
    }
}

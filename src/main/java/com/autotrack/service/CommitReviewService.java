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
}

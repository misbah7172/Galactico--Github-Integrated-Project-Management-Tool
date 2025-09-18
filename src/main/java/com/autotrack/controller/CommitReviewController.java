package com.autotrack.controller;

import com.autotrack.dto.CommitWebhookDTO;
import com.autotrack.dto.CommitReviewDTO;
import com.autotrack.model.PendingCommit;
import com.autotrack.model.ApprovedCommit;
import com.autotrack.service.CommitReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for commit review workflow API endpoints.
 */
@RestController
@RequestMapping("/api/commits")
public class CommitReviewController {

    @Autowired
    private CommitReviewService commitReviewService;

    /**
     * Webhook endpoint for receiving commits from VS Code extension.
     */
    @PostMapping("/webhook")
    public ResponseEntity<PendingCommit> receiveCommit(@RequestBody CommitWebhookDTO commitDTO) {
        try {
            PendingCommit savedCommit = commitReviewService.processIncomingCommit(commitDTO);
            return ResponseEntity.ok(savedCommit);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Review a pending commit (approve/reject).
     */
    @PostMapping("/review")
    @PreAuthorize("hasRole('TEAM_LEAD') or hasRole('ADMIN')")
    public ResponseEntity<Object> reviewCommit(@RequestBody CommitReviewDTO reviewDTO) {
        try {
            Object result = commitReviewService.reviewCommit(reviewDTO);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get all pending commits for review.
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('TEAM_LEAD') or hasRole('ADMIN')")
    public ResponseEntity<List<PendingCommit>> getPendingReviews() {
        List<PendingCommit> pendingCommits = commitReviewService.getPendingReviews();
        return ResponseEntity.ok(pendingCommits);
    }

    /**
     * Get pending commits by project.
     */
    @GetMapping("/pending/project/{projectId}")
    @PreAuthorize("hasRole('TEAM_LEAD') or hasRole('ADMIN')")
    public ResponseEntity<List<PendingCommit>> getPendingCommitsByProject(@PathVariable Long projectId) {
        List<PendingCommit> pendingCommits = commitReviewService.getPendingCommitsByProject(projectId);
        return ResponseEntity.ok(pendingCommits);
    }

    /**
     * Get user's pending commits.
     */
    @GetMapping("/user/{username}/pending")
    public ResponseEntity<List<PendingCommit>> getUserPendingCommits(@PathVariable String username) {
        List<PendingCommit> pendingCommits = commitReviewService.getUserPendingCommits(username);
        return ResponseEntity.ok(pendingCommits);
    }

    /**
     * Get user's rejected commits.
     */
    @GetMapping("/user/{username}/rejected")
    public ResponseEntity<List<PendingCommit>> getUserRejectedCommits(@PathVariable String username) {
        List<PendingCommit> rejectedCommits = commitReviewService.getUserRejectedCommits(username);
        return ResponseEntity.ok(rejectedCommits);
    }

    /**
     * Get approved commits by project.
     */
    @GetMapping("/approved/project/{projectId}")
    public ResponseEntity<List<ApprovedCommit>> getApprovedCommitsByProject(@PathVariable Long projectId) {
        List<ApprovedCommit> approvedCommits = commitReviewService.getApprovedCommitsByProject(projectId);
        return ResponseEntity.ok(approvedCommits);
    }

    /**
     * Get user's approved commits.
     */
    @GetMapping("/user/{username}/approved")
    public ResponseEntity<List<ApprovedCommit>> getUserApprovedCommits(@PathVariable String username) {
        List<ApprovedCommit> approvedCommits = commitReviewService.getUserApprovedCommits(username);
        return ResponseEntity.ok(approvedCommits);
    }

    /**
     * Get recent approved commits (last 30 days).
     */
    @GetMapping("/approved/recent")
    public ResponseEntity<List<ApprovedCommit>> getRecentApprovedCommits() {
        List<ApprovedCommit> recentCommits = commitReviewService.getRecentApprovedCommits();
        return ResponseEntity.ok(recentCommits);
    }

    /**
     * Mark a commit as merged.
     */
    @PostMapping("/{commitId}/merge")
    @PreAuthorize("hasRole('TEAM_LEAD') or hasRole('ADMIN')")
    public ResponseEntity<PendingCommit> markCommitAsMerged(@PathVariable Long commitId) {
        try {
            PendingCommit mergedCommit = commitReviewService.markCommitAsMerged(commitId);
            return ResponseEntity.ok(mergedCommit);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get commit statistics for a user.
     */
    @GetMapping("/user/{username}/stats")
    public ResponseEntity<CommitReviewService.CommitStats> getUserCommitStats(@PathVariable String username) {
        CommitReviewService.CommitStats stats = commitReviewService.getUserCommitStats(username);
        return ResponseEntity.ok(stats);
    }
}

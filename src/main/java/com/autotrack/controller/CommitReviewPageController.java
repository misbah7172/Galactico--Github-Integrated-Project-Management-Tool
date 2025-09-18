package com.autotrack.controller;

import com.autotrack.model.PendingCommit;
import com.autotrack.model.ApprovedCommit;
import com.autotrack.service.CommitReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Controller for commit review web pages.
 */
@Controller
public class CommitReviewPageController {

    @Autowired
    private CommitReviewService commitReviewService;

    /**
     * Display the commit review dashboard.
     */
    @GetMapping("/commit-review")
    public String commitReviewDashboard(Model model, Authentication authentication) {
        // Get pending commits for review
        List<PendingCommit> pendingCommits = commitReviewService.getPendingReviews();
        
        // Get recent approved commits
        List<ApprovedCommit> approvedCommits = commitReviewService.getRecentApprovedCommits();
        
        // Get user's commits if authenticated
        List<PendingCommit> userCommits = null;
        CommitReviewService.CommitStats userStats = null;
        
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            userCommits = commitReviewService.getUserPendingCommits(username);
            userStats = commitReviewService.getUserCommitStats(username);
            model.addAttribute("currentUser", username);
        }
        
        // Add model attributes
        model.addAttribute("pendingCommits", pendingCommits);
        model.addAttribute("approvedCommits", approvedCommits);
        model.addAttribute("userCommits", userCommits);
        model.addAttribute("userStats", userStats);
        model.addAttribute("pendingCount", pendingCommits.size());
        model.addAttribute("approvedCount", approvedCommits.size());
        
        // Check user role for permissions
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_TEAM_LEAD") || auth.getAuthority().equals("ROLE_ADMIN"))) {
            model.addAttribute("userRole", "TEAM_LEAD");
        } else {
            model.addAttribute("userRole", "DEVELOPER");
        }
        
        return "commit-review";
    }
}

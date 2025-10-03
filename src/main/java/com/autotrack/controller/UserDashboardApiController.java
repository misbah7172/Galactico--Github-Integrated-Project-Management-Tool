package com.autotrack.controller;

import com.autotrack.model.Task;
import com.autotrack.model.User;
import com.autotrack.service.TaskService;
import com.autotrack.service.UserService;
import com.autotrack.service.CommitReviewService;
import com.autotrack.service.ExtensionAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API controller for user-specific data needed by VS Code extension dashboard.
 * Now uses token-based authentication instead of username parameters.
 */
@RestController
@RequestMapping("/api/user")
public class UserDashboardApiController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserService userService;

    @Autowired
    private CommitReviewService commitReviewService;

    @Autowired
    private ExtensionAuthService authService;

    /**
     * Get all tasks assigned to the authenticated user (for VS Code dashboard).
     */
    @GetMapping("/tasks")
    public ResponseEntity<List<Map<String, Object>>> getUserTasks(@RequestHeader("Authorization") String authHeader) {
        try {
            // Authenticate user via token
            User user = authService.authenticateUser(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).build();
            }

            // Get assigned tasks
            List<Task> tasks = taskService.getTasksByAssignee(user);

            // Transform tasks to dashboard format
            List<Map<String, Object>> taskData = tasks.stream().map(task -> {
                Map<String, Object> taskMap = new HashMap<>();
                taskMap.put("id", task.getId());
                taskMap.put("title", task.getTitle());
                taskMap.put("description", task.getFeatureCode() != null ? task.getFeatureCode() : "");
                
                Map<String, Object> projectMap = new HashMap<>();
                projectMap.put("id", task.getProject().getId());
                projectMap.put("name", task.getProject().getName());
                taskMap.put("project", projectMap);
                
                taskMap.put("status", task.getStatus().toString());
                taskMap.put("createdAt", task.getCreatedAt().toString());
                taskMap.put("updatedAt", task.getUpdatedAt() != null ? task.getUpdatedAt().toString() : task.getCreatedAt().toString());
                taskMap.put("assignedDate", task.getCreatedAt().toString());
                taskMap.put("deadline", calculateDeadline(task));
                taskMap.put("featureCode", task.getFeatureCode());
                taskMap.put("githubIssueUrl", task.getGitHubIssueUrl());
                taskMap.put("commits", getTaskCommits(task));
                
                return taskMap;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(taskData);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get user contributions statistics for the authenticated user.
     */
    @GetMapping("/contributions")
    public ResponseEntity<Map<String, Object>> getUserContributions(@RequestHeader("Authorization") String authHeader) {
        try {
            // Authenticate user via token
            User user = authService.authenticateUser(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).build();
            }

            // Get task statistics
            List<Task> allTasks = taskService.getTasksByAssignee(user);
            long totalTasks = allTasks.size();
            long completedTasks = allTasks.stream().mapToLong(task -> 
                "DONE".equals(task.getStatus().toString()) ? 1 : 0).sum();
            long inProgressTasks = allTasks.stream().mapToLong(task -> 
                "IN_PROGRESS".equals(task.getStatus().toString()) ? 1 : 0).sum();
            
            // Calculate overdue tasks (simplified - tasks updated more than 7 days ago and not done)
            LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
            long overdueTasks = allTasks.stream().mapToLong(task -> {
                LocalDateTime lastUpdate = task.getUpdatedAt() != null ? task.getUpdatedAt() : task.getCreatedAt();
                return (lastUpdate.isBefore(oneWeekAgo) && !"DONE".equals(task.getStatus().toString())) ? 1 : 0;
            }).sum();

            // Get commit statistics
            CommitReviewService.CommitStats commitStats = commitReviewService.getUserCommitStats(user.getNickname());

            Map<String, Object> contributions = new HashMap<>();
            contributions.put("totalTasks", totalTasks);
            contributions.put("completedTasks", completedTasks);
            contributions.put("pendingTasks", inProgressTasks);
            contributions.put("overdueTasks", overdueTasks);
            contributions.put("totalCommits", commitStats.getTotalCount());
            contributions.put("approvedCommits", commitStats.getApprovedCount());
            contributions.put("pendingCommits", commitStats.getPendingCount());
            contributions.put("rejectedCommits", commitStats.getRejectedCount());

            return ResponseEntity.ok(contributions);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Calculate deadline for a task (default 7 days from creation).
     */
    private String calculateDeadline(Task task) {
        // If task has a specific deadline, use it. Otherwise, default to 7 days from creation
        LocalDateTime deadline = task.getCreatedAt().plusDays(7);
        return deadline.toString();
    }

    /**
     * Get commits related to a task (simplified - returns empty for now).
     */
    private List<Map<String, Object>> getTaskCommits(Task task) {
        // This would integrate with commit tracking system
        // For now, return empty list as placeholder
        return List.of();
    }
}

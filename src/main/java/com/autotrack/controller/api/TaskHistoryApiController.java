package com.autotrack.controller.api;

import com.autotrack.model.*;
import com.autotrack.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API Controller for task history and timeline management.
 * Provides Jira-like timeline functionality for tracking task management history.
 */
@RestController
@RequestMapping("/api/timeline")
public class TaskHistoryApiController {
    
    private final TaskHistoryService taskHistoryService;
    private final TaskService taskService;
    private final ProjectService projectService;
    private final UserService userService;
    
    @Autowired
    public TaskHistoryApiController(TaskHistoryService taskHistoryService,
                                   TaskService taskService,
                                   ProjectService projectService,
                                   UserService userService) {
        this.taskHistoryService = taskHistoryService;
        this.taskService = taskService;
        this.projectService = projectService;
        this.userService = userService;
    }
    
    /**
     * Get task history timeline for a specific task
     */
    @GetMapping("/task/{taskId}")
    public ResponseEntity<Page<TaskHistory>> getTaskTimeline(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal OAuth2User principal) {
        
        User user = userService.getCurrentUser(principal);
        Task task = taskService.getTaskById(taskId);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<TaskHistory> timeline = taskHistoryService.getTaskHistory(task, pageable);
        
        return ResponseEntity.ok(timeline);
    }
    
    /**
     * Get project-wide timeline showing all task management activities
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<Page<TaskHistory>> getProjectTimeline(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @AuthenticationPrincipal OAuth2User principal) {
        
        User user = userService.getCurrentUser(principal);
        Project project = projectService.getProjectById(projectId);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        
        Page<TaskHistory> timeline;
        if (actionType != null || userId != null || startDate != null || endDate != null) {
            // Filtered timeline
            timeline = taskHistoryService.getProjectTimelineFiltered(
                project, actionType, userId, startDate, endDate, pageable);
        } else {
            // All project activities
            timeline = taskHistoryService.getProjectTimeline(project, pageable);
        }
        
        return ResponseEntity.ok(timeline);
    }
    
    /**
     * Get sprint timeline showing all activities during a sprint
     */
    @GetMapping("/sprint/{sprintId}")
    public ResponseEntity<Page<TaskHistory>> getSprintTimeline(
            @PathVariable Long sprintId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal OAuth2User principal) {
        
        User user = userService.getCurrentUser(principal);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<TaskHistory> timeline = taskHistoryService.getSprintTimeline(sprintId, pageable);
        
        return ResponseEntity.ok(timeline);
    }
    
    /**
     * Get user activity timeline
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<TaskHistory>> getUserTimeline(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Long projectId,
            @AuthenticationPrincipal OAuth2User principal) {
        
        User currentUser = userService.getCurrentUser(principal);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<TaskHistory> timeline;
        
        if (projectId != null) {
            timeline = taskHistoryService.getUserTimelineForProject(userId, projectId, pageable);
        } else {
            timeline = taskHistoryService.getUserTimeline(userId, pageable);
        }
        
        return ResponseEntity.ok(timeline);
    }
    
    /**
     * Get timeline statistics for dashboard
     */
    @GetMapping("/stats/project/{projectId}")
    public ResponseEntity<Map<String, Object>> getTimelineStats(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "7") int days,
            @AuthenticationPrincipal OAuth2User principal) {
        
        User user = userService.getCurrentUser(principal);
        Project project = projectService.getProjectById(projectId);
        
        Map<String, Object> stats = new HashMap<>();
        
        // Get activity counts for the last N days
        Map<String, Long> activityCounts = taskHistoryService.getActivityCountsByDay(projectId, days);
        stats.put("activityByDay", activityCounts);
        
        // Get activity breakdown by type
        Map<String, Long> activityByType = taskHistoryService.getActivityCountsByType(projectId, days);
        stats.put("activityByType", activityByType);
        
        // Get most active users
        List<Map<String, Object>> activeUsers = taskHistoryService.getMostActiveUsers(projectId, days, 5);
        stats.put("mostActiveUsers", activeUsers);
        
        // Get recent critical activities
        List<TaskHistory> criticalActivities = taskHistoryService.getRecentCriticalActivities(projectId, 10);
        stats.put("criticalActivities", criticalActivities);
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get detailed activity breakdown for a specific time period
     */
    @GetMapping("/analysis/project/{projectId}")
    public ResponseEntity<Map<String, Object>> getActivityAnalysis(
            @PathVariable Long projectId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @AuthenticationPrincipal OAuth2User principal) {
        
        User user = userService.getCurrentUser(principal);
        Project project = projectService.getProjectById(projectId);
        
        Map<String, Object> analysis = taskHistoryService.getActivityAnalysis(
            projectId, startDate, endDate);
        
        return ResponseEntity.ok(analysis);
    }
}
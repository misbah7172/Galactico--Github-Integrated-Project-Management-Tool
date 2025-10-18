package com.autotrack.service;

import com.autotrack.model.*;
import com.autotrack.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for managing task history and timeline functionality.
 * Provides Jira-like activity tracking and analysis.
 */
@Service
@Transactional
public class TaskHistoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskHistoryService.class);
    
    private final TaskHistoryRepository taskHistoryRepository;
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    
    @Autowired
    public TaskHistoryService(TaskHistoryRepository taskHistoryRepository,
                             TaskRepository taskRepository,
                             ProjectRepository projectRepository,
                             UserRepository userRepository) {
        this.taskHistoryRepository = taskHistoryRepository;
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * Record a new task history entry
     */
    public TaskHistory recordActivity(Task task, User user, HistoryActionType actionType, String description) {
        TaskHistory history = new TaskHistory(task, user, actionType, description);
        TaskHistory saved = taskHistoryRepository.save(history);
        
        logger.debug("Recorded activity: {} for task {} by user {}", 
                    actionType, task.getId(), user.getNickname());
        
        return saved;
    }
    
    /**
     * Record field change activity
     */
    public TaskHistory recordFieldChange(Task task, User user, String fieldName, 
                                        String oldValue, String newValue) {
        HistoryActionType actionType = determineActionType(fieldName);
        TaskHistory history = new TaskHistory(task, user, actionType, fieldName, oldValue, newValue);
        TaskHistory saved = taskHistoryRepository.save(history);
        
        logger.debug("Recorded field change: {} from '{}' to '{}' for task {} by user {}", 
                    fieldName, oldValue, newValue, task.getId(), user.getNickname());
        
        return saved;
    }
    
    private HistoryActionType determineActionType(String fieldName) {
        return switch (fieldName.toLowerCase()) {
            case "status" -> HistoryActionType.STATUS_CHANGED;
            case "assignee" -> HistoryActionType.ASSIGNED;
            case "priority", "prioritylevel" -> HistoryActionType.PRIORITY_CHANGED;
            case "sprint" -> HistoryActionType.SPRINT_CHANGED;
            case "description" -> HistoryActionType.DESCRIPTION_UPDATED;
            case "acceptancecriteria" -> HistoryActionType.ACCEPTANCE_CRITERIA_UPDATED;
            case "storypoints", "estimate" -> HistoryActionType.ESTIMATION_UPDATED;
            case "duedate" -> HistoryActionType.DUE_DATE_CHANGED;
            case "tags", "labels" -> HistoryActionType.LABELS_UPDATED;
            default -> HistoryActionType.FIELD_UPDATED;
        };
    }
    
    /**
     * Get task history with pagination
     */
    public Page<TaskHistory> getTaskHistory(Task task, Pageable pageable) {
        return taskHistoryRepository.findByTaskOrderByTimestampDesc(task, pageable);
    }
    
    /**
     * Get project timeline with pagination
     */
    public Page<TaskHistory> getProjectTimeline(Project project, Pageable pageable) {
        return taskHistoryRepository.findByProjectOrderByTimestampDesc(project, pageable);
    }
    
    /**
     * Get sprint timeline with pagination
     */
    public Page<TaskHistory> getSprintTimeline(Long sprintId, Pageable pageable) {
        return taskHistoryRepository.findBySprintIdOrderByTimestampDesc(sprintId, pageable);
    }
    
    /**
     * Get user timeline with pagination
     */
    public Page<TaskHistory> getUserTimeline(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        return taskHistoryRepository.findByUserOrderByTimestampDesc(user, pageable);
    }
    
    /**
     * Get user timeline for specific project
     */
    public Page<TaskHistory> getUserTimelineForProject(Long userId, Long projectId, Pageable pageable) {
        return taskHistoryRepository.findByUserIdAndProjectIdOrderByTimestampDesc(userId, projectId, pageable);
    }
    
    /**
     * Get filtered project timeline
     */
    public Page<TaskHistory> getProjectTimelineFiltered(Project project, String actionTypeStr, 
                                                       Long userId, String startDateStr, String endDateStr, 
                                                       Pageable pageable) {
        HistoryActionType actionType = null;
        if (actionTypeStr != null && !actionTypeStr.trim().isEmpty()) {
            try {
                actionType = HistoryActionType.valueOf(actionTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid action type: {}", actionTypeStr);
            }
        }
        
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        
        if (startDateStr != null && !startDateStr.trim().isEmpty()) {
            startDate = LocalDate.parse(startDateStr).atStartOfDay();
        }
        
        if (endDateStr != null && !endDateStr.trim().isEmpty()) {
            endDate = LocalDate.parse(endDateStr).atTime(23, 59, 59);
        }
        
        return taskHistoryRepository.findByProjectWithFilters(
            project, actionType, userId, startDate, endDate, pageable);
    }
    
    /**
     * Get activity counts by day for analytics
     */
    public Map<String, Long> getActivityCountsByDay(Long projectId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Object[]> results = taskHistoryRepository.getActivityCountsByDay(projectId, startDate);
        
        Map<String, Long> activityByDay = new LinkedHashMap<>();
        for (Object[] result : results) {
            LocalDate date = (LocalDate) result[0];
            Long count = (Long) result[1];
            activityByDay.put(date.toString(), count);
        }
        
        return activityByDay;
    }
    
    /**
     * Get activity counts by type for analytics
     */
    public Map<String, Long> getActivityCountsByType(Long projectId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Object[]> results = taskHistoryRepository.getActivityCountsByType(projectId, startDate);
        
        Map<String, Long> activityByType = new LinkedHashMap<>();
        for (Object[] result : results) {
            HistoryActionType actionType = (HistoryActionType) result[0];
            Long count = (Long) result[1];
            activityByType.put(actionType.getDisplayName(), count);
        }
        
        return activityByType;
    }
    
    /**
     * Get most active users for analytics
     */
    public List<Map<String, Object>> getMostActiveUsers(Long projectId, int days, int limit) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> results = taskHistoryRepository.getMostActiveUsers(projectId, startDate, pageable);
        
        return results.stream().map(result -> {
            Map<String, Object> user = new HashMap<>();
            user.put("userId", result[0]);
            user.put("username", result[1]);
            user.put("avatarUrl", result[2]);
            user.put("activityCount", result[3]);
            return user;
        }).collect(Collectors.toList());
    }
    
    /**
     * Get recent critical activities
     */
    public List<TaskHistory> getRecentCriticalActivities(Long projectId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return taskHistoryRepository.getRecentCriticalActivities(projectId, pageable);
    }
    
    /**
     * Get comprehensive activity analysis for a time period
     */
    public Map<String, Object> getActivityAnalysis(Long projectId, String startDateStr, String endDateStr) {
        LocalDateTime startDate = LocalDate.parse(startDateStr).atStartOfDay();
        LocalDateTime endDate = LocalDate.parse(endDateStr).atTime(23, 59, 59);
        
        Map<String, Object> analysis = new HashMap<>();
        
        // Total activities in period
        Long totalActivities = taskHistoryRepository.countActivitiesInRange(projectId, startDate, endDate);
        analysis.put("totalActivities", totalActivities);
        
        // Activity breakdown by type
        List<Object[]> typeResults = taskHistoryRepository.getActivityCountsByType(projectId, startDate);
        Map<String, Long> activityByType = typeResults.stream()
            .collect(Collectors.toMap(
                result -> ((HistoryActionType) result[0]).getDisplayName(),
                result -> (Long) result[1],
                (existing, replacement) -> existing,
                LinkedHashMap::new
            ));
        analysis.put("activityByType", activityByType);
        
        // Daily activity trend
        List<Object[]> dayResults = taskHistoryRepository.getActivityCountsByDay(projectId, startDate);
        Map<String, Long> dailyTrend = dayResults.stream()
            .collect(Collectors.toMap(
                result -> result[0].toString(),
                result -> (Long) result[1],
                (existing, replacement) -> existing,
                LinkedHashMap::new
            ));
        analysis.put("dailyTrend", dailyTrend);
        
        // Most active users in period
        List<Object[]> userResults = taskHistoryRepository.getMostActiveUsers(
            projectId, startDate, PageRequest.of(0, 10));
        List<Map<String, Object>> activeUsers = userResults.stream().map(result -> {
            Map<String, Object> user = new HashMap<>();
            user.put("userId", result[0]);
            user.put("username", result[1]);
            user.put("avatarUrl", result[2]);
            user.put("activityCount", result[3]);
            return user;
        }).collect(Collectors.toList());
        analysis.put("mostActiveUsers", activeUsers);
        
        return analysis;
    }
    
    /**
     * Utility method to create task creation history
     */
    public void recordTaskCreation(Task task, User user) {
        recordActivity(task, user, HistoryActionType.TASK_CREATED, "Task created");
    }
    
    /**
     * Utility method to record task status change
     */
    public void recordStatusChange(Task task, User user, TaskStatus oldStatus, TaskStatus newStatus) {
        recordFieldChange(task, user, "status", 
                         oldStatus != null ? oldStatus.toString() : null, 
                         newStatus != null ? newStatus.toString() : null);
    }
    
    /**
     * Utility method to record task assignment
     */
    public void recordAssignment(Task task, User user, User assignee) {
        if (assignee != null) {
            recordActivity(task, user, HistoryActionType.ASSIGNED, 
                          "Assigned to " + assignee.getNickname());
        } else {
            recordActivity(task, user, HistoryActionType.UNASSIGNED, "Unassigned from task");
        }
    }
    
    /**
     * Utility method to record sprint change
     */
    public void recordSprintChange(Task task, User user, Sprint oldSprint, Sprint newSprint) {
        String oldSprintName = oldSprint != null ? oldSprint.getName() : "No Sprint";
        String newSprintName = newSprint != null ? newSprint.getName() : "Backlog";
        
        recordFieldChange(task, user, "sprint", oldSprintName, newSprintName);
    }
}
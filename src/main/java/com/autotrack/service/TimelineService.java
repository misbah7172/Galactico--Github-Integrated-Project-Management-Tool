package com.autotrack.service;

import com.autotrack.model.*;
import com.autotrack.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing timeline insights and entity tracking.
 * Provides timeline tracking, risk assessment, and predictive analytics for tasks, sprints, and projects.
 */
@Service
@Transactional
public class TimelineService {
    
    private static final Logger logger = LoggerFactory.getLogger(TimelineService.class);
    
    @Autowired
    private TimelineInsightsRepository timelineInsightsRepository;
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private SprintRepository sprintRepository;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * Create timeline insight for a specific entity (task, sprint, or project)
     */
    public TimelineInsights createTimelineInsight(TimelineInsights.EntityType entityType, Long entityId) {
        logger.info("Creating timeline insight for {} with ID: {}", entityType, entityId);
        
        // Check if insight already exists
        Optional<TimelineInsights> existing = timelineInsightsRepository.findByEntityTypeAndEntityId(entityType, entityId);
        if (existing.isPresent()) {
            logger.warn("Timeline insight already exists for {} ID: {}, updating instead", entityType, entityId);
            return updateTimelineInsight(existing.get());
        }
        
        TimelineInsights insight = new TimelineInsights(entityType, entityId);
        
        // Calculate timeline predictions based on entity type
        calculateTimelinePredictions(insight, entityType, entityId);
        
        TimelineInsights saved = timelineInsightsRepository.save(insight);
        logger.info("Timeline insight created successfully with ID: {}", saved.getId());
        
        return saved;
    }
    
    /**
     * Update timeline insight
     */
    public TimelineInsights updateTimelineInsight(TimelineInsights insight) {
        logger.info("Updating timeline insight: {}", insight.getId());
        
        // Recalculate predictions
        calculateTimelinePredictions(insight, insight.getEntityType(), insight.getEntityId());
        
        insight.setUpdatedAt(LocalDateTime.now());
        
        TimelineInsights saved = timelineInsightsRepository.save(insight);
        logger.info("Timeline insight updated successfully: {}", saved.getId());
        
        return saved;
    }
    
    /**
     * Delete timeline insight
     */
    public void deleteTimelineInsight(Long insightId) {
        logger.info("Deleting timeline insight: {}", insightId);
        
        TimelineInsights insight = getTimelineInsightById(insightId);
        timelineInsightsRepository.delete(insight);
        
        logger.info("Timeline insight deleted successfully: {}", insightId);
    }
    
    /**
     * Get timeline insight by ID
     */
    public TimelineInsights getTimelineInsightById(Long id) {
        return timelineInsightsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Timeline insight not found with id: " + id));
    }
    
    /**
     * Get timeline insight by entity type and ID
     */
    public Optional<TimelineInsights> getTimelineInsightByEntity(TimelineInsights.EntityType entityType, Long entityId) {
        return timelineInsightsRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }
    
    /**
     * Get all timeline insights by entity type
     */
    public List<TimelineInsights> getTimelineInsightsByType(TimelineInsights.EntityType entityType) {
        return timelineInsightsRepository.findByEntityTypeOrderByUpdatedAtDesc(entityType);
    }
    
    /**
     * Generate or update timeline insights for all tasks, sprints, and projects
     */
    public void generateAllTimelineInsights() {
        logger.info("Starting comprehensive timeline insights generation");
        
        try {
            // Generate insights for all tasks
            generateTaskInsights();
            
            // Generate insights for all sprints
            generateSprintInsights();
            
            // Generate insights for all projects
            generateProjectInsights();
            
            logger.info("Comprehensive timeline insights generation completed successfully");
        } catch (Exception e) {
            logger.error("Error during timeline insights generation: {}", e.getMessage());
            throw new RuntimeException("Failed to generate timeline insights", e);
        }
    }
    
    /**
     * Generate timeline insights for all tasks
     */
    private void generateTaskInsights() {
        logger.debug("Generating timeline insights for all tasks");
        
        List<Task> allTasks = taskRepository.findAll();
        for (Task task : allTasks) {
            try {
                createOrUpdateTaskInsight(task);
            } catch (Exception e) {
                logger.warn("Error generating insight for task {}: {}", task.getId(), e.getMessage());
            }
        }
    }
    
    /**
     * Generate timeline insights for all sprints
     */
    private void generateSprintInsights() {
        logger.debug("Generating timeline insights for all sprints");
        
        List<Sprint> allSprints = sprintRepository.findAll();
        for (Sprint sprint : allSprints) {
            try {
                createOrUpdateSprintInsight(sprint);
            } catch (Exception e) {
                logger.warn("Error generating insight for sprint {}: {}", sprint.getId(), e.getMessage());
            }
        }
    }
    
    /**
     * Generate timeline insights for all projects
     */
    private void generateProjectInsights() {
        logger.debug("Generating timeline insights for all projects");
        
        List<Project> allProjects = projectRepository.findAll();
        for (Project project : allProjects) {
            try {
                createOrUpdateProjectInsight(project);
            } catch (Exception e) {
                logger.warn("Error generating insight for project {}: {}", project.getId(), e.getMessage());
            }
        }
    }
    
    /**
     * Calculate timeline predictions based on entity type
     */
    private void calculateTimelinePredictions(TimelineInsights insight, TimelineInsights.EntityType entityType, Long entityId) {
        logger.debug("Calculating timeline predictions for {} with ID: {}", entityType, entityId);
        
        switch (entityType) {
            case TASK:
                calculateTaskTimelinePredictions(insight, entityId);
                break;
            case SPRINT:
                calculateSprintTimelinePredictions(insight, entityId);
                break;
            case PROJECT:
                calculateProjectTimelinePredictions(insight, entityId);
                break;
            default:
                logger.warn("Unknown entity type: {}", entityType);
        }
        
        logger.debug("Timeline predictions calculation completed for {} ID: {}", entityType, entityId);
    }
    
    /**
     * Create or update task insight
     */
    private void createOrUpdateTaskInsight(Task task) {
        Optional<TimelineInsights> existingInsight = timelineInsightsRepository
            .findByEntityTypeAndEntityId(TimelineInsights.EntityType.TASK, task.getId());
        
        if (existingInsight.isPresent()) {
            updateTimelineInsight(existingInsight.get());
        } else {
            createTimelineInsight(TimelineInsights.EntityType.TASK, task.getId());
        }
    }
    
    /**
     * Create or update sprint insight
     */
    private void createOrUpdateSprintInsight(Sprint sprint) {
        Optional<TimelineInsights> existingInsight = timelineInsightsRepository
            .findByEntityTypeAndEntityId(TimelineInsights.EntityType.SPRINT, sprint.getId());
        
        if (existingInsight.isPresent()) {
            updateTimelineInsight(existingInsight.get());
        } else {
            createTimelineInsight(TimelineInsights.EntityType.SPRINT, sprint.getId());
        }
    }
    
    /**
     * Create or update project insight
     */
    private void createOrUpdateProjectInsight(Project project) {
        Optional<TimelineInsights> existingInsight = timelineInsightsRepository
            .findByEntityTypeAndEntityId(TimelineInsights.EntityType.PROJECT, project.getId());
        
        if (existingInsight.isPresent()) {
            updateTimelineInsight(existingInsight.get());
        } else {
            createTimelineInsight(TimelineInsights.EntityType.PROJECT, project.getId());
        }
    }
    
    /**
     * Calculate timeline predictions for a task
     */
    private void calculateTaskTimelinePredictions(TimelineInsights insight, Long taskId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            logger.warn("Task not found with ID: {}", taskId);
            return;
        }
        
        Task task = taskOpt.get();
        
        // Set estimated dates based on task data
        if (task.getSprint() != null) {
            insight.setEstimatedStartDate(task.getSprint().getStartDate().toLocalDate());
            insight.setEstimatedEndDate(task.getSprint().getEndDate().toLocalDate());
        }
        
        // Calculate progress based on task status
        BigDecimal progress = switch (task.getStatus()) {
            case TODO -> BigDecimal.ZERO;
            case IN_PROGRESS -> BigDecimal.valueOf(50);
            case DONE -> BigDecimal.valueOf(100);
            default -> BigDecimal.valueOf(25); // For other statuses
        };
        insight.setProgressPercentage(progress);
        
        // Set risk level based on task priority
        if (task.getPriorityLevel() != null) {
            insight.setTimelineRiskLevel(task.getPriorityLevel());
        }
        
        // Calculate duration from estimates
        if (task.getOriginalEstimateHours() != null) {
            // Convert hours to days (assuming 8 hours per day)
            int estimatedDays = task.getOriginalEstimateHours().divide(BigDecimal.valueOf(8), 0, RoundingMode.HALF_UP).intValue();
            insight.setEstimatedDurationDays(estimatedDays);
        }
        
        // Set actual duration if task is completed
        if (task.getStatus() == TaskStatus.DONE && task.getLoggedHours() != null) {
            int actualDays = task.getLoggedHours().divide(BigDecimal.valueOf(8), 0, RoundingMode.HALF_UP).intValue();
            insight.setActualDurationDays(actualDays);
        }
    }
    
    /**
     * Calculate timeline predictions for a sprint
     */
    private void calculateSprintTimelinePredictions(TimelineInsights insight, Long sprintId) {
        Optional<Sprint> sprintOpt = sprintRepository.findById(sprintId);
        if (sprintOpt.isEmpty()) {
            logger.warn("Sprint not found with ID: {}", sprintId);
            return;
        }
        
        Sprint sprint = sprintOpt.get();
        
        // Set timeline dates
        insight.setEstimatedStartDate(sprint.getStartDate().toLocalDate());
        insight.setEstimatedEndDate(sprint.getEndDate().toLocalDate());
        
        if (sprint.getStatus() == SprintStatus.ACTIVE) {
            insight.setActualStartDate(sprint.getStartDate().toLocalDate());
        } else if (sprint.getStatus() == SprintStatus.COMPLETED) {
            insight.setActualStartDate(sprint.getStartDate().toLocalDate());
            insight.setActualEndDate(sprint.getEndDate().toLocalDate());
        }
        
        // Calculate sprint progress based on completed tasks
        List<Task> sprintTasks = taskRepository.findBySprintOrderByUpdatedAtDesc(sprint);
        if (!sprintTasks.isEmpty()) {
            long completedTasks = sprintTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.DONE)
                .count();
            BigDecimal progress = BigDecimal.valueOf(completedTasks * 100.0 / sprintTasks.size())
                .setScale(2, RoundingMode.HALF_UP);
            insight.setProgressPercentage(progress);
        }
        
        // Calculate duration
        long estimatedDays = ChronoUnit.DAYS.between(sprint.getStartDate(), sprint.getEndDate());
        insight.setEstimatedDurationDays((int) estimatedDays);
        
        if (sprint.getStatus() == SprintStatus.COMPLETED) {
            insight.setActualDurationDays((int) estimatedDays);
        }
        
        // Set risk level based on sprint status and overdue check
        if (sprint.getEndDate().isBefore(LocalDateTime.now()) && sprint.getStatus() != SprintStatus.COMPLETED) {
            insight.setTimelineRiskLevel(PriorityLevel.HIGH);
            insight.setRiskFactors("Sprint is overdue");
        } else if (sprint.getStatus() == SprintStatus.ACTIVE) {
            insight.setTimelineRiskLevel(PriorityLevel.MEDIUM);
        } else {
            insight.setTimelineRiskLevel(PriorityLevel.LOW);
        }
    }
    
    /**
     * Calculate timeline predictions for a project
     */
    private void calculateProjectTimelinePredictions(TimelineInsights insight, Long projectId) {
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            logger.warn("Project not found with ID: {}", projectId);
            return;
        }
        
        Project project = projectOpt.get();
        
        // Find earliest and latest sprint dates for project timeline
        List<Sprint> projectSprints = sprintRepository.findByProjectOrderByStartDateDesc(project);
        if (!projectSprints.isEmpty()) {
            LocalDate earliestStart = projectSprints.stream()
                .map(sprint -> sprint.getStartDate().toLocalDate())
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());
            
            LocalDate latestEnd = projectSprints.stream()
                .map(sprint -> sprint.getEndDate().toLocalDate())
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());
            
            insight.setEstimatedStartDate(earliestStart);
            insight.setEstimatedEndDate(latestEnd);
            
            // Calculate duration
            long estimatedDays = ChronoUnit.DAYS.between(earliestStart, latestEnd);
            insight.setEstimatedDurationDays((int) estimatedDays);
        }
        
        // Calculate overall project progress
        List<Task> allTasks = taskRepository.findAll().stream()
            .filter(task -> task.getProject().getId().equals(projectId))
            .collect(Collectors.toList());
        
        if (!allTasks.isEmpty()) {
            long completedTasks = allTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.DONE)
                .count();
            BigDecimal progress = BigDecimal.valueOf(completedTasks * 100.0 / allTasks.size())
                .setScale(2, RoundingMode.HALF_UP);
            insight.setProgressPercentage(progress);
        }
        
        // Assess project risk based on sprint statuses
        long overdueSprints = projectSprints.stream()
            .filter(sprint -> sprint.getEndDate().isBefore(LocalDateTime.now()))
            .filter(sprint -> sprint.getStatus() != SprintStatus.COMPLETED)
            .count();
        
        if (overdueSprints > 0) {
            insight.setTimelineRiskLevel(PriorityLevel.HIGH);
            insight.setRiskFactors("Project has " + overdueSprints + " overdue sprint(s)");
        } else {
            insight.setTimelineRiskLevel(PriorityLevel.LOW);
        }
    }
    
    /**
     * Get overdue timeline insights
     */
    public List<TimelineInsights> getOverdueInsights() {
        return timelineInsightsRepository.findOverdueItems(LocalDate.now());
    }
    
    /**
     * Get high-risk timeline insights
     */
    public List<TimelineInsights> getHighRiskInsights() {
        return timelineInsightsRepository.findByTimelineRiskLevelOrderByEstimatedEndDateAsc(PriorityLevel.HIGH);
    }
    
    /**
     * Get items behind schedule
     */
    public List<TimelineInsights> getBehindScheduleInsights() {
        return timelineInsightsRepository.findBehindScheduleItems();
    }
    
    /**
     * Generate timeline summary for dashboard
     */
    public Map<String, Object> getTimelineSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        List<TimelineInsights> allInsights = timelineInsightsRepository.findAll();
        
        // Count by entity type
        Map<TimelineInsights.EntityType, Long> countsByType = allInsights.stream()
            .collect(Collectors.groupingBy(
                TimelineInsights::getEntityType,
                Collectors.counting()
            ));
        
        summary.put("totalInsights", allInsights.size());
        summary.put("taskInsights", countsByType.getOrDefault(TimelineInsights.EntityType.TASK, 0L));
        summary.put("sprintInsights", countsByType.getOrDefault(TimelineInsights.EntityType.SPRINT, 0L));
        summary.put("projectInsights", countsByType.getOrDefault(TimelineInsights.EntityType.PROJECT, 0L));
        
        // Risk level distribution
        Map<PriorityLevel, Long> riskDistribution = allInsights.stream()
            .collect(Collectors.groupingBy(
                insight -> insight.getTimelineRiskLevel() != null ? insight.getTimelineRiskLevel() : PriorityLevel.LOW,
                Collectors.counting()
            ));
        
        summary.put("riskDistribution", riskDistribution);
        
        // Average progress
        OptionalDouble avgProgress = allInsights.stream()
            .filter(insight -> insight.getProgressPercentage() != null)
            .mapToDouble(insight -> insight.getProgressPercentage().doubleValue())
            .average();
        
        summary.put("averageProgress", avgProgress.isPresent() ? 
            BigDecimal.valueOf(avgProgress.getAsDouble()).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        
        return summary;
    }
}
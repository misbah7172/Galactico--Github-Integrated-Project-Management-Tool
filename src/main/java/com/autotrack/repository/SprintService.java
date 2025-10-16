package com.autotrack.service;

import com.autotrack.dto.BurndownDataPoint;
import com.autotrack.dto.SprintProgressDto;
import com.autotrack.dto.TaskSummaryDto;
import com.autotrack.model.*;
import com.autotrack.repository.SprintRepository;
import com.autotrack.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class for managing sprints.
 * Provides comprehensive sprint management functionality similar to Jira.
 */
@Service
@Transactional
public class SprintService {
    
    private static final Logger logger = LoggerFactory.getLogger(SprintService.class);
    
    @Autowired
    private SprintRepository sprintRepository;
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * Create a new sprint
     */
    public Sprint createSprint(Sprint sprint) {
        logger.info("Creating new sprint: {} for project: {}", sprint.getName(), sprint.getProject().getName());
        
        // Validate sprint dates
        validateSprintDates(sprint);
        
        // Check for overlapping sprints
        validateNoOverlappingSprints(sprint);
        
        // Set initial status based on dates
        sprint.setStatus(determineInitialStatus(sprint));
        
        Sprint savedSprint = sprintRepository.save(sprint);
        logger.info("Sprint created successfully with ID: {}", savedSprint.getId());
        
        return savedSprint;
    }
    
    /**
     * Update an existing sprint
     */
    public Sprint updateSprint(Sprint sprint) {
        logger.info("Updating sprint: {}", sprint.getId());
        
        validateSprintDates(sprint);
        validateNoOverlappingSprints(sprint);
        
        Sprint savedSprint = sprintRepository.save(sprint);
        logger.info("Sprint updated successfully: {}", savedSprint.getId());
        
        return savedSprint;
    }
    
    /**
     * Delete a sprint and handle associated tasks
     */
    public void deleteSprint(Long sprintId, String taskAction) {
        logger.info("Deleting sprint: {} with task action: {}", sprintId, taskAction);
        
        Sprint sprint = getSprintById(sprintId);
        List<Task> sprintTasks = taskRepository.findBySprintOrderByUpdatedAtDesc(sprint);
        
        if ("UNASSIGN".equals(taskAction)) {
            // Remove sprint assignment from all tasks
            sprintTasks.forEach(task -> {
                task.setSprint(null);
                taskRepository.save(task);
            });
            logger.info("Unassigned {} tasks from sprint {}", sprintTasks.size(), sprintId);
        } else if ("ARCHIVE".equals(taskAction)) {
            // Archive tasks (you might want to add an archived field to Task entity)
            logger.info("Archiving {} tasks from sprint {}", sprintTasks.size(), sprintId);
        }
        
        sprintRepository.delete(sprint);
        logger.info("Sprint deleted successfully: {}", sprintId);
    }
    
    /**
     * Get sprint by ID
     */
    public Sprint getSprintById(Long id) {
        return sprintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sprint not found with id: " + id));
    }
    
    /**
     * Get all sprints for a project
     */
    public List<Sprint> getSprintsByProject(Project project) {
        return sprintRepository.findByProjectOrderByStartDateDesc(project);
    }
    
    /**
     * Get all sprints for a project by project ID
     */
    public List<Sprint> getSprintsByProjectId(Long projectId) {
        return sprintRepository.findByProjectIdOrderByStartDateDesc(projectId);
    }
    
    /**
     * Get active sprint for a project
     */
    public Optional<Sprint> getActiveSprintByProject(Long projectId) {
        return sprintRepository.findActiveSprintByProjectId(projectId);
    }
    
    /**
     * Calculate comprehensive sprint progress
     */
    public SprintProgressDto getSprintProgress(Long sprintId) {
        Sprint sprint = getSprintById(sprintId);
        List<Task> sprintTasks = taskRepository.findBySprintOrderByUpdatedAtDesc(sprint);
        
        SprintProgressDto progressDto = new SprintProgressDto(
                sprint.getId(),
                sprint.getName(),
                sprint.getDescription(),
                sprint.getStartDate(),
                sprint.getEndDate(),
                sprint.getStatus(),
                sprint.getProject().getName(),
                sprint.getProject().getId()
        );
        
        // Calculate task statistics
        int totalTasks = sprintTasks.size();
        long todoTasks = sprintTasks.stream().filter(t -> t.getStatus() == TaskStatus.TODO).count();
        long inProgressTasks = sprintTasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
        long doneTasks = sprintTasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
        
        progressDto.setTotalTasks(totalTasks);
        progressDto.setTodoTasks((int) todoTasks);
        progressDto.setInProgressTasks((int) inProgressTasks);
        progressDto.setDoneTasks((int) doneTasks);
        
        // Convert tasks to DTOs
        List<TaskSummaryDto> taskSummaries = sprintTasks.stream()
                .map(this::convertToTaskSummary)
                .collect(Collectors.toList());
        progressDto.setTasks(taskSummaries);
        
        // Generate burndown data if sprint is active or completed
        if (sprint.getStatus() == SprintStatus.ACTIVE || sprint.getStatus() == SprintStatus.COMPLETED) {
            progressDto.setBurndownData(generateBurndownData(sprint, sprintTasks));
        }
        
        return progressDto;
    }
    
    /**
     * Assign a task to a sprint
     */
    public void assignTaskToSprint(Long taskId, Long sprintId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));
        Sprint sprint = getSprintById(sprintId);
        
        // Validate that task belongs to the same project as sprint
        if (!task.getProject().getId().equals(sprint.getProject().getId())) {
            throw new RuntimeException("Task and sprint must belong to the same project");
        }
        
        task.setSprint(sprint);
        taskRepository.save(task);
        
        logger.info("Task {} assigned to sprint {}", taskId, sprintId);
    }
    
    /**
     * Remove task from sprint
     */
    public void removeTaskFromSprint(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));
        
        task.setSprint(null);
        taskRepository.save(task);
        
        logger.info("Task {} removed from sprint", taskId);
    }
    
    /**
     * Start a sprint (change status to ACTIVE)
     */
    public Sprint startSprint(Long sprintId) {
        Sprint sprint = getSprintById(sprintId);
        
        // Check if there's already an active sprint for this project
        Optional<Sprint> activeSprint = getActiveSprintByProject(sprint.getProject().getId());
        if (activeSprint.isPresent() && !activeSprint.get().getId().equals(sprintId)) {
            throw new RuntimeException("There is already an active sprint for this project");
        }
        
        sprint.setStatus(SprintStatus.ACTIVE);
        Sprint savedSprint = sprintRepository.save(sprint);
        
        // Send notifications to team members
        notificationService.sendSprintStartNotification(sprint);
        
        logger.info("Sprint started: {}", sprintId);
        return savedSprint;
    }
    
    /**
     * Complete a sprint
     */
    public Sprint completeSprint(Long sprintId) {
        Sprint sprint = getSprintById(sprintId);
        sprint.setStatus(SprintStatus.COMPLETED);
        Sprint savedSprint = sprintRepository.save(sprint);
        
        // Send completion notifications
        notificationService.sendSprintCompletionNotification(sprint);
        
        logger.info("Sprint completed: {}", sprintId);
        return savedSprint;
    }
    
    /**
     * Process automatic sprint status updates (called by scheduler)
     */
    public void processAutomaticSprintStatusUpdates() {
        LocalDateTime now = LocalDateTime.now();
        
        // Activate upcoming sprints
        List<Sprint> sprintsToActivate = sprintRepository.findSprintsToActivate(now);
        for (Sprint sprint : sprintsToActivate) {
            try {
                startSprint(sprint.getId());
            } catch (Exception e) {
                logger.error("Failed to automatically start sprint {}: {}", sprint.getId(), e.getMessage());
            }
        }
        
        // Complete overdue sprints with all tasks done
        List<Sprint> sprintsToComplete = sprintRepository.findSprintsToComplete(now);
        for (Sprint sprint : sprintsToComplete) {
            List<Task> incompleteTasks = taskRepository.findBySprintAndStatusNot(sprint, TaskStatus.DONE);
            if (incompleteTasks.isEmpty()) {
                try {
                    completeSprint(sprint.getId());
                } catch (Exception e) {
                    logger.error("Failed to automatically complete sprint {}: {}", sprint.getId(), e.getMessage());
                }
            }
        }
        
        logger.info("Processed automatic sprint status updates");
    }
    
    /**
     * Send reminders for sprints ending soon with incomplete tasks
     */
    public void sendSprintEndingReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDaysFromNow = now.plusDays(3);
        
        List<Sprint> endingSprints = sprintRepository.findActiveSprintsEndingSoonWithIncompleteTasks(now, threeDaysFromNow);
        
        for (Sprint sprint : endingSprints) {
            SprintProgressDto progress = getSprintProgress(sprint.getId());
            if (progress.getCompletionPercentage() < 50) {
                notificationService.sendSprintEndingReminderNotification(sprint, progress);
            }
        }
        
        logger.info("Sent ending reminders for {} sprints", endingSprints.size());
    }
    
    // Private helper methods
    
    private void validateSprintDates(Sprint sprint) {
        if (sprint.getStartDate().isAfter(sprint.getEndDate())) {
            throw new RuntimeException("Sprint start date must be before end date");
        }
        
        if (sprint.getEndDate().isBefore(LocalDateTime.now().minusDays(1))) {
            throw new RuntimeException("Sprint end date cannot be in the past");
        }
    }
    
    private void validateNoOverlappingSprints(Sprint sprint) {
        Long sprintId = sprint.getId() != null ? sprint.getId() : -1L;
        long overlappingCount = sprintRepository.countOverlappingSprints(
                sprint.getProject().getId(),
                sprintId,
                sprint.getStartDate(),
                sprint.getEndDate()
        );
        
        if (overlappingCount > 0) {
            throw new RuntimeException("Sprint dates overlap with existing sprints");
        }
    }
    
    private SprintStatus determineInitialStatus(Sprint sprint) {
        LocalDateTime now = LocalDateTime.now();
        
        if (now.isBefore(sprint.getStartDate())) {
            return SprintStatus.UPCOMING;
        } else if (now.isAfter(sprint.getEndDate())) {
            return SprintStatus.COMPLETED;
        } else {
            return SprintStatus.ACTIVE;
        }
    }
    
    private TaskSummaryDto convertToTaskSummary(Task task) {
        return new TaskSummaryDto(
                task.getId(),
                task.getFeatureCode(),
                task.getTitle(),
                task.getStatus(),
                task.getAssignee() != null ? task.getAssignee().getNickname() : null,
                task.getAssignee() != null ? task.getAssignee().getAvatarUrl() : null,
                task.getUpdatedAt(),
                task.getMilestone()
        );
    }
    
    private List<BurndownDataPoint> generateBurndownData(Sprint sprint, List<Task> tasks) {
        List<BurndownDataPoint> burndownData = new ArrayList<>();
        
        LocalDate startDate = sprint.getStartDate().toLocalDate();
        LocalDate endDate = sprint.getEndDate().toLocalDate();
        LocalDate currentDate = startDate;
        
        int totalTasks = tasks.size();
        long totalDays = java.time.Duration.between(sprint.getStartDate(), sprint.getEndDate()).toDays();
        
        while (!currentDate.isAfter(endDate) && !currentDate.isAfter(LocalDate.now())) {
            // Calculate ideal remaining tasks (linear burndown)
            long daysElapsed = java.time.Duration.between(startDate.atStartOfDay(), currentDate.atStartOfDay()).toDays();
            int idealRemaining = totalTasks - (int) ((double) totalTasks * daysElapsed / totalDays);
            
            // Calculate actual remaining tasks (for simplicity, using current status)
            // In a real implementation, you'd track historical status changes
            int actualRemaining = (int) tasks.stream()
                    .filter(task -> task.getStatus() != TaskStatus.DONE)
                    .count();
            
            burndownData.add(new BurndownDataPoint(currentDate, actualRemaining, idealRemaining));
            currentDate = currentDate.plusDays(1);
        }
        
        return burndownData;
    }
}

package com.autotrack.service;

import com.autotrack.model.Task;
import com.autotrack.model.TaskStatus;
import com.autotrack.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for task reminders and notifications.
 */
@Service
public class TaskReminderService {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskReminderService.class);
    
    private final TaskRepository taskRepository;
    private final SlackService slackService;
    
    public TaskReminderService(TaskRepository taskRepository, SlackService slackService) {
        this.taskRepository = taskRepository;
        this.slackService = slackService;
    }
    
    /**
     * Check for idle tasks and send reminders.
     * Runs every hour.
     */
    @Scheduled(fixedRate = 3600000) // 1 hour = 3600000 ms
    public void checkIdleTasks() {
        logger.debug("Checking for idle tasks...");
        
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        
        // Find tasks that haven't been updated in 1 day and are still in progress
        List<Task> idleTasks = taskRepository.findByStatusAndUpdatedAtBefore(TaskStatus.IN_PROGRESS, oneDayAgo);
        
        for (Task task : idleTasks) {
            if (task.getAssignee() != null) {
                if (task.getUpdatedAt().isBefore(threeDaysAgo)) {
                    // Task is very idle (3+ days)
                    slackService.sendTaskReminderNotification(
                        task.getTitle(),
                        task.getFeatureCode(),
                        task.getAssignee().getNickname(),
                        "idle for 3+ days"
                    );
                } else {
                    // Task is idle (1+ day)
                    slackService.sendTaskReminderNotification(
                        task.getTitle(),
                        task.getFeatureCode(),
                        task.getAssignee().getNickname(),
                        "idle for 1+ day"
                    );
                }
            }
        }
        
        logger.debug("Checked {} idle tasks", idleTasks.size());
    }
    
    /**
     * Check for old TODO tasks and send reminders.
     * Runs daily at 9 AM.
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void checkOldTodoTasks() {
        logger.debug("Checking for old TODO tasks...");
        
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        
        // Find TODO tasks that haven't been touched in a week
        List<Task> oldTodoTasks = taskRepository.findByStatusAndUpdatedAtBefore(TaskStatus.TODO, weekAgo);
        
        for (Task task : oldTodoTasks) {
            if (task.getAssignee() != null) {
                slackService.sendTaskReminderNotification(
                    task.getTitle(),
                    task.getFeatureCode(),
                    task.getAssignee().getNickname(),
                    "pending for over a week"
                );
            }
        }
        
        logger.debug("Checked {} old TODO tasks", oldTodoTasks.size());
    }
    
    /**
     * Send a manual reminder for a specific task.
     * 
     * @param taskId The task ID
     * @param reminderType The type of reminder
     */
    public void sendManualReminder(Long taskId, String reminderType) {
        Task task = taskRepository.findById(taskId).orElse(null);
        
        if (task != null && task.getAssignee() != null) {
            slackService.sendTaskReminderNotification(
                task.getTitle(),
                task.getFeatureCode(),
                task.getAssignee().getNickname(),
                reminderType
            );
            
            logger.info("Sent manual reminder for task {} to {}", task.getFeatureCode(), task.getAssignee().getNickname());
        }
    }
}

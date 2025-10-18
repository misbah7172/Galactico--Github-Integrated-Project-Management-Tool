package com.autotrack.config;

import com.autotrack.service.SprintService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for automatic sprint lifecycle management
 * Handles automatic sprint status transitions and reminders
 */
@Component
public class SprintScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(SprintScheduler.class);
    
    private final SprintService sprintService;
    
    @Autowired
    public SprintScheduler(SprintService sprintService) {
        this.sprintService = sprintService;
    }
    
    /**
     * Process automatic sprint status updates daily at 6 AM
     */
    @Scheduled(cron = "0 0 6 * * *")
    public void processSprintStatusUpdates() {
        logger.info("Starting automatic sprint status update process");
        
        try {
            sprintService.processAutomaticSprintStatusUpdates();
            logger.info("Completed automatic sprint status update process");
        } catch (Exception e) {
            logger.error("Error during automatic sprint status update process", e);
        }
    }
    
    /**
     * Send sprint ending reminders daily at 9 AM
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendSprintEndingReminders() {
        logger.info("Starting sprint ending reminder process");
        
        try {
            sprintService.sendSprintEndingReminders();
            logger.info("Completed sprint ending reminder process");
        } catch (Exception e) {
            logger.error("Error during sprint ending reminder process", e);
        }
    }
    
    /**
     * For testing purposes - runs every hour during development
     * Remove or comment out in production
     */
    // @Scheduled(fixedRate = 3600000) // Every hour
    public void hourlySprintCheck() {
        logger.debug("Running hourly sprint check");
        
        try {
            sprintService.processAutomaticSprintStatusUpdates();
        } catch (Exception e) {
            logger.error("Error during hourly sprint check", e);
        }
    }
}

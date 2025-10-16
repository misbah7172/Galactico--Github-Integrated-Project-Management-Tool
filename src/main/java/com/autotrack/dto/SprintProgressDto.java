package com.autotrack.dto;

import com.autotrack.model.SprintStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO representing sprint progress and statistics.
 * Contains comprehensive data for sprint dashboard UI.
 */
public class SprintProgressDto {
    
    private Long sprintId;
    private String sprintName;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private SprintStatus status;
    private String projectName;
    private Long projectId;
    
    // Progress statistics
    private int totalTasks;
    private int todoTasks;
    private int inProgressTasks;
    private int doneTasks;
    private double completionPercentage;
    
    // Time-based information
    private long totalDays;
    private long remainingDays;
    private long elapsedDays;
    private boolean isOverdue;
    
    // Task breakdown by status
    private List<TaskSummaryDto> tasks;
    
    // Burndown chart data (optional)
    private List<BurndownDataPoint> burndownData;
    
    // Constructors
    public SprintProgressDto() {}
    
    public SprintProgressDto(Long sprintId, String sprintName, String description,
                            LocalDateTime startDate, LocalDateTime endDate, SprintStatus status,
                            String projectName, Long projectId) {
        this.sprintId = sprintId;
        this.sprintName = sprintName;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.projectName = projectName;
        this.projectId = projectId;
        
        calculateTimingInfo();
    }
    
    // Getters and Setters
    public Long getSprintId() {
        return sprintId;
    }
    
    public void setSprintId(Long sprintId) {
        this.sprintId = sprintId;
    }
    
    public String getSprintName() {
        return sprintName;
    }
    
    public void setSprintName(String sprintName) {
        this.sprintName = sprintName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
        calculateTimingInfo();
    }
    
    public LocalDateTime getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
        calculateTimingInfo();
    }
    
    public SprintStatus getStatus() {
        return status;
    }
    
    public void setStatus(SprintStatus status) {
        this.status = status;
    }
    
    public String getProjectName() {
        return projectName;
    }
    
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    
    public Long getProjectId() {
        return projectId;
    }
    
    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }
    
    public int getTotalTasks() {
        return totalTasks;
    }
    
    public void setTotalTasks(int totalTasks) {
        this.totalTasks = totalTasks;
        calculateCompletionPercentage();
    }
    
    public int getTodoTasks() {
        return todoTasks;
    }
    
    public void setTodoTasks(int todoTasks) {
        this.todoTasks = todoTasks;
        calculateCompletionPercentage();
    }
    
    public int getInProgressTasks() {
        return inProgressTasks;
    }
    
    public void setInProgressTasks(int inProgressTasks) {
        this.inProgressTasks = inProgressTasks;
        calculateCompletionPercentage();
    }
    
    public int getDoneTasks() {
        return doneTasks;
    }
    
    public void setDoneTasks(int doneTasks) {
        this.doneTasks = doneTasks;
        calculateCompletionPercentage();
    }
    
    public double getCompletionPercentage() {
        return completionPercentage;
    }
    
    public void setCompletionPercentage(double completionPercentage) {
        this.completionPercentage = completionPercentage;
    }
    
    public long getTotalDays() {
        return totalDays;
    }
    
    public void setTotalDays(long totalDays) {
        this.totalDays = totalDays;
    }
    
    public long getRemainingDays() {
        return remainingDays;
    }
    
    public void setRemainingDays(long remainingDays) {
        this.remainingDays = remainingDays;
    }
    
    public long getElapsedDays() {
        return elapsedDays;
    }
    
    public void setElapsedDays(long elapsedDays) {
        this.elapsedDays = elapsedDays;
    }
    
    public boolean isOverdue() {
        return isOverdue;
    }
    
    public void setOverdue(boolean overdue) {
        isOverdue = overdue;
    }
    
    public List<TaskSummaryDto> getTasks() {
        return tasks;
    }
    
    public void setTasks(List<TaskSummaryDto> tasks) {
        this.tasks = tasks;
    }
    
    public List<BurndownDataPoint> getBurndownData() {
        return burndownData;
    }
    
    public void setBurndownData(List<BurndownDataPoint> burndownData) {
        this.burndownData = burndownData;
    }
    
    // Helper methods
    private void calculateCompletionPercentage() {
        if (totalTasks == 0) {
            this.completionPercentage = 0.0;
        } else {
            this.completionPercentage = Math.round((double) doneTasks / totalTasks * 100.0 * 100.0) / 100.0;
        }
    }
    
    private void calculateTimingInfo() {
        if (startDate != null && endDate != null) {
            LocalDateTime now = LocalDateTime.now();
            
            this.totalDays = java.time.Duration.between(startDate, endDate).toDays();
            
            if (now.isAfter(endDate)) {
                this.remainingDays = 0;
                this.elapsedDays = totalDays;
                this.isOverdue = true;
            } else if (now.isBefore(startDate)) {
                this.remainingDays = totalDays;
                this.elapsedDays = 0;
                this.isOverdue = false;
            } else {
                this.elapsedDays = java.time.Duration.between(startDate, now).toDays();
                this.remainingDays = java.time.Duration.between(now, endDate).toDays();
                this.isOverdue = false;
            }
        }
    }
    
    /**
     * Check if the sprint is at risk (>50% time elapsed but <50% tasks completed)
     */
    public boolean isAtRisk() {
        if (totalDays == 0) return false;
        
        double timeProgress = (double) elapsedDays / totalDays;
        double taskProgress = completionPercentage / 100.0;
        
        return timeProgress > 0.5 && taskProgress < 0.5;
    }
    
    /**
     * Get the velocity (tasks completed per day)
     */
    public double getVelocity() {
        if (elapsedDays == 0) return 0.0;
        return (double) doneTasks / elapsedDays;
    }
    
    /**
     * Estimate completion date based on current velocity
     */
    public LocalDateTime getEstimatedCompletionDate() {
        if (getVelocity() == 0) return null;
        
        int remainingTasks = totalTasks - doneTasks;
        double daysToComplete = remainingTasks / getVelocity();
        
        return LocalDateTime.now().plusDays((long) Math.ceil(daysToComplete));
    }
}

package com.autotrack.dto;

import com.autotrack.model.TaskStatus;

import java.time.LocalDateTime;

/**
 * DTO for task summary information used in sprint views.
 */
public class TaskSummaryDto {
    
    private Long taskId;
    private String featureCode;
    private String title;
    private TaskStatus status;
    private String assigneeName;
    private String assigneeAvatarUrl;
    private LocalDateTime updatedAt;
    private String milestone;
    
    // Constructors
    public TaskSummaryDto() {}
    
    public TaskSummaryDto(Long taskId, String featureCode, String title, TaskStatus status,
                         String assigneeName, String assigneeAvatarUrl, LocalDateTime updatedAt,
                         String milestone) {
        this.taskId = taskId;
        this.featureCode = featureCode;
        this.title = title;
        this.status = status;
        this.assigneeName = assigneeName;
        this.assigneeAvatarUrl = assigneeAvatarUrl;
        this.updatedAt = updatedAt;
        this.milestone = milestone;
    }
    
    // Getters and Setters
    public Long getTaskId() {
        return taskId;
    }
    
    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }
    
    public String getFeatureCode() {
        return featureCode;
    }
    
    public void setFeatureCode(String featureCode) {
        this.featureCode = featureCode;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public TaskStatus getStatus() {
        return status;
    }
    
    public void setStatus(TaskStatus status) {
        this.status = status;
    }
    
    public String getAssigneeName() {
        return assigneeName;
    }
    
    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
    }
    
    public String getAssigneeAvatarUrl() {
        return assigneeAvatarUrl;
    }
    
    public void setAssigneeAvatarUrl(String assigneeAvatarUrl) {
        this.assigneeAvatarUrl = assigneeAvatarUrl;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getMilestone() {
        return milestone;
    }
    
    public void setMilestone(String milestone) {
        this.milestone = milestone;
    }
}

package com.autotrack.dto;

import com.autotrack.model.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Data transfer object for Task.
 */
public class TaskDTO {
    
    private Long id;
    
    @NotBlank(message = "Feature code is required")
    private String featureCode;
    
    @NotBlank(message = "Task title is required")
    private String title;
    
    @NotNull(message = "Status is required")
    private TaskStatus status;
    
    private Long assigneeId;
    
    @NotNull(message = "Project is required")
    private Long projectId;
    
    private String milestone;
    
    private String tags;

    // Constructors
    public TaskDTO() {}

    public TaskDTO(Long id, String featureCode, String title, TaskStatus status, Long assigneeId, Long projectId, String milestone, String tags) {
        this.id = id;
        this.featureCode = featureCode;
        this.title = title;
        this.status = status;
        this.assigneeId = assigneeId;
        this.projectId = projectId;
        this.milestone = milestone;
        this.tags = tags;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFeatureCode() { return featureCode; }
    public void setFeatureCode(String featureCode) { this.featureCode = featureCode; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getMilestone() { return milestone; }
    public void setMilestone(String milestone) { this.milestone = milestone; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
}

package com.autotrack.dto;

import com.autotrack.model.BacklogStatus;
import com.autotrack.model.IssueType;
import com.autotrack.model.PriorityLevel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO for BacklogItem operations.
 * Provides data transfer object for REST API operations.
 */
public class BacklogItemDto {
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
    @Positive(message = "Story points must be positive")
    private Integer storyPoints = 1;
    
    @Positive(message = "Business value must be positive")
    private Integer businessValue = 1;
    
    @Positive(message = "Effort estimate must be positive")
    private Integer effortEstimate = 1;
    
    @NotNull(message = "Priority level is required")
    private PriorityLevel priorityLevel = PriorityLevel.MEDIUM;
    
    @NotNull(message = "Issue type is required")
    private IssueType issueType = IssueType.STORY;
    
    private BacklogStatus status = BacklogStatus.PRODUCT_BACKLOG;
    
    private String acceptanceCriteria;
    
    private String epicName;
    
    private String userStory;
    
    private Long assignedToId;
    
    private Long sprintId;
    
    private Long projectId;
    
    // Constructors
    public BacklogItemDto() {}
    
    public BacklogItemDto(String title, String description, Integer storyPoints, 
                         Integer businessValue, Integer effortEstimate, PriorityLevel priorityLevel) {
        this.title = title;
        this.description = description;
        this.storyPoints = storyPoints;
        this.businessValue = businessValue;
        this.effortEstimate = effortEstimate;
        this.priorityLevel = priorityLevel;
    }
    
    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Integer getStoryPoints() { return storyPoints; }
    public void setStoryPoints(Integer storyPoints) { this.storyPoints = storyPoints; }
    
    public Integer getBusinessValue() { return businessValue; }
    public void setBusinessValue(Integer businessValue) { this.businessValue = businessValue; }
    
    public Integer getEffortEstimate() { return effortEstimate; }
    public void setEffortEstimate(Integer effortEstimate) { this.effortEstimate = effortEstimate; }
    
    public PriorityLevel getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(PriorityLevel priorityLevel) { this.priorityLevel = priorityLevel; }
    
    public IssueType getIssueType() { return issueType; }
    public void setIssueType(IssueType issueType) { this.issueType = issueType; }
    
    public BacklogStatus getStatus() { return status; }
    public void setStatus(BacklogStatus status) { this.status = status; }
    
    public String getAcceptanceCriteria() { return acceptanceCriteria; }
    public void setAcceptanceCriteria(String acceptanceCriteria) { this.acceptanceCriteria = acceptanceCriteria; }
    
    public String getEpicName() { return epicName; }
    public void setEpicName(String epicName) { this.epicName = epicName; }
    
    public String getUserStory() { return userStory; }
    public void setUserStory(String userStory) { this.userStory = userStory; }
    
    public Long getAssignedToId() { return assignedToId; }
    public void setAssignedToId(Long assignedToId) { this.assignedToId = assignedToId; }
    
    public Long getSprintId() { return sprintId; }
    public void setSprintId(Long sprintId) { this.sprintId = sprintId; }
    
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
}
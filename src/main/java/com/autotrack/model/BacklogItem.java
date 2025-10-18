package com.autotrack.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a backlog item in the product backlog.
 * Supports prioritization, story points, and sprint assignment.
 */
@Entity
@Table(name = "backlog_items")
public class BacklogItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 500)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "priority_rank", nullable = false)
    private Integer priorityRank = 0;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "priority_level", nullable = false)
    private PriorityLevel priorityLevel = PriorityLevel.MEDIUM;
    
    @Column(name = "story_points")
    private Integer storyPoints = 0;
    
    @Column(name = "business_value")
    private Integer businessValue = 0;
    
    @Column(name = "effort_estimate")
    private Integer effortEstimate = 0;
    
    @Column(name = "acceptance_criteria", columnDefinition = "TEXT")
    private String acceptanceCriteria;
    
    @Column(length = 1000)
    private String labels;
    
    @Column(name = "epic_name")
    private String epicName;
    
    @Column(name = "user_story", columnDefinition = "TEXT")
    private String userStory;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false)
    private IssueType issueType = IssueType.STORY;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "backlog_status", nullable = false)
    private BacklogStatus backlogStatus = BacklogStatus.PRODUCT_BACKLOG;
    
    // Relationships
    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    @OneToOne
    @JoinColumn(name = "task_id")
    private Task task;
    
    @ManyToOne
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;
    
    @ManyToOne
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;
    
    @ManyToOne
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;
    
    // Timestamps
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "moved_to_sprint_at")
    private LocalDateTime movedToSprintAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Constructors
    public BacklogItem() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public BacklogItem(String title, String description, PriorityLevel priorityLevel, 
                      Integer storyPoints, Project project, User createdBy) {
        this();
        this.title = title;
        this.description = description;
        this.priorityLevel = priorityLevel;
        this.storyPoints = storyPoints;
        this.project = project;
        this.createdBy = createdBy;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getPriorityRank() { return priorityRank; }
    public void setPriorityRank(Integer priorityRank) { this.priorityRank = priorityRank; }

    public PriorityLevel getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(PriorityLevel priorityLevel) { this.priorityLevel = priorityLevel; }

    public Integer getStoryPoints() { return storyPoints; }
    public void setStoryPoints(Integer storyPoints) { this.storyPoints = storyPoints; }

    public Integer getBusinessValue() { return businessValue; }
    public void setBusinessValue(Integer businessValue) { this.businessValue = businessValue; }

    public Integer getEffortEstimate() { return effortEstimate; }
    public void setEffortEstimate(Integer effortEstimate) { this.effortEstimate = effortEstimate; }

    public String getAcceptanceCriteria() { return acceptanceCriteria; }
    public void setAcceptanceCriteria(String acceptanceCriteria) { this.acceptanceCriteria = acceptanceCriteria; }

    public String getLabels() { return labels; }
    public void setLabels(String labels) { this.labels = labels; }

    public String getEpicName() { return epicName; }
    public void setEpicName(String epicName) { this.epicName = epicName; }

    public String getUserStory() { return userStory; }
    public void setUserStory(String userStory) { this.userStory = userStory; }

    public IssueType getIssueType() { return issueType; }
    public void setIssueType(IssueType issueType) { this.issueType = issueType; }

    public BacklogStatus getBacklogStatus() { return backlogStatus; }
    public void setBacklogStatus(BacklogStatus backlogStatus) { this.backlogStatus = backlogStatus; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

    public Sprint getSprint() { return sprint; }
    public void setSprint(Sprint sprint) { this.sprint = sprint; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public User getAssignedTo() { return assignedTo; }
    public void setAssignedTo(User assignedTo) { this.assignedTo = assignedTo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getMovedToSprintAt() { return movedToSprintAt; }
    public void setMovedToSprintAt(LocalDateTime movedToSprintAt) { this.movedToSprintAt = movedToSprintAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    // Utility methods
    public boolean isInSprint() {
        return sprint != null && (backlogStatus == BacklogStatus.SPRINT_BACKLOG || backlogStatus == BacklogStatus.IN_PROGRESS);
    }

    public boolean isCompleted() {
        return backlogStatus == BacklogStatus.COMPLETED;
    }

    public List<String> getLabelsList() {
        if (labels == null || labels.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return List.of(labels.split(","));
    }

    public void setLabelsList(List<String> labelsList) {
        if (labelsList == null || labelsList.isEmpty()) {
            this.labels = null;
        } else {
            this.labels = String.join(",", labelsList);
        }
    }

    // Calculate priority score for sorting
    public int getPriorityScore() {
        int levelScore = priorityLevel.ordinal() * 1000;
        return levelScore + priorityRank;
    }

    // Builder pattern
    public static BacklogItemBuilder builder() {
        return new BacklogItemBuilder();
    }

    public static class BacklogItemBuilder {
        private String title;
        private String description;
        private PriorityLevel priorityLevel = PriorityLevel.MEDIUM;
        private IssueType issueType = IssueType.STORY;
        private Integer storyPoints = 0;
        private Integer businessValue = 0;
        private Integer effortEstimate = 0;
        private String acceptanceCriteria;
        private String labels;
        private String epicName;
        private String userStory;
        private Project project;
        private User createdBy;
        private User assignedTo;

        public BacklogItemBuilder title(String title) {
            this.title = title;
            return this;
        }

        public BacklogItemBuilder description(String description) {
            this.description = description;
            return this;
        }

        public BacklogItemBuilder priorityLevel(PriorityLevel priorityLevel) {
            this.priorityLevel = priorityLevel;
            return this;
        }

        public BacklogItemBuilder issueType(IssueType issueType) {
            this.issueType = issueType;
            return this;
        }

        public BacklogItemBuilder storyPoints(Integer storyPoints) {
            this.storyPoints = storyPoints;
            return this;
        }

        public BacklogItemBuilder businessValue(Integer businessValue) {
            this.businessValue = businessValue;
            return this;
        }

        public BacklogItemBuilder effortEstimate(Integer effortEstimate) {
            this.effortEstimate = effortEstimate;
            return this;
        }

        public BacklogItemBuilder acceptanceCriteria(String acceptanceCriteria) {
            this.acceptanceCriteria = acceptanceCriteria;
            return this;
        }

        public BacklogItemBuilder labels(String labels) {
            this.labels = labels;
            return this;
        }

        public BacklogItemBuilder epicName(String epicName) {
            this.epicName = epicName;
            return this;
        }

        public BacklogItemBuilder userStory(String userStory) {
            this.userStory = userStory;
            return this;
        }

        public BacklogItemBuilder project(Project project) {
            this.project = project;
            return this;
        }

        public BacklogItemBuilder createdBy(User createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public BacklogItemBuilder assignedTo(User assignedTo) {
            this.assignedTo = assignedTo;
            return this;
        }

        public BacklogItem build() {
            BacklogItem item = new BacklogItem();
            item.title = this.title;
            item.description = this.description;
            item.priorityLevel = this.priorityLevel;
            item.issueType = this.issueType;
            item.storyPoints = this.storyPoints;
            item.businessValue = this.businessValue;
            item.effortEstimate = this.effortEstimate;
            item.acceptanceCriteria = this.acceptanceCriteria;
            item.labels = this.labels;
            item.epicName = this.epicName;
            item.userStory = this.userStory;
            item.project = this.project;
            item.createdBy = this.createdBy;
            item.assignedTo = this.assignedTo;
            return item;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
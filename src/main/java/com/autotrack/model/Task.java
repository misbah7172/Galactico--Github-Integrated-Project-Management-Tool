package com.autotrack.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Entity representing a task.
 */
@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "feature_code", nullable = false)
    private String featureCode;
    
    @Column(nullable = false)
    private String title;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;
    
    @ManyToOne
    @JoinColumn(name = "assignee_id")
    private User assignee;
    
    @Column(name = "tags", columnDefinition = "TEXT")
    private String tagsString;
    
    @Column
    private String milestone;
    
    @Column(name = "github_issue_url")
    private String gitHubIssueUrl;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Decline-related fields
    @ManyToOne
    @JoinColumn(name = "declined_by_id")
    private User declinedBy;
    
    @Column(name = "declined_at")
    private LocalDateTime declinedAt;
    
    @Column(name = "decline_reason", columnDefinition = "TEXT")
    private String declineReason;
    
    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    // Sprint relationship
    @ManyToOne
    @JoinColumn(name = "sprint_id")
    
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Commit> commits = new ArrayList<>();

    // Constructors
    public Task() {}

    public Task(Long id, String featureCode, String title, TaskStatus status, User assignee, String tagsString,
                String milestone, String gitHubIssueUrl, LocalDateTime createdAt, LocalDateTime updatedAt,
                Project project) {
        this.id = id;
        this.featureCode = featureCode;
        this.title = title;
        this.status = status;
        this.assignee = assignee;
        this.tagsString = tagsString;
        this.milestone = milestone;
        this.gitHubIssueUrl = gitHubIssueUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.project = project;
        this.commits = commits != null ? commits : new ArrayList<>();
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

    public User getAssignee() { return assignee; }
    public void setAssignee(User assignee) { this.assignee = assignee; }

    public String getTagsString() { return tagsString; }
    public void setTagsString(String tagsString) { this.tagsString = tagsString; }

    public String getMilestone() { return milestone; }
    public void setMilestone(String milestone) { this.milestone = milestone; }

    public String getGitHubIssueUrl() { return gitHubIssueUrl; }
    public void setGitHubIssueUrl(String gitHubIssueUrl) { this.gitHubIssueUrl = gitHubIssueUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    // Decline-related getters and setters
    public User getDeclinedBy() { return declinedBy; }
    public void setDeclinedBy(User declinedBy) { this.declinedBy = declinedBy; }

    public LocalDateTime getDeclinedAt() { return declinedAt; }
    public void setDeclinedAt(LocalDateTime declinedAt) { this.declinedAt = declinedAt; }

    public String getDeclineReason() { return declineReason; }
    public void setDeclineReason(String declineReason) { this.declineReason = declineReason; }

    // Utility method to check if task was declined
    public boolean isDeclined() {
        return declinedBy != null && declinedAt != null;
    }

    // Builder pattern
    public static TaskBuilder builder() {
        return new TaskBuilder();
    }

    public static class TaskBuilder {
        private Long id;
        private String featureCode;
        private String title;
        private TaskStatus status;
        private User assignee;
        private String tagsString;
        private String milestone;
        private String gitHubIssueUrl;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Project project;
        private List<Commit> commits = new ArrayList<>();

        public TaskBuilder id(Long id) { this.id = id; return this; }
        public TaskBuilder featureCode(String featureCode) { this.featureCode = featureCode; return this; }
        public TaskBuilder title(String title) { this.title = title; return this; }
        public TaskBuilder status(TaskStatus status) { this.status = status; return this; }
        public TaskBuilder assignee(User assignee) { this.assignee = assignee; return this; }
        public TaskBuilder tagsString(String tagsString) { this.tagsString = tagsString; return this; }
        public TaskBuilder milestone(String milestone) { this.milestone = milestone; return this; }
        public TaskBuilder gitHubIssueUrl(String gitHubIssueUrl) { this.gitHubIssueUrl = gitHubIssueUrl; return this; }
        public TaskBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public TaskBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public TaskBuilder project(Project project) { this.project = project; return this; }
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    @Transient
    public List<String> getTags() {
        if (tagsString == null || tagsString.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(tagsString.split(","));
    }
    
    public void setTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            this.tagsString = "";
        } else {
            this.tagsString = String.join(",", tags);
        }
    }
}

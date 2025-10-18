package com.autotrack.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entity representing the history/timeline of task changes.
 * Provides Jira-like activity tracking for tasks.
 */
@Entity
@Table(name = "task_history")
public class TaskHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "action_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private HistoryActionType actionType;
    
    @Column(name = "field_name")
    private String fieldName;
    
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;
    
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    // Constructors
    public TaskHistory() {
        this.timestamp = LocalDateTime.now();
    }
    
    public TaskHistory(Task task, User user, HistoryActionType actionType, String description) {
        this();
        this.task = task;
        this.user = user;
        this.actionType = actionType;
        this.description = description;
        this.project = task.getProject();
        this.sprint = task.getSprint();
    }
    
    public TaskHistory(Task task, User user, HistoryActionType actionType, 
                      String fieldName, String oldValue, String newValue) {
        this();
        this.task = task;
        this.user = user;
        this.actionType = actionType;
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.project = task.getProject();
        this.sprint = task.getSprint();
        
        // Generate description based on field change
        generateDescription();
    }
    
    private void generateDescription() {
        if (fieldName != null) {
            switch (actionType) {
                case FIELD_UPDATED -> this.description = String.format("Updated %s from '%s' to '%s'", 
                    fieldName, oldValue != null ? oldValue : "empty", 
                    newValue != null ? newValue : "empty");
                case STATUS_CHANGED -> this.description = String.format("Changed status from %s to %s", 
                    oldValue, newValue);
                case ASSIGNED -> this.description = String.format("Assigned to %s", newValue);
                case UNASSIGNED -> this.description = "Unassigned from task";
                case PRIORITY_CHANGED -> this.description = String.format("Changed priority from %s to %s", 
                    oldValue, newValue);
                default -> this.description = String.format("Updated %s", fieldName);
            }
        }
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Task getTask() {
        return task;
    }
    
    public void setTask(Task task) {
        this.task = task;
        if (task != null) {
            this.project = task.getProject();
            this.sprint = task.getSprint();
        }
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public HistoryActionType getActionType() {
        return actionType;
    }
    
    public void setActionType(HistoryActionType actionType) {
        this.actionType = actionType;
    }
    
    public String getFieldName() {
        return fieldName;
    }
    
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }
    
    public String getOldValue() {
        return oldValue;
    }
    
    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }
    
    public String getNewValue() {
        return newValue;
    }
    
    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Sprint getSprint() {
        return sprint;
    }
    
    public void setSprint(Sprint sprint) {
        this.sprint = sprint;
    }
    
    public Project getProject() {
        return project;
    }
    
    public void setProject(Project project) {
        this.project = project;
    }
}
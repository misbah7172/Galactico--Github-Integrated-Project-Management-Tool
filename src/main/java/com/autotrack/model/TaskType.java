package com.autotrack.model;

/**
 * Enumeration representing different types of tasks.
 * Used to categorize work items in the project management system.
 */
public enum TaskType {
    /**
     * User story - A feature from the user's perspective
     */
    STORY("Story", "story", "#007bff"),
    
    /**
     * Bug fix - Addressing defects in the system
     */
    BUG("Bug", "bug", "#dc3545"),
    
    /**
     * Epic - Large user story that can be broken down into smaller stories
     */
    EPIC("Epic", "epic", "#6f42c1"),
    
    /**
     * Task - General work item
     */
    TASK("Task", "task", "#28a745"),
    
    /**
     * Subtask - Part of a larger task or story
     */
    SUBTASK("Subtask", "subtask", "#6c757d");
    
    private final String displayName;
    private final String shortName;
    private final String colorCode;
    
    TaskType(String displayName, String shortName, String colorCode) {
        this.displayName = displayName;
        this.shortName = shortName;
        this.colorCode = colorCode;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getShortName() {
        return shortName;
    }
    
    public String getColorCode() {
        return colorCode;
    }
    
    /**
     * Get CSS class name for styling
     */
    public String getCssClass() {
        return "task-type-" + shortName;
    }
    
    /**
     * Get Bootstrap badge class
     */
    public String getBadgeClass() {
        switch (this) {
            case STORY: return "badge-primary";
            case BUG: return "badge-danger";
            case EPIC: return "badge-purple";
            case TASK: return "badge-success";
            case SUBTASK: return "badge-secondary";
            default: return "badge-secondary";
        }
    }
    
    /**
     * Get icon class for UI display
     */
    public String getIconClass() {
        switch (this) {
            case STORY: return "fas fa-bookmark";
            case BUG: return "fas fa-bug";
            case EPIC: return "fas fa-flag";
            case TASK: return "fas fa-tasks";
            case SUBTASK: return "fas fa-check";
            default: return "fas fa-circle";
        }
    }
}
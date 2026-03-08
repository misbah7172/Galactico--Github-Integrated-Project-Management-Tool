package com.autotrack.model;

/**
 * Enumeration representing the status of backlog items.
 * Tracks the lifecycle of backlog items from creation to completion.
 */
public enum BacklogStatus {
    /**
     * Item is in the product backlog awaiting prioritization
     */
    PRODUCT_BACKLOG("Product Backlog", "backlog", "#6c757d"),
    
    /**
     * Item has been moved to a sprint backlog
     */
    SPRINT_BACKLOG("Sprint Backlog", "sprint", "#007bff"),
    
    /**
     * Item is actively being worked on
     */
    IN_PROGRESS("In Progress", "progress", "#ffc107"),
    
    /**
     * Item has been completed
     */
    COMPLETED("Completed", "done", "#28a745"),
    
    /**
     * Item has been archived or cancelled
     */
    ARCHIVED("Archived", "archived", "#868e96");
    
    private final String displayName;
    private final String shortName;
    private final String colorCode;
    
    BacklogStatus(String displayName, String shortName, String colorCode) {
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
        return "status-" + shortName;
    }
    
    /**
     * Get Bootstrap badge class
     */
    public String getBadgeClass() {
        switch (this) {
            case PRODUCT_BACKLOG: return "badge-secondary";
            case SPRINT_BACKLOG: return "badge-primary";
            case IN_PROGRESS: return "badge-warning";
            case COMPLETED: return "badge-success";
            case ARCHIVED: return "badge-dark";
            default: return "badge-secondary";
        }
    }
    
    /**
     * Check if status indicates active work
     */
    public boolean isActive() {
        return this == IN_PROGRESS || this == SPRINT_BACKLOG;
    }
    
    /**
     * Check if status indicates completion
     */
    public boolean isCompleted() {
        return this == COMPLETED;
    }
}
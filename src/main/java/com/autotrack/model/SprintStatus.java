package com.autotrack.model;

/**
 * Enumeration representing the status of a sprint.
 * Similar to Jira sprint statuses.
 */
public enum SprintStatus {
    /**
     * Sprint is planned but not yet started
     */
    UPCOMING("Upcoming"),
    
    /**
     * Sprint is currently in progress
     */
    ACTIVE("Active"),
    
    /**
     * Sprint has been completed
     */
    COMPLETED("Completed"),
    
    /**
     * Sprint has been cancelled/closed without completion
     */
    CANCELLED("Cancelled");
    
    private final String displayName;
    
    SprintStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}

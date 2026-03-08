package com.autotrack.model;

/**
 * Enumeration representing priority levels for backlog items and tasks.
 * Used for prioritization and sorting in backlogs and sprints.
 */
public enum PriorityLevel {
    /**
     * Low priority - nice to have features
     */
    LOW("Low", 1, "#28a745"),
    
    /**
     * Medium priority - standard features
     */
    MEDIUM("Medium", 2, "#ffc107"),
    
    /**
     * High priority - important features
     */
    HIGH("High", 3, "#fd7e14"),
    
    /**
     * Critical priority - urgent fixes and essential features
     */
    CRITICAL("Critical", 4, "#dc3545");
    
    private final String displayName;
    private final int priorityValue;
    private final String colorCode;
    
    PriorityLevel(String displayName, int priorityValue, String colorCode) {
        this.displayName = displayName;
        this.priorityValue = priorityValue;
        this.colorCode = colorCode;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getPriorityValue() {
        return priorityValue;
    }
    
    public String getColorCode() {
        return colorCode;
    }
    
    /**
     * Get CSS class name for styling
     */
    public String getCssClass() {
        return "priority-" + name().toLowerCase();
    }
    
    /**
     * Get Bootstrap badge class
     */
    public String getBadgeClass() {
        switch (this) {
            case LOW: return "badge-success";
            case MEDIUM: return "badge-warning";
            case HIGH: return "badge-primary";
            case CRITICAL: return "badge-danger";
            default: return "badge-secondary";
        }
    }
}
package com.autotrack.model;

/**
 * Enumeration representing different types of sprints.
 */
public enum SprintType {
    /**
     * Regular development sprint
     */
    REGULAR("Regular", "#007bff"),
    
    /**
     * Hotfix sprint for critical issues
     */
    HOTFIX("Hotfix", "#dc3545"),
    
    /**
     * Release preparation sprint
     */
    RELEASE("Release", "#28a745"),
    
    /**
     * Planning and research sprint
     */
    PLANNING("Planning", "#6c757d");
    
    private final String displayName;
    private final String colorCode;
    
    SprintType(String displayName, String colorCode) {
        this.displayName = displayName;
        this.colorCode = colorCode;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getColorCode() {
        return colorCode;
    }
    
    /**
     * Get Bootstrap badge class for UI display
     */
    public String getBadgeClass() {
        switch (this) {
            case REGULAR: return "badge-primary";
            case HOTFIX: return "badge-danger";
            case RELEASE: return "badge-success";
            case PLANNING: return "badge-secondary";
            default: return "badge-secondary";
        }
    }
    
    /**
     * Get icon class for UI display
     */
    public String getIconClass() {
        switch (this) {
            case REGULAR: return "fas fa-sync";
            case HOTFIX: return "fas fa-fire";
            case RELEASE: return "fas fa-rocket";
            case PLANNING: return "fas fa-clipboard-list";
            default: return "fas fa-calendar";
        }
    }
}
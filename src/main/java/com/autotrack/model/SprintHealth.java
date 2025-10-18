package com.autotrack.model;

/**
 * Enumeration representing sprint health status.
 * Used to indicate how well a sprint is progressing.
 */
public enum SprintHealth {
    /**
     * Sprint is progressing well and on schedule
     */
    ON_TRACK("On Track", "#28a745"),
    
    /**
     * Sprint is showing some risk indicators but recoverable
     */
    AT_RISK("At Risk", "#ffc107"),
    
    /**
     * Sprint is significantly behind and unlikely to meet goals
     */
    OFF_TRACK("Off Track", "#dc3545");
    
    private final String displayName;
    private final String colorCode;
    
    SprintHealth(String displayName, String colorCode) {
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
            case ON_TRACK: return "badge-success";
            case AT_RISK: return "badge-warning";
            case OFF_TRACK: return "badge-danger";
            default: return "badge-secondary";
        }
    }
    
    /**
     * Get CSS class for custom styling
     */
    public String getCssClass() {
        return "sprint-health-" + name().toLowerCase().replace("_", "-");
    }
}
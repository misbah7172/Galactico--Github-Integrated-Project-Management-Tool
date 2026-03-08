package com.autotrack.model;

/**
 * Enumeration representing different types of issues in the backlog.
 * Similar to Jira issue types.
 */
public enum IssueType {
    /**
     * A story represents a feature or functionality from user's perspective.
     */
    STORY,
    
    /**
     * A task is a specific piece of work to be done.
     */
    TASK,
    
    /**
     * A bug represents a defect or issue that needs to be fixed.
     */
    BUG,
    
    /**
     * An epic is a large body of work that can be broken down into smaller stories.
     */
    EPIC
}
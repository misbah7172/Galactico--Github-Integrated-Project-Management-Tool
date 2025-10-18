package com.autotrack.model;

/**
 * Enumeration for different types of task history actions.
 * Used to categorize activities in the task timeline.
 */
public enum HistoryActionType {
    /**
     * Task was created
     */
    TASK_CREATED("Task Created"),
    
    /**
     * Task was updated (generic field change)
     */
    FIELD_UPDATED("Field Updated"),
    
    /**
     * Task status was changed
     */
    STATUS_CHANGED("Status Changed"),
    
    /**
     * Task was assigned to a user
     */
    ASSIGNED("Assigned"),
    
    /**
     * Task was unassigned from a user
     */
    UNASSIGNED("Unassigned"),
    
    /**
     * Task priority was changed
     */
    PRIORITY_CHANGED("Priority Changed"),
    
    /**
     * Task was moved to a different sprint
     */
    SPRINT_CHANGED("Sprint Changed"),
    
    /**
     * Task was moved to backlog
     */
    MOVED_TO_BACKLOG("Moved to Backlog"),
    
    /**
     * Comment was added to task
     */
    COMMENT_ADDED("Comment Added"),
    
    /**
     * File was attached to task
     */
    ATTACHMENT_ADDED("Attachment Added"),
    
    /**
     * Time was logged for task
     */
    TIME_LOGGED("Time Logged"),
    
    /**
     * Task was blocked
     */
    TASK_BLOCKED("Task Blocked"),
    
    /**
     * Task was unblocked
     */
    TASK_UNBLOCKED("Task Unblocked"),
    
    /**
     * Task was deleted
     */
    TASK_DELETED("Task Deleted"),
    
    /**
     * Task was archived
     */
    TASK_ARCHIVED("Task Archived"),
    
    /**
     * Task was restored from archive
     */
    TASK_RESTORED("Task Restored"),
    
    /**
     * Task estimation was updated
     */
    ESTIMATION_UPDATED("Estimation Updated"),
    
    /**
     * Task due date was changed
     */
    DUE_DATE_CHANGED("Due Date Changed"),
    
    /**
     * Task labels/tags were updated
     */
    LABELS_UPDATED("Labels Updated"),
    
    /**
     * Task description was updated
     */
    DESCRIPTION_UPDATED("Description Updated"),
    
    /**
     * Task acceptance criteria was updated
     */
    ACCEPTANCE_CRITERIA_UPDATED("Acceptance Criteria Updated"),
    
    /**
     * Task was linked to another task
     */
    TASK_LINKED("Task Linked"),
    
    /**
     * Task link was removed
     */
    TASK_UNLINKED("Task Unlinked");
    
    private final String displayName;
    
    HistoryActionType(String displayName) {
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
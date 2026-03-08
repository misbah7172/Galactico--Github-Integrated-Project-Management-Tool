package com.autotrack.service;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for parsing commit messages to extract task information.
 */
@Service
public class CommitParserService {
    
    // Enhanced regex patterns for commit message parsing with sprint and backlog support
    private static final Pattern FEATURE_PATTERN = Pattern.compile("(?:Feature|F)(\\d+)\\s*:", Pattern.CASE_INSENSITIVE);
    private static final Pattern TASK_PATTERN = Pattern.compile(":\\s*([^->]+)(?=->|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ASSIGNEE_PATTERN = Pattern.compile("->\\s*([^->]+?)(?=\\s*->|$)", Pattern.CASE_INSENSITIVE);
    
    // Sprint assignment patterns
    private static final Pattern SPRINT_PATTERN = Pattern.compile("->\\s*sprint(\\d+|current|next)\\s*(?=->|$)", Pattern.CASE_INSENSITIVE);
    
    // Backlog priority patterns
    private static final Pattern BACKLOG_PATTERN = Pattern.compile("->\\s*backlog-(low|medium|high|critical)\\s*(?=->|$)", Pattern.CASE_INSENSITIVE);
    
    // Story points estimation pattern
    private static final Pattern STORY_POINTS_PATTERN = Pattern.compile("->\\s*sp:(\\d+)\\s*(?=->|$)", Pattern.CASE_INSENSITIVE);
    
    // Time estimation pattern
    private static final Pattern TIME_ESTIMATE_PATTERN = Pattern.compile("->\\s*estimate:(\\d+[hdwm])\\s*(?=->|$)", Pattern.CASE_INSENSITIVE);
    
    // Task type pattern
    private static final Pattern TASK_TYPE_PATTERN = Pattern.compile("->\\s*(story|bug|epic|task|subtask)\\s*(?=->|$)", Pattern.CASE_INSENSITIVE);
    
    // Enhanced status pattern with more states
    private static final Pattern STATUS_PATTERN = Pattern.compile("->\\s*(todo|backlog|in-progress|review|done)\\s*$", Pattern.CASE_INSENSITIVE);
    
    // Tag pattern (unchanged)
    private static final Pattern TAG_PATTERN = Pattern.compile("#(\\w+)");
    
    /**
     * Parse a commit message to extract task information.
     * 
     * @param commitMessage The commit message to parse
     * @return CommitInfo object containing extracted information
     */
    public Optional<CommitInfo> parseCommitMessage(String commitMessage) {
        if (commitMessage == null || commitMessage.trim().isEmpty()) {
            return Optional.empty();
        }
        
        // Extract feature code
        Matcher featureMatcher = FEATURE_PATTERN.matcher(commitMessage);
        if (!featureMatcher.find()) {
            return Optional.empty();
        }
        
        CommitInfo info = new CommitInfo();
        info.setFeatureCode("Feature" + featureMatcher.group(1));
        
        // Extract task title
        Matcher taskMatcher = TASK_PATTERN.matcher(commitMessage);
        if (taskMatcher.find()) {
            info.setTaskTitle(taskMatcher.group(1).trim());
        } else {
            return Optional.empty();
        }
        
        // Extract assignee nickname
        Matcher assigneeMatcher = ASSIGNEE_PATTERN.matcher(commitMessage);
        if (assigneeMatcher.find()) {
            info.setAssigneeNickname(assigneeMatcher.group(1).trim());
        }
        
        // Extract sprint assignment
        Matcher sprintMatcher = SPRINT_PATTERN.matcher(commitMessage);
        if (sprintMatcher.find()) {
            info.setSprintId(sprintMatcher.group(1).trim());
        }

        // Extract backlog priority
        Matcher backlogMatcher = BACKLOG_PATTERN.matcher(commitMessage);
        if (backlogMatcher.find()) {
            info.setBacklogPriority(backlogMatcher.group(1).toUpperCase());
        }

        // Extract story points
        Matcher storyPointsMatcher = STORY_POINTS_PATTERN.matcher(commitMessage);
        if (storyPointsMatcher.find()) {
            try {
                info.setStoryPoints(Integer.parseInt(storyPointsMatcher.group(1)));
            } catch (NumberFormatException e) {
                // Ignore invalid story points
            }
        }

        // Extract time estimate
        Matcher timeEstimateMatcher = TIME_ESTIMATE_PATTERN.matcher(commitMessage);
        if (timeEstimateMatcher.find()) {
            info.setTimeEstimate(timeEstimateMatcher.group(1).trim());
        }

        // Extract task type
        Matcher taskTypeMatcher = TASK_TYPE_PATTERN.matcher(commitMessage);
        if (taskTypeMatcher.find()) {
            info.setTaskType(taskTypeMatcher.group(1).toUpperCase());
        }

        // Extract status (enhanced with more states)
        Matcher statusMatcher = STATUS_PATTERN.matcher(commitMessage);
        if (statusMatcher.find()) {
            String status = statusMatcher.group(1).toLowerCase().replace("-", "_");
            switch (status) {
                case "todo":
                    info.setStatus("TODO");
                    break;
                case "backlog":
                    info.setStatus("BACKLOG");
                    break;
                case "in_progress":
                    info.setStatus("IN_PROGRESS");
                    break;
                case "review":
                    info.setStatus("REVIEW");
                    break;
                case "done":
                    info.setStatus("DONE");
                    break;
            }
        } else {
            // Default status logic
            if (info.getBacklogPriority() != null) {
                info.setStatus("BACKLOG");
            } else if (info.getAssigneeNickname() != null) {
                info.setStatus("IN_PROGRESS");
            } else {
                info.setStatus("TODO");
            }
        }
        
        // Extract tags
        Matcher tagMatcher = TAG_PATTERN.matcher(commitMessage);
        StringBuilder tags = new StringBuilder();
        while (tagMatcher.find()) {
            if (tags.length() > 0) {
                tags.append(",");
            }
            tags.append(tagMatcher.group(1));
        }
        
        if (tags.length() > 0) {
            info.setTags(tags.toString());
        }
        
        return Optional.of(info);
    }
    
    /**
     * Enhanced class to hold parsed commit information including sprint and backlog data.
     */
    public static class CommitInfo {
        private String featureCode;
        private String taskTitle;
        private String assigneeNickname;
        private String status;
        private String tags;
        
        // Enhanced fields for sprint and backlog management
        private String sprintId;
        private String backlogPriority;
        private Integer storyPoints;
        private String timeEstimate;
        private String taskType;

        // Constructors
        public CommitInfo() {}

        public CommitInfo(String featureCode, String taskTitle, String assigneeNickname, String status, String tags) {
            this.featureCode = featureCode;
            this.taskTitle = taskTitle;
            this.assigneeNickname = assigneeNickname;
            this.status = status;
            this.tags = tags;
        }

        // Getters and Setters
        public String getFeatureCode() { return featureCode; }
        public void setFeatureCode(String featureCode) { this.featureCode = featureCode; }

        public String getTaskTitle() { return taskTitle; }
        public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }

        public String getAssigneeNickname() { return assigneeNickname; }
        public void setAssigneeNickname(String assigneeNickname) { this.assigneeNickname = assigneeNickname; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getTags() { return tags; }
        public void setTags(String tags) { this.tags = tags; }

        // Enhanced fields getters and setters
        public String getSprintId() { return sprintId; }
        public void setSprintId(String sprintId) { this.sprintId = sprintId; }

        public String getBacklogPriority() { return backlogPriority; }
        public void setBacklogPriority(String backlogPriority) { this.backlogPriority = backlogPriority; }

        public Integer getStoryPoints() { return storyPoints; }
        public void setStoryPoints(Integer storyPoints) { this.storyPoints = storyPoints; }

        public String getTimeEstimate() { return timeEstimate; }
        public void setTimeEstimate(String timeEstimate) { this.timeEstimate = timeEstimate; }

        public String getTaskType() { return taskType; }
        public void setTaskType(String taskType) { this.taskType = taskType; }
    }
}

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
    
    // Regex patterns for commit message parsing
    private static final Pattern FEATURE_PATTERN = Pattern.compile("(?:Feature|F)(\\d+)\\s*:", Pattern.CASE_INSENSITIVE);
    private static final Pattern TASK_PATTERN = Pattern.compile(":\\s*([^->]+)(?=->|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ASSIGNEE_PATTERN = Pattern.compile("->\\s*([^->]+)(?=->|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern STATUS_PATTERN = Pattern.compile("->\\s*(todo|done)\\s*$", Pattern.CASE_INSENSITIVE);
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
        
        // Extract status
        Matcher statusMatcher = STATUS_PATTERN.matcher(commitMessage);
        if (statusMatcher.find()) {
            String status = statusMatcher.group(1).toLowerCase();
            if ("todo".equals(status)) {
                info.setStatus("TODO");
            } else if ("done".equals(status)) {
                info.setStatus("DONE");
            }
        } else {
            // Default status when assignee is present but no explicit status
            if (info.getAssigneeNickname() != null) {
                info.setStatus("IN_PROGRESS");
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
     * Class to hold parsed commit information.
     */
    public static class CommitInfo {
        private String featureCode;
        private String taskTitle;
        private String assigneeNickname;
        private String status;
        private String tags;

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
    }
}

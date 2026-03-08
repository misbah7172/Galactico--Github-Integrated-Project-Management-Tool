package com.autotrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for GitHub commit webhook data.
 * Used for processing GitHub webhook payloads related to commits.
 */
public class CommitWebhookDTO {

    @NotBlank(message = "Repository name is required")
    private String repositoryName;

    @NotBlank(message = "Branch name is required")
    private String branchName;

    @NotNull(message = "Commits list cannot be null")
    private List<CommitInfo> commits;

    @NotBlank(message = "Repository URL is required")
    private String repositoryUrl;

    private String pusherName;
    private String pusherEmail;
    private LocalDateTime timestamp;

    // Additional fields for commit review functionality
    private String projectId;
    private String username;
    private String commitMessage;
    private String branch;
    private String taskId;
    private LocalDateTime commitTime;
    private String commitUrl;
    private String commitSha;

    // Default constructor
    public CommitWebhookDTO() {
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public List<CommitInfo> getCommits() {
        return commits;
    }

    public void setCommits(List<CommitInfo> commits) {
        this.commits = commits;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getPusherName() {
        return pusherName;
    }

    public void setPusherName(String pusherName) {
        this.pusherName = pusherName;
    }

    public String getPusherEmail() {
        return pusherEmail;
    }

    public void setPusherEmail(String pusherEmail) {
        this.pusherEmail = pusherEmail;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public LocalDateTime getCommitTime() {
        return commitTime;
    }

    public void setCommitTime(LocalDateTime commitTime) {
        this.commitTime = commitTime;
    }

    public String getCommitUrl() {
        return commitUrl;
    }

    public void setCommitUrl(String commitUrl) {
        this.commitUrl = commitUrl;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public void setCommitSha(String commitSha) {
        this.commitSha = commitSha;
    }

    /**
     * Nested class for commit information
     */
    public static class CommitInfo {
        @NotBlank(message = "Commit ID is required")
        private String id;

        @NotBlank(message = "Commit message is required")
        private String message;

        private String authorName;
        private String authorEmail;
        private LocalDateTime timestamp;
        private List<String> addedFiles;
        private List<String> modifiedFiles;
        private List<String> removedFiles;

        // Default constructor
        public CommitInfo() {}

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getAuthorName() {
            return authorName;
        }

        public void setAuthorName(String authorName) {
            this.authorName = authorName;
        }

        public String getAuthorEmail() {
            return authorEmail;
        }

        public void setAuthorEmail(String authorEmail) {
            this.authorEmail = authorEmail;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }

        public List<String> getAddedFiles() {
            return addedFiles;
        }

        public void setAddedFiles(List<String> addedFiles) {
            this.addedFiles = addedFiles;
        }

        public List<String> getModifiedFiles() {
            return modifiedFiles;
        }

        public void setModifiedFiles(List<String> modifiedFiles) {
            this.modifiedFiles = modifiedFiles;
        }

        public List<String> getRemovedFiles() {
            return removedFiles;
        }

        public void setRemovedFiles(List<String> removedFiles) {
            this.removedFiles = removedFiles;
        }
    }
}
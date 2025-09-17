package com.autotrack.dto;

import java.time.LocalDateTime;

/**
 * DTO for receiving commit data from VS Code extension.
 */
public class CommitWebhookDTO {
    
    private String username;
    private String commitMessage;
    private String branch;
    private String taskId;
    private LocalDateTime commitTime;
    private String commitUrl;
    private String commitSha;
    private String projectId;

    // Constructors
    public CommitWebhookDTO() {}

    public CommitWebhookDTO(String username, String commitMessage, String branch, 
                           String taskId, LocalDateTime commitTime, String commitUrl) {
        this.username = username;
        this.commitMessage = commitMessage;
        this.branch = branch;
        this.taskId = taskId;
        this.commitTime = commitTime;
        this.commitUrl = commitUrl;
    }

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getCommitMessage() { return commitMessage; }
    public void setCommitMessage(String commitMessage) { this.commitMessage = commitMessage; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public LocalDateTime getCommitTime() { return commitTime; }
    public void setCommitTime(LocalDateTime commitTime) { this.commitTime = commitTime; }

    public String getCommitUrl() { return commitUrl; }
    public void setCommitUrl(String commitUrl) { this.commitUrl = commitUrl; }

    public String getCommitSha() { return commitSha; }
    public void setCommitSha(String commitSha) { this.commitSha = commitSha; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
}

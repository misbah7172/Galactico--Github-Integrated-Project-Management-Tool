package com.autotrack.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing an approved commit that has been merged to main.
 */
@Entity
@Table(name = "approved_commits")
public class ApprovedCommit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String username;
    
    @Column(nullable = false, length = 1000)
    private String commitMessage;
    
    @Column(nullable = false)
    private String originalBranch;
    
    @Column(name = "task_id")
    private String taskId;
    
    @Column(name = "commit_time", nullable = false)
    private LocalDateTime commitTime;
    
    @Column(name = "merge_time", nullable = false)
    private LocalDateTime mergeTime;
    
    @Column(name = "commit_url")
    private String commitUrl;
    
    @Column(name = "commit_sha")
    private String commitSha;
    
    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "approved_by", nullable = false)
    private User approvedBy;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "approved_time")
    private LocalDateTime approvedTime;

    // Constructors
    public ApprovedCommit() {}

    public ApprovedCommit(PendingCommit pendingCommit) {
        this.username = pendingCommit.getUsername();
        this.commitMessage = pendingCommit.getCommitMessage();
        this.originalBranch = pendingCommit.getBranch();
        this.taskId = pendingCommit.getTaskId();
        this.commitTime = pendingCommit.getCommitTime();
        this.commitUrl = pendingCommit.getCommitUrl();
        this.commitSha = pendingCommit.getCommitSha();
        this.project = pendingCommit.getProject();
        this.user = pendingCommit.getUser();
        this.mergeTime = LocalDateTime.now();
    }

    public ApprovedCommit(PendingCommit pendingCommit, User approvedBy) {
        this.username = pendingCommit.getUsername();
        this.commitMessage = pendingCommit.getCommitMessage();
        this.originalBranch = pendingCommit.getBranch();
        this.taskId = pendingCommit.getTaskId();
        this.commitTime = pendingCommit.getCommitTime();
        this.commitUrl = pendingCommit.getCommitUrl();
        this.commitSha = pendingCommit.getCommitSha();
        this.project = pendingCommit.getProject();
        this.user = pendingCommit.getUser();
        this.approvedBy = approvedBy;
        this.mergeTime = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getCommitMessage() { return commitMessage; }
    public void setCommitMessage(String commitMessage) { this.commitMessage = commitMessage; }

    public String getOriginalBranch() { return originalBranch; }
    public void setOriginalBranch(String originalBranch) { this.originalBranch = originalBranch; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public LocalDateTime getCommitTime() { return commitTime; }
    public void setCommitTime(LocalDateTime commitTime) { this.commitTime = commitTime; }

    public LocalDateTime getMergeTime() { return mergeTime; }
    public void setMergeTime(LocalDateTime mergeTime) { this.mergeTime = mergeTime; }

    public String getCommitUrl() { return commitUrl; }
    public void setCommitUrl(String commitUrl) { this.commitUrl = commitUrl; }

    public String getCommitSha() { return commitSha; }
    public void setCommitSha(String commitSha) { this.commitSha = commitSha; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public User getApprovedBy() { return approvedBy; }
    public void setApprovedBy(User approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getApprovedTime() { return approvedTime; }
    public void setApprovedTime(LocalDateTime approvedTime) { this.approvedTime = approvedTime; }
}

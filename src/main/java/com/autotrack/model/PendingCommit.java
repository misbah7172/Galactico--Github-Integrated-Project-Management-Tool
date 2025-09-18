package com.autotrack.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a pending commit awaiting review from team lead.
 */
@Entity
@Table(name = "pending_commits")
public class PendingCommit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String username;
    
    @Column(nullable = false, length = 1000)
    private String commitMessage;
    
    @Column(nullable = false)
    private String branch;
    
    @Column(name = "task_id")
    private String taskId;
    
    @Column(name = "commit_time", nullable = false)
    private LocalDateTime commitTime;
    
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
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommitStatus status = CommitStatus.PENDING_REVIEW;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;
    
    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;
    
    @Column(name = "merged_at")
    private LocalDateTime mergedAt;

    // Constructors
    public PendingCommit() {}

    public PendingCommit(String username, String commitMessage, String branch, 
                        String taskId, LocalDateTime commitTime, String commitUrl) {
        this.username = username;
        this.commitMessage = commitMessage;
        this.branch = branch;
        this.taskId = taskId;
        this.commitTime = commitTime;
        this.commitUrl = commitUrl;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public CommitStatus getStatus() { return status; }
    public void setStatus(CommitStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public User getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(User reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    
    public LocalDateTime getMergedAt() { return mergedAt; }
    public void setMergedAt(LocalDateTime mergedAt) { this.mergedAt = mergedAt; }
}

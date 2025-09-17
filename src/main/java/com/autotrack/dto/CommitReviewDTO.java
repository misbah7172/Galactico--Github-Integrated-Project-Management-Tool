package com.autotrack.dto;

import com.autotrack.model.CommitStatus;

/**
 * DTO for commit review decisions from team leads.
 */
public class CommitReviewDTO {
    
    private Long commitId;
    private CommitStatus status;
    private String rejectionReason;
    private String reviewedBy;

    // Constructors
    public CommitReviewDTO() {}

    public CommitReviewDTO(Long commitId, CommitStatus status, String reviewedBy) {
        this.commitId = commitId;
        this.status = status;
        this.reviewedBy = reviewedBy;
    }

    // Getters and Setters
    public Long getCommitId() { return commitId; }
    public void setCommitId(Long commitId) { this.commitId = commitId; }

    public CommitStatus getStatus() { return status; }
    public void setStatus(CommitStatus status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
}

package com.autotrack.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contributor_stats", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "contributor_email"}))
public class ContributorStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "contributor_name", nullable = false)
    private String contributorName;

    @Column(name = "contributor_email", nullable = false)
    private String contributorEmail;

    @Column(name = "total_commits")
    private Integer totalCommits = 0;

    @Column(name = "total_lines_added")
    private Integer totalLinesAdded = 0;

    @Column(name = "total_lines_modified")
    private Integer totalLinesModified = 0;

    @Column(name = "total_lines_deleted")
    private Integer totalLinesDeleted = 0;

    @Column(name = "total_files_changed")
    private Integer totalFilesChanged = 0;

    @Column(name = "approved_commits")
    private Integer approvedCommits = 0;

    @Column(name = "rejected_commits")
    private Integer rejectedCommits = 0;

    @Column(name = "pending_commits")
    private Integer pendingCommits = 0;

    @Column(name = "first_commit_date")
    private LocalDateTime firstCommitDate;

    @Column(name = "last_commit_date")
    private LocalDateTime lastCommitDate;

    @Column(name = "avg_commit_size")
    private Double avgCommitSize = 0.0;

    @Column(name = "productivity_score")
    private Double productivityScore = 0.0;

    @Column(name = "code_quality_score")
    private Double codeQualityScore = 0.0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ContributorStats() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public ContributorStats(Project project, String contributorName, String contributorEmail) {
        this();
        this.project = project;
        this.contributorName = contributorName;
        this.contributorEmail = contributorEmail;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
        calculateMetrics();
    }

    public void calculateMetrics() {
        if (totalCommits != null && totalCommits > 0) {
            int totalLines = (totalLinesAdded != null ? totalLinesAdded : 0) + 
                           (totalLinesModified != null ? totalLinesModified : 0) + 
                           (totalLinesDeleted != null ? totalLinesDeleted : 0);
            this.avgCommitSize = (double) totalLines / totalCommits;
            
            double approvalRate = approvedCommits != null && totalCommits > 0 ? 
                                (double) approvedCommits / totalCommits : 0.0;
            this.codeQualityScore = approvalRate * 100;
            
            this.productivityScore = calculateProductivityScore();
        }
    }

    private Double calculateProductivityScore() {
        if (totalCommits == null || totalCommits == 0) return 0.0;
        
        double commitScore = Math.min(totalCommits * 2.0, 100.0);
        double qualityScore = codeQualityScore != null ? codeQualityScore : 0.0;
        double sizeScore = avgCommitSize != null ? Math.min(avgCommitSize / 10.0, 50.0) : 0.0;
        
        return (commitScore * 0.4 + qualityScore * 0.4 + sizeScore * 0.2);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public String getContributorName() { return contributorName; }
    public void setContributorName(String contributorName) { this.contributorName = contributorName; }

    public String getContributorEmail() { return contributorEmail; }
    public void setContributorEmail(String contributorEmail) { this.contributorEmail = contributorEmail; }

    public Integer getTotalCommits() { return totalCommits; }
    public void setTotalCommits(Integer totalCommits) { this.totalCommits = totalCommits; }

    public Integer getTotalLinesAdded() { return totalLinesAdded; }
    public void setTotalLinesAdded(Integer totalLinesAdded) { this.totalLinesAdded = totalLinesAdded; }

    public Integer getTotalLinesModified() { return totalLinesModified; }
    public void setTotalLinesModified(Integer totalLinesModified) { this.totalLinesModified = totalLinesModified; }

    public Integer getTotalLinesDeleted() { return totalLinesDeleted; }
    public void setTotalLinesDeleted(Integer totalLinesDeleted) { this.totalLinesDeleted = totalLinesDeleted; }

    public Integer getTotalFilesChanged() { return totalFilesChanged; }
    public void setTotalFilesChanged(Integer totalFilesChanged) { this.totalFilesChanged = totalFilesChanged; }

    public Integer getApprovedCommits() { return approvedCommits; }
    public void setApprovedCommits(Integer approvedCommits) { this.approvedCommits = approvedCommits; }

    public Integer getRejectedCommits() { return rejectedCommits; }
    public void setRejectedCommits(Integer rejectedCommits) { this.rejectedCommits = rejectedCommits; }

    public Integer getPendingCommits() { return pendingCommits; }
    public void setPendingCommits(Integer pendingCommits) { this.pendingCommits = pendingCommits; }

    public LocalDateTime getFirstCommitDate() { return firstCommitDate; }
    public void setFirstCommitDate(LocalDateTime firstCommitDate) { this.firstCommitDate = firstCommitDate; }

    public LocalDateTime getLastCommitDate() { return lastCommitDate; }
    public void setLastCommitDate(LocalDateTime lastCommitDate) { this.lastCommitDate = lastCommitDate; }

    public Double getAvgCommitSize() { return avgCommitSize; }
    public void setAvgCommitSize(Double avgCommitSize) { this.avgCommitSize = avgCommitSize; }

    public Double getProductivityScore() { return productivityScore; }
    public void setProductivityScore(Double productivityScore) { this.productivityScore = productivityScore; }

    public Double getCodeQualityScore() { return codeQualityScore; }
    public void setCodeQualityScore(Double codeQualityScore) { this.codeQualityScore = codeQualityScore; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public int getTotalLinesChanged() {
        return (totalLinesAdded != null ? totalLinesAdded : 0) + 
               (totalLinesModified != null ? totalLinesModified : 0) + 
               (totalLinesDeleted != null ? totalLinesDeleted : 0);
    }

    public double getApprovalRate() {
        if (totalCommits == null || totalCommits == 0) return 0.0;
        return approvedCommits != null ? (double) approvedCommits / totalCommits * 100 : 0.0;
    }
}
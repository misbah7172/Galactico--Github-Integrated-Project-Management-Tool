package com.autotrack.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing sprint statistics and analytics.
 * Tracks sprint progress, velocity, and performance metrics.
 */
@Entity
@Table(name = "sprint_statistics")
public class SprintStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "sprint_id", nullable = false, unique = true)
    private Sprint sprint;
    
    // Sprint metrics
    @Column(name = "planned_story_points")
    private Integer plannedStoryPoints = 0;
    
    @Column(name = "completed_story_points")
    private Integer completedStoryPoints = 0;
    
    @Column(name = "total_tasks")
    private Integer totalTasks = 0;
    
    @Column(name = "completed_tasks")
    private Integer completedTasks = 0;
    
    // Velocity metrics
    @Column(name = "daily_burndown", columnDefinition = "JSON")
    private String dailyBurndown; // JSON string for daily progress
    
    @Column(name = "velocity_trend", precision = 5, scale = 2)
    private BigDecimal velocityTrend = BigDecimal.ZERO;
    
    // Code metrics
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
    
    // Time tracking
    @Column(name = "estimated_hours", precision = 8, scale = 2)
    private BigDecimal estimatedHours = BigDecimal.ZERO;
    
    @Column(name = "actual_hours", precision = 8, scale = 2)
    private BigDecimal actualHours = BigDecimal.ZERO;
    
    // Sprint health indicators
    @Enumerated(EnumType.STRING)
    @Column(name = "sprint_health")
    private SprintHealth sprintHealth = SprintHealth.ON_TRACK;
    
    @Column(name = "completion_prediction_date")
    private LocalDate completionPredictionDate;
    
    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    // Constructors
    public SprintStatistics() {
        this.calculatedAt = LocalDateTime.now();
    }

    public SprintStatistics(Sprint sprint) {
        this();
        this.sprint = sprint;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Sprint getSprint() { return sprint; }
    public void setSprint(Sprint sprint) { this.sprint = sprint; }

    public Integer getPlannedStoryPoints() { return plannedStoryPoints; }
    public void setPlannedStoryPoints(Integer plannedStoryPoints) { this.plannedStoryPoints = plannedStoryPoints; }

    public Integer getCompletedStoryPoints() { return completedStoryPoints; }
    public void setCompletedStoryPoints(Integer completedStoryPoints) { this.completedStoryPoints = completedStoryPoints; }

    public Integer getTotalTasks() { return totalTasks; }
    public void setTotalTasks(Integer totalTasks) { this.totalTasks = totalTasks; }

    public Integer getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(Integer completedTasks) { this.completedTasks = completedTasks; }

    public String getDailyBurndown() { return dailyBurndown; }
    public void setDailyBurndown(String dailyBurndown) { this.dailyBurndown = dailyBurndown; }

    public BigDecimal getVelocityTrend() { return velocityTrend; }
    public void setVelocityTrend(BigDecimal velocityTrend) { this.velocityTrend = velocityTrend; }

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

    public BigDecimal getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(BigDecimal estimatedHours) { this.estimatedHours = estimatedHours; }

    public BigDecimal getActualHours() { return actualHours; }
    public void setActualHours(BigDecimal actualHours) { this.actualHours = actualHours; }

    public SprintHealth getSprintHealth() { return sprintHealth; }
    public void setSprintHealth(SprintHealth sprintHealth) { this.sprintHealth = sprintHealth; }

    public LocalDate getCompletionPredictionDate() { return completionPredictionDate; }
    public void setCompletionPredictionDate(LocalDate completionPredictionDate) { this.completionPredictionDate = completionPredictionDate; }

    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }

    // Utility methods
    public double getCompletionPercentage() {
        if (plannedStoryPoints == null || plannedStoryPoints == 0) return 0.0;
        return (double) completedStoryPoints / plannedStoryPoints * 100.0;
    }

    public int getRemainingStoryPoints() {
        return Math.max(0, plannedStoryPoints - completedStoryPoints);
    }

    public double getTaskCompletionPercentage() {
        if (totalTasks == null || totalTasks == 0) return 0.0;
        return (double) completedTasks / totalTasks * 100.0;
    }

    public int getTotalLinesChanged() {
        return totalLinesAdded + totalLinesModified + totalLinesDeleted;
    }

    public BigDecimal getHoursVariance() {
        if (estimatedHours == null || actualHours == null) return BigDecimal.ZERO;
        return actualHours.subtract(estimatedHours);
    }

    public double getHoursVariancePercentage() {
        if (estimatedHours == null || estimatedHours.equals(BigDecimal.ZERO)) return 0.0;
        return getHoursVariance().divide(estimatedHours, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue();
    }

    @PrePersist
    @PreUpdate
    protected void updateCalculatedAt() {
        this.calculatedAt = LocalDateTime.now();
    }
}
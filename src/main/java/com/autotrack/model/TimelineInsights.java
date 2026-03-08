package com.autotrack.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing timeline insights and predictions.
 * Tracks timeline data for tasks, sprints, and projects.
 */
@Entity
@Table(name = "timeline_insights")
public class TimelineInsights {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private EntityType entityType;
    
    @Column(name = "entity_id", nullable = false)
    private Long entityId;
    
    // Timeline dates
    @Column(name = "estimated_start_date")
    private LocalDate estimatedStartDate;
    
    @Column(name = "actual_start_date")
    private LocalDate actualStartDate;
    
    @Column(name = "estimated_end_date")
    private LocalDate estimatedEndDate;
    
    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;
    
    @Column(name = "predicted_end_date")
    private LocalDate predictedEndDate;
    
    // Progress tracking
    @Column(name = "progress_percentage", precision = 5, scale = 2)
    private BigDecimal progressPercentage = BigDecimal.ZERO;
    
    @Column(name = "estimated_duration_days")
    private Integer estimatedDurationDays = 0;
    
    @Column(name = "actual_duration_days")
    private Integer actualDurationDays = 0;
    
    // Risk assessment
    @Enumerated(EnumType.STRING)
    @Column(name = "timeline_risk_level")
    private PriorityLevel timelineRiskLevel = PriorityLevel.LOW;
    
    @Column(name = "risk_factors", columnDefinition = "JSON")
    private String riskFactors; // JSON string for risk factors
    
    // Dependency tracking
    @Column(name = "dependencies", columnDefinition = "JSON")
    private String dependencies; // JSON string for dependencies
    
    @Column(name = "blocking_issues", columnDefinition = "JSON")
    private String blockingIssues; // JSON string for blocking issues
    
    // Timestamps
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public TimelineInsights() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public TimelineInsights(EntityType entityType, Long entityId) {
        this();
        this.entityType = entityType;
        this.entityId = entityId;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public EntityType getEntityType() { return entityType; }
    public void setEntityType(EntityType entityType) { this.entityType = entityType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public LocalDate getEstimatedStartDate() { return estimatedStartDate; }
    public void setEstimatedStartDate(LocalDate estimatedStartDate) { this.estimatedStartDate = estimatedStartDate; }

    public LocalDate getActualStartDate() { return actualStartDate; }
    public void setActualStartDate(LocalDate actualStartDate) { this.actualStartDate = actualStartDate; }

    public LocalDate getEstimatedEndDate() { return estimatedEndDate; }
    public void setEstimatedEndDate(LocalDate estimatedEndDate) { this.estimatedEndDate = estimatedEndDate; }

    public LocalDate getActualEndDate() { return actualEndDate; }
    public void setActualEndDate(LocalDate actualEndDate) { this.actualEndDate = actualEndDate; }

    public LocalDate getPredictedEndDate() { return predictedEndDate; }
    public void setPredictedEndDate(LocalDate predictedEndDate) { this.predictedEndDate = predictedEndDate; }

    public BigDecimal getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(BigDecimal progressPercentage) { this.progressPercentage = progressPercentage; }

    public Integer getEstimatedDurationDays() { return estimatedDurationDays; }
    public void setEstimatedDurationDays(Integer estimatedDurationDays) { this.estimatedDurationDays = estimatedDurationDays; }

    public Integer getActualDurationDays() { return actualDurationDays; }
    public void setActualDurationDays(Integer actualDurationDays) { this.actualDurationDays = actualDurationDays; }

    public PriorityLevel getTimelineRiskLevel() { return timelineRiskLevel; }
    public void setTimelineRiskLevel(PriorityLevel timelineRiskLevel) { this.timelineRiskLevel = timelineRiskLevel; }

    public String getRiskFactors() { return riskFactors; }
    public void setRiskFactors(String riskFactors) { this.riskFactors = riskFactors; }

    public String getDependencies() { return dependencies; }
    public void setDependencies(String dependencies) { this.dependencies = dependencies; }

    public String getBlockingIssues() { return blockingIssues; }
    public void setBlockingIssues(String blockingIssues) { this.blockingIssues = blockingIssues; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Utility methods
    public boolean isOverdue() {
        if (estimatedEndDate == null) return false;
        return LocalDate.now().isAfter(estimatedEndDate) && actualEndDate == null;
    }

    public boolean isCompleted() {
        return actualEndDate != null;
    }

    public int getDaysRemaining() {
        if (estimatedEndDate == null || isCompleted()) return 0;
        LocalDate today = LocalDate.now();
        return estimatedEndDate.isAfter(today) ? 
            (int) today.until(estimatedEndDate).getDays() : 0;
    }

    public int getDaysOverdue() {
        if (estimatedEndDate == null || !isOverdue()) return 0;
        return (int) estimatedEndDate.until(LocalDate.now()).getDays();
    }

    public BigDecimal getScheduleVariance() {
        if (estimatedDurationDays == null || actualDurationDays == null) return BigDecimal.ZERO;
        return BigDecimal.valueOf(actualDurationDays - estimatedDurationDays);
    }

    public double getScheduleVariancePercentage() {
        if (estimatedDurationDays == null || estimatedDurationDays == 0) return 0.0;
        return getScheduleVariance().divide(BigDecimal.valueOf(estimatedDurationDays), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Entity type enum
    public enum EntityType {
        TASK("Task"),
        SPRINT("Sprint"),
        PROJECT("Project");
        
        private final String displayName;
        
        EntityType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }

    // Builder pattern
    public static TimelineInsightsBuilder builder() {
        return new TimelineInsightsBuilder();
    }

    public static class TimelineInsightsBuilder {
        private EntityType entityType;
        private Long entityId;
        private LocalDate estimatedStartDate;
        private LocalDate estimatedEndDate;
        private Integer estimatedDurationDays;

        public TimelineInsightsBuilder entityType(EntityType entityType) {
            this.entityType = entityType;
            return this;
        }

        public TimelineInsightsBuilder entityId(Long entityId) {
            this.entityId = entityId;
            return this;
        }

        public TimelineInsightsBuilder estimatedStartDate(LocalDate estimatedStartDate) {
            this.estimatedStartDate = estimatedStartDate;
            return this;
        }

        public TimelineInsightsBuilder estimatedEndDate(LocalDate estimatedEndDate) {
            this.estimatedEndDate = estimatedEndDate;
            return this;
        }

        public TimelineInsightsBuilder estimatedDurationDays(Integer estimatedDurationDays) {
            this.estimatedDurationDays = estimatedDurationDays;
            return this;
        }

        public TimelineInsights build() {
            TimelineInsights insights = new TimelineInsights();
            insights.entityType = this.entityType;
            insights.entityId = this.entityId;
            insights.estimatedStartDate = this.estimatedStartDate;
            insights.estimatedEndDate = this.estimatedEndDate;
            insights.estimatedDurationDays = this.estimatedDurationDays;
            return insights;
        }
    }
}
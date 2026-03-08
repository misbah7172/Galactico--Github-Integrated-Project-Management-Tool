package com.autotrack.controller;

import com.autotrack.model.TimelineInsights;
import com.autotrack.service.TimelineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Timeline Insights management operations.
 * Provides endpoints for project timeline analytics and risk assessment.
 */
@RestController
@RequestMapping("/api/v1/timeline")
public class TimelineController {

    private static final Logger logger = LoggerFactory.getLogger(TimelineController.class);

    @Autowired
    private TimelineService timelineService;

    /**
     * Get timeline insights by type with pagination.
     */
    @GetMapping("/insights/{entityType}")
    public ResponseEntity<Page<TimelineInsights>> getTimelineInsightsByType(
            @PathVariable String entityType,
            Pageable pageable) {
        
        try {
            TimelineInsights.EntityType type = TimelineInsights.EntityType.valueOf(entityType.toUpperCase());
            List<TimelineInsights> allInsights = timelineService.getTimelineInsightsByType(type);
            Page<TimelineInsights> insights = new org.springframework.data.domain.PageImpl<>(allInsights, pageable, allInsights.size());
            
            return ResponseEntity.ok(insights);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid entity type: {}", entityType);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error fetching timeline insights for type {}: {}", entityType, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get timeline insight by entity ID and type.
     */
    @GetMapping("/insights/{entityType}/{entityId}")
    public ResponseEntity<TimelineInsights> getTimelineInsightByEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        
        try {
            TimelineInsights.EntityType type = TimelineInsights.EntityType.valueOf(entityType.toUpperCase());
            java.util.Optional<TimelineInsights> insight = timelineService.getTimelineInsightByEntity(type, entityId);
            
            if (insight.isPresent()) {
                return ResponseEntity.ok(insight.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            logger.error("Invalid entity type: {}", entityType);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error fetching timeline insight for {} {}: {}", entityType, entityId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a timeline insight for a specific entity.
     */
    @PostMapping("/insights/{entityType}/{entityId}")
    public ResponseEntity<TimelineInsights> createTimelineInsight(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        
        try {
            TimelineInsights.EntityType type = TimelineInsights.EntityType.valueOf(entityType.toUpperCase());
            TimelineInsights insight = timelineService.createTimelineInsight(type, entityId);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(insight);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid entity type: {}", entityType);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error creating timeline insight for {} {}: {}", entityType, entityId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete a timeline insight.
     */
    @DeleteMapping("/insights/{insightId}")
    public ResponseEntity<Void> deleteTimelineInsight(@PathVariable Long insightId) {
        try {
            TimelineInsights insight = timelineService.getTimelineInsightById(insightId);
            if (insight == null) {
                return ResponseEntity.notFound().build();
            }
            
            timelineService.deleteTimelineInsight(insightId);
            return ResponseEntity.noContent().build();
            
        } catch (Exception e) {
            logger.error("Error deleting timeline insight {}: {}", insightId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate all timeline insights.
     */
    @PostMapping("/insights/generate")
    public ResponseEntity<Void> generateAllTimelineInsights() {
        try {
            timelineService.generateAllTimelineInsights();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error generating timeline insights: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get overdue insights.
     */
    @GetMapping("/insights/overdue")
    public ResponseEntity<List<TimelineInsights>> getOverdueInsights() {
        try {
            List<TimelineInsights> insights = timelineService.getOverdueInsights();
            return ResponseEntity.ok(insights);
        } catch (Exception e) {
            logger.error("Error fetching overdue insights: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get high risk insights.
     */
    @GetMapping("/insights/high-risk")
    public ResponseEntity<List<TimelineInsights>> getHighRiskInsights() {
        try {
            List<TimelineInsights> insights = timelineService.getHighRiskInsights();
            return ResponseEntity.ok(insights);
        } catch (Exception e) {
            logger.error("Error fetching high risk insights: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get behind schedule insights.
     */
    @GetMapping("/insights/behind-schedule")
    public ResponseEntity<List<TimelineInsights>> getBehindScheduleInsights() {
        try {
            List<TimelineInsights> insights = timelineService.getBehindScheduleInsights();
            return ResponseEntity.ok(insights);
        } catch (Exception e) {
            logger.error("Error fetching behind schedule insights: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get overall timeline summary.
     */
    @GetMapping("/summary")
    public ResponseEntity<java.util.Map<String, Object>> getTimelineSummary() {
        try {
            java.util.Map<String, Object> summary = timelineService.getTimelineSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Error fetching timeline summary: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
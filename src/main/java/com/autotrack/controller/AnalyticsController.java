package com.autotrack.controller;

import com.autotrack.model.Project;
import com.autotrack.service.CommitAnalyticsService;
import com.autotrack.service.ProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final CommitAnalyticsService commitAnalyticsService;
    private final ProjectService projectService;

    public AnalyticsController(CommitAnalyticsService commitAnalyticsService,
                              ProjectService projectService) {
        this.commitAnalyticsService = commitAnalyticsService;
        this.projectService = projectService;
    }

    @GetMapping("/{projectId}/overview")
    public ResponseEntity<Map<String, Object>> getProjectOverview(@PathVariable Long projectId) {
        try {
            Project project = projectService.getProjectById(projectId);
            Map<String, Object> overview = commitAnalyticsService.calculateProjectOverview(project);
            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{projectId}/daily-activity")
    public ResponseEntity<List<Map<String, Object>>> getDailyActivity(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "30") int days) {
        try {
            Project project = projectService.getProjectById(projectId);
            List<Map<String, Object>> activity = commitAnalyticsService.getDailyActivity(project, days);
            return ResponseEntity.ok(activity);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{projectId}/commit-sizes")
    public ResponseEntity<Map<String, Object>> getCommitSizeDistribution(@PathVariable Long projectId) {
        try {
            Project project = projectService.getProjectById(projectId);
            Map<String, Object> distribution = commitAnalyticsService.getCommitSizeDistribution(project);
            return ResponseEntity.ok(distribution);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{projectId}/contributors")
    public ResponseEntity<List<Map<String, Object>>> getTopContributors(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            Project project = projectService.getProjectById(projectId);
            List<Map<String, Object>> contributors = commitAnalyticsService.getTopContributors(project, limit);
            return ResponseEntity.ok(contributors);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{projectId}/time-distribution")
    public ResponseEntity<Map<String, Object>> getTimeDistribution(@PathVariable Long projectId) {
        try {
            Project project = projectService.getProjectById(projectId);
            Map<String, Object> timeDistribution = commitAnalyticsService.getTimeDistribution(project);
            return ResponseEntity.ok(timeDistribution);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{projectId}/file-types")
    public ResponseEntity<Map<String, Object>> getFileTypeDistribution(@PathVariable Long projectId) {
        try {
            Project project = projectService.getProjectById(projectId);
            Map<String, Object> fileTypes = commitAnalyticsService.getFileTypeDistribution(project);
            return ResponseEntity.ok(fileTypes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{projectId}/trends")
    public ResponseEntity<Map<String, Object>> getCommitTrends(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "6") int months) {
        try {
            Project project = projectService.getProjectById(projectId);
            Map<String, Object> trends = commitAnalyticsService.getCommitTrends(project, months);
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{projectId}/code-quality")
    public ResponseEntity<Map<String, Object>> getCodeQualityMetrics(@PathVariable Long projectId) {
        try {
            Project project = projectService.getProjectById(projectId);
            Map<String, Object> quality = commitAnalyticsService.getCodeQualityMetrics(project);
            return ResponseEntity.ok(quality);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{projectId}/productivity-trends")
    public ResponseEntity<Map<String, Object>> getProductivityTrends(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "12") int weeks) {
        try {
            Project project = projectService.getProjectById(projectId);
            Map<String, Object> trends = commitAnalyticsService.getProductivityTrends(project, weeks);
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{projectId}/developer-performance")
    public ResponseEntity<Map<String, Object>> getDeveloperPerformance(@PathVariable Long projectId) {
        try {
            Project project = projectService.getProjectById(projectId);
            Map<String, Object> performance = commitAnalyticsService.getDeveloperPerformanceMetrics(project);
            return ResponseEntity.ok(performance);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{projectId}/team-collaboration")
    public ResponseEntity<Map<String, Object>> getTeamCollaboration(@PathVariable Long projectId) {
        try {
            Project project = projectService.getProjectById(projectId);
            Map<String, Object> collaboration = commitAnalyticsService.getTeamCollaborationInsights(project);
            return ResponseEntity.ok(collaboration);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
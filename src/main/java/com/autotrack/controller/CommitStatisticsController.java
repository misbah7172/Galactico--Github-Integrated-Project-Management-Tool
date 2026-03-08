package com.autotrack.controller;

import com.autotrack.model.Project;
import com.autotrack.repository.ProjectRepository;
import com.autotrack.service.CommitStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for commit statistics and analytics
 */
@RestController
@RequestMapping("/api/commit-stats")
public class CommitStatisticsController {
    
    private final CommitStatisticsService commitStatisticsService;
    private final ProjectRepository projectRepository;
    
    @Autowired
    public CommitStatisticsController(CommitStatisticsService commitStatisticsService,
                                    ProjectRepository projectRepository) {
        this.commitStatisticsService = commitStatisticsService;
        this.projectRepository = projectRepository;
    }
    
    /**
     * Get commit statistics for a project
     */
    @GetMapping("/project/{projectId}")
    // @PreAuthorize("@projectSecurityService.canAccessProject(#projectId, authentication.name)")
    public ResponseEntity<?> getProjectCommitStats(@PathVariable Long projectId) {
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        CommitStatisticsService.CommitStatsSummary stats = 
            commitStatisticsService.getProjectCommitStats(projectOpt.get());
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get commit statistics for a specific user in a project
     */
    @GetMapping("/project/{projectId}/user/{authorName}")
    @PreAuthorize("@projectSecurityService.canAccessProject(#projectId, authentication.name)")
    public ResponseEntity<?> getUserCommitStats(@PathVariable Long projectId, 
                                              @PathVariable String authorName) {
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        CommitStatisticsService.CommitStatsSummary stats = 
            commitStatisticsService.getUserCommitStats(projectOpt.get(), authorName);
        
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get daily commit activity for a project
     */
    @GetMapping("/project/{projectId}/daily-activity")
    // @PreAuthorize("@projectSecurityService.canAccessProject(#projectId, authentication.name)")
    public ResponseEntity<?> getDailyCommitActivity(@PathVariable Long projectId,
                                                   @RequestParam(defaultValue = "30") int days) {
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        List<CommitStatisticsService.DailyCommitActivity> activity = 
            commitStatisticsService.getDailyCommitActivity(projectOpt.get(), days);
        
        return ResponseEntity.ok(activity);
    }
    
    /**
     * Get top contributors for a project
     */
    @GetMapping("/project/{projectId}/top-contributors")
    // @PreAuthorize("@projectSecurityService.canAccessProject(#projectId, authentication.name)")
    public ResponseEntity<?> getTopContributors(@PathVariable Long projectId,
                                              @RequestParam(defaultValue = "10") int limit) {
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        List<Map<String, Object>> contributors = 
            commitStatisticsService.getTopContributors(projectOpt.get(), limit);
        
        return ResponseEntity.ok(contributors);
    }
    
    /**
     * Get commit size distribution for a project
     */
    @GetMapping("/project/{projectId}/size-distribution")
    // @PreAuthorize("@projectSecurityService.canAccessProject(#projectId, authentication.name)")
    public ResponseEntity<?> getCommitSizeDistribution(@PathVariable Long projectId) {
        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Integer> distribution = 
            commitStatisticsService.getCommitSizeDistribution(projectOpt.get());
        
        return ResponseEntity.ok(distribution);
    }
    
    /**
     * Get global commit statistics for a user
     */
    @GetMapping("/user/{authorName}/global")
    public ResponseEntity<?> getUserGlobalCommitStats(@PathVariable String authorName) {
        CommitStatisticsService.CommitStatsSummary stats = 
            commitStatisticsService.getUserCommitStatsGlobal(authorName);
        
        return ResponseEntity.ok(stats);
    }
}

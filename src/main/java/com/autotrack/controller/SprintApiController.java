package com.autotrack.controller;

import com.autotrack.dto.SprintDto;
import com.autotrack.model.*;
import com.autotrack.service.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API Controller for Sprint Management
 * Provides comprehensive sprint functionality like Jira
 */
@RestController
@RequestMapping("/api/v1/sprints")
public class SprintApiController {

    private final SprintService sprintService;
    private final ProjectService projectService;
    private final UserService userService;
    private final TaskService taskService;
    private final BacklogService backlogService;

    public SprintApiController(SprintService sprintService, ProjectService projectService, 
                              UserService userService, TaskService taskService, 
                              BacklogService backlogService) {
        this.sprintService = sprintService;
        this.projectService = projectService;
        this.userService = userService;
        this.taskService = taskService;
        this.backlogService = backlogService;
    }

    /**
     * Get all sprints for a project with pagination
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<Page<Sprint>> getSprintsByProject(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @AuthenticationPrincipal OAuth2User principal) {
        
        User user = userService.getCurrentUser(principal);
        Project project = projectService.getProjectById(projectId);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Sprint> sprints = sprintService.getSprintsByProject(projectId, pageable);
        return ResponseEntity.ok(sprints);
    }

    /**
     * Get active sprint for a project
     */
    @GetMapping("/project/{projectId}/active")
    public ResponseEntity<Sprint> getActiveSprint(@PathVariable Long projectId,
                                                 @AuthenticationPrincipal OAuth2User principal) {
        User user = userService.getCurrentUser(principal);
        
        Optional<Sprint> activeSprint = sprintService.getActiveSprintByProject(projectId);
        if (activeSprint.isPresent()) {
            return ResponseEntity.ok(activeSprint.get());
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    /**
     * Create a new sprint
     */
    @PostMapping("/project/{projectId}")
    public ResponseEntity<Sprint> createSprint(@PathVariable Long projectId,
                                              @Valid @RequestBody SprintDto sprintDto,
                                              @AuthenticationPrincipal OAuth2User principal) {
        User user = userService.getCurrentUser(principal);
        Project project = projectService.getProjectById(projectId);
        
        Sprint sprint = new Sprint();
        sprint.setName(sprintDto.getName());
        sprint.setSprintGoal(sprintDto.getGoal());
        sprint.setStartDate(sprintDto.getStartDate());
        sprint.setEndDate(sprintDto.getEndDate());
        sprint.setProject(project);
        sprint.setCreatedBy(user);
        sprint.setStatus(SprintStatus.UPCOMING);
        sprint.setCreatedAt(LocalDateTime.now());

        Sprint savedSprint = sprintService.createSprint(sprint);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSprint);
    }

    /**
     * Get sprint by ID
     */
    @GetMapping("/{sprintId}")
    public ResponseEntity<Sprint> getSprintById(@PathVariable Long sprintId,
                                               @AuthenticationPrincipal OAuth2User principal) {
        User user = userService.getCurrentUser(principal);
        Sprint sprint = sprintService.getSprintById(sprintId);
        return ResponseEntity.ok(sprint);
    }

    /**
     * Update sprint
     */
    @PutMapping("/{sprintId}")
    public ResponseEntity<Sprint> updateSprint(@PathVariable Long sprintId,
                                              @Valid @RequestBody SprintDto sprintDto,
                                              @AuthenticationPrincipal OAuth2User principal) {
        User user = userService.getCurrentUser(principal);
        Sprint sprint = sprintService.getSprintById(sprintId);
        
        sprint.setName(sprintDto.getName());
        sprint.setSprintGoal(sprintDto.getGoal());
        sprint.setStartDate(sprintDto.getStartDate());
        sprint.setEndDate(sprintDto.getEndDate());
        sprint.setUpdatedAt(LocalDateTime.now());

        Sprint updatedSprint = sprintService.updateSprint(sprint);
        return ResponseEntity.ok(updatedSprint);
    }

    /**
     * Start a sprint
     */
    @PutMapping("/{sprintId}/start")
    public ResponseEntity<Sprint> startSprint(@PathVariable Long sprintId,
                                             @AuthenticationPrincipal OAuth2User principal) {
        User user = userService.getCurrentUser(principal);
        Sprint sprint = sprintService.startSprint(sprintId);
        return ResponseEntity.ok(sprint);
    }

    /**
     * Complete a sprint
     */
    @PutMapping("/{sprintId}/complete")
    public ResponseEntity<Sprint> completeSprint(@PathVariable Long sprintId,
                                                @RequestBody(required = false) Map<String, String> requestBody,
                                                @AuthenticationPrincipal OAuth2User principal) {
        User user = userService.getCurrentUser(principal);
        String retrospectiveNotes = requestBody != null ? requestBody.get("retrospectiveNotes") : null;
        Sprint sprint = sprintService.completeSprint(sprintId);
        return ResponseEntity.ok(sprint);
    }

    /**
     * Delete a sprint
     */
    @DeleteMapping("/{sprintId}")
    public ResponseEntity<Void> deleteSprint(@PathVariable Long sprintId,
                                            @AuthenticationPrincipal OAuth2User principal) {
        User user = userService.getCurrentUser(principal);
        sprintService.deleteSprint(sprintId, "MOVE_TO_BACKLOG");
        return ResponseEntity.noContent().build();
    }

    /**
     * Get sprint progress and analytics
     */
    @GetMapping("/{sprintId}/progress")
    public ResponseEntity<Map<String, Object>> getSprintProgress(@PathVariable Long sprintId,
                                                                @AuthenticationPrincipal OAuth2User principal) {
        User user = userService.getCurrentUser(principal);
        Sprint sprint = sprintService.getSprintById(sprintId);
        
        // Get tasks in this sprint
        List<Task> sprintTasks = taskService.getTasksBySprint(sprint);
        
        // Calculate progress
        int totalTasks = sprintTasks.size();
        int completedTasks = (int) sprintTasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
        int inProgressTasks = (int) sprintTasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
        int todoTasks = totalTasks - completedTasks - inProgressTasks;
        
        // Calculate story points
        int totalStoryPoints = sprintTasks.stream().mapToInt(t -> t.getStoryPoints() != null ? t.getStoryPoints() : 0).sum();
        int completedStoryPoints = sprintTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .mapToInt(t -> t.getStoryPoints() != null ? t.getStoryPoints() : 0).sum();
        
        // Calculate days remaining
        int daysRemaining = 0;
        if (sprint.getEndDate() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (sprint.getEndDate().isAfter(now)) {
                daysRemaining = (int) java.time.Duration.between(now, sprint.getEndDate()).toDays();
            }
        }
        
        Map<String, Object> progress = new HashMap<>();
        progress.put("sprintId", sprintId);
        progress.put("sprintName", sprint.getName());
        progress.put("sprintStatus", sprint.getStatus());
        progress.put("totalTasks", totalTasks);
        progress.put("completedTasks", completedTasks);
        progress.put("inProgressTasks", inProgressTasks);
        progress.put("todoTasks", todoTasks);
        progress.put("totalStoryPoints", totalStoryPoints);
        progress.put("completedStoryPoints", completedStoryPoints);
        progress.put("daysRemaining", daysRemaining);
        progress.put("completionPercentage", totalStoryPoints > 0 ? (completedStoryPoints * 100.0 / totalStoryPoints) : 0);
        
        return ResponseEntity.ok(progress);
    }

    /**
     * Add backlog item to sprint
     */
    @PutMapping("/{sprintId}/backlog/{backlogItemId}")
    public ResponseEntity<Map<String, String>> addBacklogItemToSprint(@PathVariable Long sprintId,
                                                                      @PathVariable Long backlogItemId,
                                                                      @AuthenticationPrincipal OAuth2User principal) {
        User user = userService.getCurrentUser(principal);
        
        try {
            backlogService.assignBacklogItemToSprint(backlogItemId, sprintId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Backlog item successfully assigned to sprint");
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Failed to assign backlog item to sprint: " + e.getMessage());
            response.put("status", "error");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Remove backlog item from sprint
     */
    @DeleteMapping("/{sprintId}/backlog/{backlogItemId}")
    public ResponseEntity<Map<String, String>> removeBacklogItemFromSprint(@PathVariable Long sprintId,
                                                                           @PathVariable Long backlogItemId,
                                                                           @AuthenticationPrincipal OAuth2User principal) {
        User user = userService.getCurrentUser(principal);
        
        try {
            backlogService.removeBacklogItemFromSprint(backlogItemId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Backlog item successfully removed from sprint");
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Failed to remove backlog item from sprint: " + e.getMessage());
            response.put("status", "error");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Get sprint backlog (backlog items assigned to this sprint)
     */
    @GetMapping("/{sprintId}/backlog")
    public ResponseEntity<Page<BacklogItem>> getSprintBacklog(@PathVariable Long sprintId,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "50") int size,
                                                             @AuthenticationPrincipal OAuth2User principal) {
        User user = userService.getCurrentUser(principal);
        Sprint sprint = sprintService.getSprintById(sprintId);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<BacklogItem> sprintBacklog = backlogService.getBacklogItemsBySprint(sprint, pageable);
        
        return ResponseEntity.ok(sprintBacklog);
    }
}
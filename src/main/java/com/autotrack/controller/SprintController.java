package com.autotrack.controller;

import com.autotrack.dto.SprintProgressDto;
import com.autotrack.model.Project;
import com.autotrack.model.Sprint;
import com.autotrack.model.SprintStatus;
import com.autotrack.model.User;
import com.autotrack.service.ProjectService;
import com.autotrack.service.SprintService;
import com.autotrack.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing sprints.
 * Provides both web UI and REST API endpoints for sprint management.
 */
@Controller
@RequestMapping("/sprints")
public class SprintController {

    private final SprintService sprintService;
    private final ProjectService projectService;
    private final UserService userService;

    @Autowired
    public SprintController(SprintService sprintService, ProjectService projectService, UserService userService) {
        this.sprintService = sprintService;
        this.projectService = projectService;
        this.userService = userService;
    }

    /**
     * Show sprint list for a project
     */
    @GetMapping("/project/{projectId}")
    public String listSprints(@PathVariable Long projectId, Model model,
                             @AuthenticationPrincipal OAuth2User principal) {
        User currentUser = userService.getCurrentUser(principal);
        Project project = projectService.getProjectById(projectId);
        
        List<Sprint> sprints = sprintService.getSprintsByProjectId(projectId);
        
        model.addAttribute("project", project);
        model.addAttribute("sprints", sprints);
        model.addAttribute("currentUser", currentUser);
        
        return "sprint/list";
    }

    /**
     * Show sprint creation form
     */
    @GetMapping("/project/{projectId}/create")
    public String showCreateForm(@PathVariable Long projectId, Model model,
                                @AuthenticationPrincipal OAuth2User principal) {
        User currentUser = userService.getCurrentUser(principal);
        Project project = projectService.getProjectById(projectId);
        
        Sprint sprint = new Sprint();
        sprint.setProject(project);
        
        model.addAttribute("sprint", sprint);
        model.addAttribute("project", project);
        model.addAttribute("currentUser", currentUser);
        
        return "sprint/create";
    }

    /**
     * Create a new sprint
     */
    @PostMapping("/project/{projectId}/create")
    public String createSprint(@PathVariable Long projectId,
                              @ModelAttribute Sprint sprint,
                              @RequestParam String startDateStr,
                              @RequestParam String endDateStr,
                              @AuthenticationPrincipal OAuth2User principal,
                              RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.getCurrentUser(principal);
            Project project = projectService.getProjectById(projectId);
            
            // Parse dates
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            sprint.setStartDate(LocalDateTime.parse(startDateStr, formatter));
            sprint.setEndDate(LocalDateTime.parse(endDateStr, formatter));
            
            sprint.setProject(project);
            sprint.setCreatedBy(currentUser);
            
            Sprint createdSprint = sprintService.createSprint(sprint);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Sprint '" + createdSprint.getName() + "' created successfully!");
            
            return "redirect:/sprints/project/" + projectId;
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error creating sprint: " + e.getMessage());
            return "redirect:/sprints/project/" + projectId + "/create";
        }
    }

    /**
     * Show sprint details and dashboard
     */
    @GetMapping("/{sprintId}")
    public String showSprintDashboard(@PathVariable Long sprintId, Model model,
                                     @AuthenticationPrincipal OAuth2User principal) {
        User currentUser = userService.getCurrentUser(principal);
        SprintProgressDto sprintProgress = sprintService.getSprintProgress(sprintId);
        
        model.addAttribute("sprintProgress", sprintProgress);
        model.addAttribute("currentUser", currentUser);
        
        return "sprint/dashboard";
    }

    /**
     * Show sprint edit form
     */
    @GetMapping("/{sprintId}/edit")
    public String showEditForm(@PathVariable Long sprintId, Model model,
                              @AuthenticationPrincipal OAuth2User principal) {
        User currentUser = userService.getCurrentUser(principal);
        Sprint sprint = sprintService.getSprintById(sprintId);
        
        model.addAttribute("sprint", sprint);
        model.addAttribute("currentUser", currentUser);
        
        return "sprint/edit";
    }

    /**
     * Update a sprint
     */
    @PostMapping("/{sprintId}/edit")
    public String updateSprint(@PathVariable Long sprintId,
                              @ModelAttribute Sprint sprint,
                              @RequestParam String startDateStr,
                              @RequestParam String endDateStr,
                              @AuthenticationPrincipal OAuth2User principal,
                              RedirectAttributes redirectAttributes) {
        try {
            Sprint existingSprint = sprintService.getSprintById(sprintId);
            
            // Parse dates
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            existingSprint.setName(sprint.getName());
            existingSprint.setDescription(sprint.getDescription());
            existingSprint.setStartDate(LocalDateTime.parse(startDateStr, formatter));
            existingSprint.setEndDate(LocalDateTime.parse(endDateStr, formatter));
            
            sprintService.updateSprint(existingSprint);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Sprint updated successfully!");
            
            return "redirect:/sprints/" + sprintId;
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error updating sprint: " + e.getMessage());
            return "redirect:/sprints/" + sprintId + "/edit";
        }
    }

    /**
     * Start a sprint
     */
    @PostMapping("/{sprintId}/start")
    @ResponseBody
    public ResponseEntity<Map<String, String>> startSprint(@PathVariable Long sprintId) {
        try {
            sprintService.startSprint(sprintId);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Sprint started successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Complete a sprint
     */
    @PostMapping("/{sprintId}/complete")
    @ResponseBody
    public ResponseEntity<Map<String, String>> completeSprint(@PathVariable Long sprintId) {
        try {
            sprintService.completeSprint(sprintId);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Sprint completed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Delete a sprint
     */
    @DeleteMapping("/{sprintId}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deleteSprint(@PathVariable Long sprintId,
                                                           @RequestParam(defaultValue = "UNASSIGN") String taskAction) {
        try {
            sprintService.deleteSprint(sprintId, taskAction);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Sprint deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // REST API Endpoints

    /**
     * Get sprint progress data (REST API)
     */
    @GetMapping("/{sprintId}/progress")
    @ResponseBody
    public ResponseEntity<SprintProgressDto> getSprintProgress(@PathVariable Long sprintId) {
        try {
            SprintProgressDto progress = sprintService.getSprintProgress(sprintId);
            return ResponseEntity.ok(progress);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all sprints for a project (REST API)
     */
    @GetMapping("/api/project/{projectId}")
    @ResponseBody
    public ResponseEntity<List<Sprint>> getSprintsByProject(@PathVariable Long projectId) {
        try {
            List<Sprint> sprints = sprintService.getSprintsByProjectId(projectId);
            return ResponseEntity.ok(sprints);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Assign task to sprint (REST API)
     */
    @PostMapping("/{sprintId}/tasks/{taskId}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> assignTaskToSprint(@PathVariable Long sprintId,
                                                                 @PathVariable Long taskId) {
        try {
            sprintService.assignTaskToSprint(taskId, sprintId);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Task assigned to sprint"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Remove task from sprint (REST API)
     */
    @DeleteMapping("/{sprintId}/tasks/{taskId}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> removeTaskFromSprint(@PathVariable Long sprintId,
                                                                   @PathVariable Long taskId) {
        try {
            sprintService.removeTaskFromSprint(taskId);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Task removed from sprint"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Create sprint via REST API
     */
    @PostMapping("/api/project/{projectId}")
    @ResponseBody
    public ResponseEntity<Sprint> createSprintAPI(@PathVariable Long projectId,
                                                 @RequestBody Sprint sprint,
                                                 @AuthenticationPrincipal OAuth2User principal) {
        try {
            User currentUser = userService.getCurrentUser(principal);
            Project project = projectService.getProjectById(projectId);
            
            sprint.setProject(project);
            sprint.setCreatedBy(currentUser);
            
            Sprint createdSprint = sprintService.createSprint(sprint);
            return ResponseEntity.ok(createdSprint);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

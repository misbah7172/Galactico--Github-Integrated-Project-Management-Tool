package com.autotrack.controller;

import com.autotrack.dto.BacklogItemDto;
import com.autotrack.model.*;
import com.autotrack.service.BacklogService;
import com.autotrack.service.ProjectService;
import com.autotrack.service.SprintService;
import com.autotrack.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;


/**
 * REST Controller for managing backlog items and prioritization.
 * Provides comprehensive backlog management API for AutoTrack.
 */
@RestController
@RequestMapping("/api/backlog")
public class BacklogController {
    
    private static final Logger logger = LoggerFactory.getLogger(BacklogController.class);
    
    @Autowired
    private BacklogService backlogService;
    
    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private SprintService sprintService;
    
    @Autowired
    private UserService userService;
    
    /**
     * Get all backlog items for a project
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<Page<BacklogItem>> getBacklogItemsByProject(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "priorityScore") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            Pageable pageable) {
        
        try {
            Project project = projectService.getProjectById(projectId);
            
            List<BacklogItem> allItems = backlogService.getBacklogItemsByProject(project);
            // Convert to Page manually since BacklogService doesn't support Pageable yet
            // This is a simplified implementation - you might want to enhance BacklogService later
            Page<BacklogItem> backlogItems = new org.springframework.data.domain.PageImpl<>(allItems, pageable, allItems.size());
            return ResponseEntity.ok(backlogItems);
        } catch (Exception e) {
            logger.error("Error fetching backlog items for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get backlog items by priority level
     */
    @GetMapping("/project/{projectId}/priority/{priority}")
    public ResponseEntity<List<BacklogItem>> getBacklogItemsByPriority(
            @PathVariable Long projectId,
            @PathVariable PriorityLevel priority) {
        
        try {
            Project project = projectService.getProjectById(projectId);
            
            List<BacklogItem> items = backlogService.getBacklogItemsByPriority(project, priority);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            logger.error("Error fetching backlog items by priority for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get backlog items by status
     */
    @GetMapping("/project/{projectId}/status/{status}")
    public ResponseEntity<List<BacklogItem>> getBacklogItemsByStatus(
            @PathVariable Long projectId,
            @PathVariable BacklogStatus status) {
        
        try {
            Project project = projectService.getProjectById(projectId);
            
            List<BacklogItem> items = backlogService.getBacklogItemsByStatus(project, status);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            logger.error("Error fetching backlog items by status for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Create a new backlog item
     */
    @PostMapping("/project/{projectId}")
    public ResponseEntity<BacklogItem> createBacklogItem(
            @PathVariable Long projectId,
            @Valid @RequestBody BacklogItemDto backlogItemDto,
            Principal principal) {
        
        try {
            Project project = projectService.getProjectById(projectId);
            
            User creator = userService.getUserByNickname(principal.getName());
            
            BacklogItem backlogItem = BacklogItem.builder()
                .title(backlogItemDto.getTitle())
                .description(backlogItemDto.getDescription())
                .storyPoints(backlogItemDto.getStoryPoints())
                .businessValue(backlogItemDto.getBusinessValue())
                .effortEstimate(backlogItemDto.getEffortEstimate())
                .priorityLevel(backlogItemDto.getPriorityLevel())
                .issueType(backlogItemDto.getIssueType())
                .acceptanceCriteria(backlogItemDto.getAcceptanceCriteria())
                .epicName(backlogItemDto.getEpicName())
                .userStory(backlogItemDto.getUserStory())
                .project(project)
                .createdBy(creator)
                .build();
            
            BacklogItem savedItem = backlogService.createBacklogItem(backlogItem);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedItem);
            
        } catch (Exception e) {
            logger.error("Error creating backlog item for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Update an existing backlog item
     */
    @PutMapping("/{backlogItemId}")
    public ResponseEntity<BacklogItem> updateBacklogItem(
            @PathVariable Long backlogItemId,
            @Valid @RequestBody BacklogItemDto backlogItemDto,
            Principal principal) {
        
        try {
            BacklogItem existingItem = backlogService.getBacklogItemById(backlogItemId);
            if (existingItem == null) {
                return ResponseEntity.notFound().build();
            }
            
            existingItem.setTitle(backlogItemDto.getTitle());
            existingItem.setDescription(backlogItemDto.getDescription());
            existingItem.setStoryPoints(backlogItemDto.getStoryPoints());
            existingItem.setBusinessValue(backlogItemDto.getBusinessValue());
            existingItem.setEffortEstimate(backlogItemDto.getEffortEstimate());
            existingItem.setPriorityLevel(backlogItemDto.getPriorityLevel());
            existingItem.setAcceptanceCriteria(backlogItemDto.getAcceptanceCriteria());
            existingItem.setEpicName(backlogItemDto.getEpicName());
            existingItem.setUserStory(backlogItemDto.getUserStory());
            
            // Update assignee if provided
            if (backlogItemDto.getAssignedToId() != null) {
                User assignee = userService.getUserById(backlogItemDto.getAssignedToId());
                existingItem.setAssignedTo(assignee);
            }
            
            BacklogItem updatedItem = backlogService.updateBacklogItem(existingItem);
            return ResponseEntity.ok(updatedItem);
            
        } catch (Exception e) {
            logger.error("Error updating backlog item {}: {}", backlogItemId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Delete a backlog item
     */
    @DeleteMapping("/{backlogItemId}")
    public ResponseEntity<Void> deleteBacklogItem(@PathVariable Long backlogItemId) {
        try {
            BacklogItem existingItem = backlogService.getBacklogItemById(backlogItemId);
            if (existingItem == null) {
                return ResponseEntity.notFound().build();
            }
            
            backlogService.deleteBacklogItem(backlogItemId);
            return ResponseEntity.noContent().build();
            
        } catch (Exception e) {
            logger.error("Error deleting backlog item {}: {}", backlogItemId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Assign backlog item to a sprint
     */
    @PostMapping("/{backlogItemId}/assign-sprint/{sprintId}")
    public ResponseEntity<BacklogItem> assignToSprint(
            @PathVariable Long backlogItemId,
            @PathVariable Long sprintId) {
        
        try {
            BacklogItem backlogItem = backlogService.getBacklogItemById(backlogItemId);
            if (backlogItem == null) {
                return ResponseEntity.notFound().build();
            }
            
            Sprint sprint = sprintService.getSprintById(sprintId);
            if (sprint == null) {
                return ResponseEntity.notFound().build();
            }
            
            BacklogItem updatedItem = backlogService.assignBacklogItemToSprint(backlogItem, sprint);
            return ResponseEntity.ok(updatedItem);
            
        } catch (Exception e) {
            logger.error("Error assigning backlog item {} to sprint {}: {}", backlogItemId, sprintId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Remove backlog item from sprint
     */
    @PostMapping("/{backlogItemId}/remove-sprint")
    public ResponseEntity<BacklogItem> removeFromSprint(@PathVariable Long backlogItemId) {
        try {
            BacklogItem backlogItem = backlogService.getBacklogItemById(backlogItemId);
            if (backlogItem == null) {
                return ResponseEntity.notFound().build();
            }
            
            BacklogItem updatedItem = backlogService.removeBacklogItemFromSprint(backlogItem);
            return ResponseEntity.ok(updatedItem);
            
        } catch (Exception e) {
            logger.error("Error removing backlog item {} from sprint: {}", backlogItemId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Mark backlog item as completed
     */
    @PostMapping("/{backlogItemId}/complete")
    public ResponseEntity<BacklogItem> completeBacklogItem(@PathVariable Long backlogItemId) {
        try {
            BacklogItem backlogItem = backlogService.getBacklogItemById(backlogItemId);
            if (backlogItem == null) {
                return ResponseEntity.notFound().build();
            }
            
            BacklogItem completedItem = backlogService.completeBacklogItem(backlogItem.getId());
            return ResponseEntity.ok(completedItem);
            
        } catch (Exception e) {
            logger.error("Error completing backlog item {}: {}", backlogItemId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get top priority backlog items for sprint planning
     */
    @GetMapping("/project/{projectId}/top-priority")
    public ResponseEntity<List<BacklogItem>> getTopPriorityItems(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "10") int limit) {
        
        try {
            Project project = projectService.getProjectById(projectId);
            
            List<BacklogItem> topItems = backlogService.getTopPriorityBacklogItems(project, limit);
            return ResponseEntity.ok(topItems);
            
        } catch (Exception e) {
            logger.error("Error fetching top priority items for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Bulk update backlog item priorities
     */
    @PostMapping("/project/{projectId}/bulk-priority")
    public ResponseEntity<List<BacklogItem>> bulkUpdatePriorities(
            @PathVariable Long projectId,
            @RequestBody Map<Long, PriorityLevel> priorityUpdates) {
        
        try {
            // Validate project exists
            projectService.getProjectById(projectId);
            
            List<BacklogItem> updatedItems = backlogService.bulkUpdatePriorities(priorityUpdates);
            return ResponseEntity.ok(updatedItems);
            
        } catch (Exception e) {
            logger.error("Error bulk updating priorities for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get backlog analytics for dashboard
     */
    @GetMapping("/project/{projectId}/analytics")
    public ResponseEntity<Map<String, Object>> getBacklogAnalytics(@PathVariable Long projectId) {
        try {
            Project project = projectService.getProjectById(projectId);
            
            Map<String, Object> analytics = backlogService.getBacklogAnalytics(project);
            return ResponseEntity.ok(analytics);
            
        } catch (Exception e) {
            logger.error("Error fetching backlog analytics for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Search backlog items by text
     */
    @GetMapping("/project/{projectId}/search")
    public ResponseEntity<List<BacklogItem>> searchBacklogItems(
            @PathVariable Long projectId,
            @RequestParam String query,
            @RequestParam(defaultValue = "20") int limit) {
        
        try {
            Project project = projectService.getProjectById(projectId);
            
            List<BacklogItem> results = backlogService.searchBacklogItems(project, query, limit);
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            logger.error("Error searching backlog items for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
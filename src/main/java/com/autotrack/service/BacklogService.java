package com.autotrack.service;

import com.autotrack.model.*;
import com.autotrack.repository.BacklogItemRepository;
import com.autotrack.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing product backlog and sprint backlog operations.
 */
@Service
@Transactional
public class BacklogService {

    private final BacklogItemRepository backlogItemRepository;
    private final TaskRepository taskRepository;
    private final NotificationService notificationService;

    private final SprintService sprintService;

    @Autowired
    public BacklogService(BacklogItemRepository backlogItemRepository,
                         TaskRepository taskRepository,
                         NotificationService notificationService,
                         SprintService sprintService) {
        this.backlogItemRepository = backlogItemRepository;
        this.taskRepository = taskRepository;
        this.notificationService = notificationService;
        this.sprintService = sprintService;
    }

    /**
     * Create a new backlog item.
     */
    public BacklogItem createBacklogItem(BacklogItem backlogItem) {
        // Set default priority rank if not specified
        if (backlogItem.getPriorityRank() == null || backlogItem.getPriorityRank() == 0) {
            int nextRank = getNextPriorityRank(backlogItem.getProject(), backlogItem.getPriorityLevel());
            backlogItem.setPriorityRank(nextRank);
        }

        BacklogItem savedItem = backlogItemRepository.save(backlogItem);
        
        // Send notification to project team
        if (savedItem.getAssignedTo() != null) {
            notificationService.notifyBacklogItemAssigned(savedItem);
        }
        
        return savedItem;
    }

    /**
     * Update an existing backlog item.
     */
    public BacklogItem updateBacklogItem(BacklogItem backlogItem) {
        BacklogItem existingItem = getBacklogItemById(backlogItem.getId());
        
        // Track priority changes
        boolean priorityChanged = !existingItem.getPriorityLevel().equals(backlogItem.getPriorityLevel());
        
        // Update fields
        existingItem.setTitle(backlogItem.getTitle());
        existingItem.setDescription(backlogItem.getDescription());
        existingItem.setPriorityLevel(backlogItem.getPriorityLevel());
        existingItem.setPriorityRank(backlogItem.getPriorityRank());
        existingItem.setStoryPoints(backlogItem.getStoryPoints());
        existingItem.setBusinessValue(backlogItem.getBusinessValue());
        existingItem.setEffortEstimate(backlogItem.getEffortEstimate());
        existingItem.setAcceptanceCriteria(backlogItem.getAcceptanceCriteria());
        existingItem.setLabels(backlogItem.getLabels());
        existingItem.setEpicName(backlogItem.getEpicName());
        existingItem.setUserStory(backlogItem.getUserStory());
        
        BacklogItem updatedItem = backlogItemRepository.save(existingItem);
        
        // Notify about priority changes
        if (priorityChanged && updatedItem.getAssignedTo() != null) {
            notificationService.notifyBacklogItemPriorityChanged(updatedItem, existingItem.getPriorityLevel());
        }
        
        return updatedItem;
    }

    /**
     * Get backlog item by ID.
     */
    public BacklogItem getBacklogItemById(Long id) {
        return backlogItemRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Backlog item not found with ID: " + id));
    }

    /**
     * Get all backlog items for a project ordered by priority.
     */
    public List<BacklogItem> getBacklogItemsByProject(Project project) {
        return backlogItemRepository.findByProjectOrderByPriority(project);
    }

    /**
     * Get product backlog items (not in any sprint).
     */
    public List<BacklogItem> getProductBacklog(Project project) {
        return backlogItemRepository.findProductBacklogByProject(project);
    }

    /**
     * Get backlog items for a specific sprint.
     */
    public List<BacklogItem> getSprintBacklog(Sprint sprint) {
        return backlogItemRepository.findBySprintOrderByPriorityLevelDescPriorityRankAsc(sprint);
    }

    /**
     * Move backlog item to sprint.
     */
    public BacklogItem moveToSprint(Long backlogItemId, Sprint sprint) {
        BacklogItem item = getBacklogItemById(backlogItemId);
        
        // Check sprint capacity
        int currentSprintPoints = getTotalStoryPointsInSprint(sprint);
        int availableCapacity = sprint.getPlannedVelocity() - currentSprintPoints;
        
        if (item.getStoryPoints() > availableCapacity) {
            throw new RuntimeException("Sprint capacity exceeded. Available: " + availableCapacity + 
                                     " points, Required: " + item.getStoryPoints() + " points");
        }
        
        item.setSprint(sprint);
        item.setBacklogStatus(BacklogStatus.SPRINT_BACKLOG);
        item.setMovedToSprintAt(LocalDateTime.now());
        
        BacklogItem updatedItem = backlogItemRepository.save(item);
        
        // Create or update associated task
        createOrUpdateTask(updatedItem);
        
        // Notify assignee about sprint assignment
        if (updatedItem.getAssignedTo() != null) {
            notificationService.notifyBacklogItemMovedToSprint(updatedItem, sprint);
        }
        
        return updatedItem;
    }

    /**
     * Move backlog item back to product backlog.
     */
    public BacklogItem moveToProductBacklog(Long backlogItemId) {
        BacklogItem item = getBacklogItemById(backlogItemId);
        
        Sprint previousSprint = item.getSprint();
        item.setSprint(null);
        item.setBacklogStatus(BacklogStatus.PRODUCT_BACKLOG);
        item.setMovedToSprintAt(null);
        
        BacklogItem updatedItem = backlogItemRepository.save(item);
        
        // Notify about removal from sprint
        if (updatedItem.getAssignedTo() != null && previousSprint != null) {
            notificationService.notifyBacklogItemRemovedFromSprint(updatedItem, previousSprint);
        }
        
        return updatedItem;
    }

    /**
     * Reorder backlog items by priority.
     */
    public void reorderBacklogItems(Project project, List<Long> orderedItemIds) {
        for (int i = 0; i < orderedItemIds.size(); i++) {
            Long itemId = orderedItemIds.get(i);
            BacklogItem item = getBacklogItemById(itemId);
            item.setPriorityRank(i + 1);
            backlogItemRepository.save(item);
        }
    }

    /**
     * Complete a backlog item.
     */
    public BacklogItem completeBacklogItem(Long backlogItemId) {
        BacklogItem item = getBacklogItemById(backlogItemId);
        
        item.setBacklogStatus(BacklogStatus.COMPLETED);
        item.setCompletedAt(LocalDateTime.now());
        
        BacklogItem completedItem = backlogItemRepository.save(item);
        
        // Update associated task status
        if (completedItem.getTask() != null) {
            Task task = completedItem.getTask();
            task.setStatus(TaskStatus.DONE);
            taskRepository.save(task);
        }
        
        // Notify about completion
        if (completedItem.getAssignedTo() != null) {
            notificationService.notifyBacklogItemCompleted(completedItem);
        }
        
        return completedItem;
    }

    /**
     * Delete a backlog item.
     */
    public void deleteBacklogItem(Long backlogItemId) {
        BacklogItem item = getBacklogItemById(backlogItemId);
        
        // Delete associated task if exists
        if (item.getTask() != null) {
            taskRepository.delete(item.getTask());
        }
        
        backlogItemRepository.delete(item);
    }

    /**
     * Get backlog items by epic.
     */
    public List<BacklogItem> getBacklogItemsByEpic(Project project, String epicName) {
        return backlogItemRepository.findByProjectAndEpicNameOrderByPriorityRankAsc(project, epicName);
    }

    /**
     * Get backlog velocity data for analytics.
     */
    public List<BacklogVelocityData> getVelocityData(Project project) {
        List<Object[]> rawData = backlogItemRepository.getVelocityDataByProject(project);
        
        return rawData.stream()
            .map(row -> new BacklogVelocityData(
                (Long) row[0],     // sprintId
                (Long) row[1],     // itemCount
                (Long) row[2]      // totalStoryPoints
            ))
            .collect(Collectors.toList());
    }

    /**
     * Get total story points in a sprint.
     */
    public int getTotalStoryPointsInSprint(Sprint sprint) {
        Integer total = backlogItemRepository.sumStoryPointsBySprint(sprint);
        return total != null ? total : 0;
    }

    // Private helper methods

    private int getNextPriorityRank(Project project, PriorityLevel priorityLevel) {
        List<BacklogItem> items = backlogItemRepository.findByProjectAndPriorityLevelOrderByPriorityRankAsc(project, priorityLevel);
        return items.isEmpty() ? 1 : items.get(items.size() - 1).getPriorityRank() + 1;
    }

    private void createOrUpdateTask(BacklogItem backlogItem) {
        Task task = backlogItem.getTask();
        
        if (task == null) {
            // Create new task from backlog item
            task = Task.builder()
                .featureCode(generateFeatureCode(backlogItem))
                .title(backlogItem.getTitle())
                .status(TaskStatus.TODO)
                .assignee(backlogItem.getAssignedTo())
                .project(backlogItem.getProject())
                .sprint(backlogItem.getSprint())
                .storyPoints(backlogItem.getStoryPoints())
                .acceptanceCriteria(backlogItem.getAcceptanceCriteria())
                .epicName(backlogItem.getEpicName())
                .userStory(backlogItem.getUserStory())
                .priorityLevel(backlogItem.getPriorityLevel())
                .taskType(TaskType.STORY)
                .build();
                
            task = taskRepository.save(task);
            
            // Link back to backlog item
            backlogItem.setTask(task);
            backlogItemRepository.save(backlogItem);
        } else {
            // Update existing task
            task.setTitle(backlogItem.getTitle());
            task.setAssignee(backlogItem.getAssignedTo());
            task.setSprint(backlogItem.getSprint());
            task.setStoryPoints(backlogItem.getStoryPoints());
            task.setAcceptanceCriteria(backlogItem.getAcceptanceCriteria());
            task.setEpicName(backlogItem.getEpicName());
            task.setUserStory(backlogItem.getUserStory());
            task.setPriorityLevel(backlogItem.getPriorityLevel());
            
            taskRepository.save(task);
        }
    }

    private String generateFeatureCode(BacklogItem backlogItem) {
        // Generate a feature code based on project and backlog item
        String projectPrefix = backlogItem.getProject().getName().substring(0, 
            Math.min(3, backlogItem.getProject().getName().length())).toUpperCase();
        return projectPrefix + backlogItem.getId();
    }

    // Data class for velocity analytics
    public static class BacklogVelocityData {
        private final Long sprintId;
        private final Long itemCount;
        private final Long totalStoryPoints;

        public BacklogVelocityData(Long sprintId, Long itemCount, Long totalStoryPoints) {
            this.sprintId = sprintId;
            this.itemCount = itemCount;
            this.totalStoryPoints = totalStoryPoints;
        }

        public Long getSprintId() { return sprintId; }
        public Long getItemCount() { return itemCount; }
        public Long getTotalStoryPoints() { return totalStoryPoints; }
    }

    /**
     * Get backlog items filtered by priority level.
     */
    public List<BacklogItem> getBacklogItemsByPriority(Project project, PriorityLevel priority) {
        return backlogItemRepository.findByProjectAndPriorityLevel(project, priority);
    }

    /**
     * Get backlog items filtered by status.
     */
    public List<BacklogItem> getBacklogItemsByStatus(Project project, BacklogStatus status) {
        return backlogItemRepository.findByProjectAndBacklogStatus(project, status);
    }

    /**
     * Assign backlog item to sprint.
     */
    public BacklogItem assignBacklogItemToSprint(BacklogItem item, Sprint sprint) {
        item.setSprint(sprint);
        item.setBacklogStatus(BacklogStatus.SPRINT_BACKLOG);
        BacklogItem updatedItem = backlogItemRepository.save(item);
        
        // Send notification
        if (item.getAssignedTo() != null) {
            notificationService.notifyBacklogItemAssigned(updatedItem);
        }
        
        return updatedItem;
    }

    /**
     * Remove backlog item from sprint (move to product backlog).
     */
    public BacklogItem removeBacklogItemFromSprint(BacklogItem item) {
        item.setSprint(null);
        item.setBacklogStatus(BacklogStatus.PRODUCT_BACKLOG);
        return backlogItemRepository.save(item);
    }
    
    /**
     * Assign backlog item to sprint using IDs.
     */
    public BacklogItem assignBacklogItemToSprint(Long backlogItemId, Long sprintId) {
        BacklogItem item = getBacklogItemById(backlogItemId);
        Sprint sprint = sprintService.getSprintById(sprintId);
        return assignBacklogItemToSprint(item, sprint);
    }
    
    /**
     * Remove backlog item from sprint using ID.
     */
    public BacklogItem removeBacklogItemFromSprint(Long backlogItemId) {
        BacklogItem item = getBacklogItemById(backlogItemId);
        return removeBacklogItemFromSprint(item);
    }
    
    /**
     * Get backlog items by sprint with pagination.
     */
    public Page<BacklogItem> getBacklogItemsBySprint(Sprint sprint, Pageable pageable) {
        return backlogItemRepository.findBySprintOrderByPriorityRankAsc(sprint, pageable);
    }

    /**
     * Get top priority backlog items for a project.
     */
    public List<BacklogItem> getTopPriorityBacklogItems(Project project, int limit) {
        return backlogItemRepository.findByProjectOrderByPriorityRankAsc(project)
            .stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Bulk update priorities of backlog items.
     */
    public List<BacklogItem> bulkUpdatePriorities(java.util.Map<Long, PriorityLevel> priorityUpdates) {
        return priorityUpdates.entrySet().stream()
            .map(entry -> {
                BacklogItem item = getBacklogItemById(entry.getKey());
                if (item != null) {
                    item.setPriorityLevel(entry.getValue());
                    return backlogItemRepository.save(item);
                }
                return null;
            })
            .filter(item -> item != null)
            .collect(Collectors.toList());
    }

    /**
     * Get backlog analytics for a project.
     */
    public java.util.Map<String, Object> getBacklogAnalytics(Project project) {
        List<BacklogItem> allItems = getBacklogItemsByProject(project);
        
        java.util.Map<String, Object> analytics = new java.util.HashMap<>();
        analytics.put("totalItems", allItems.size());
        analytics.put("totalStoryPoints", allItems.stream().mapToInt(BacklogItem::getStoryPoints).sum());
        
        // Count by status
        java.util.Map<BacklogStatus, Long> statusCounts = allItems.stream()
            .collect(Collectors.groupingBy(BacklogItem::getBacklogStatus, Collectors.counting()));
        analytics.put("statusBreakdown", statusCounts);
        
        // Count by priority
        java.util.Map<PriorityLevel, Long> priorityCounts = allItems.stream()
            .collect(Collectors.groupingBy(BacklogItem::getPriorityLevel, Collectors.counting()));
        analytics.put("priorityBreakdown", priorityCounts);
        
        return analytics;
    }

    /**
     * Search backlog items in a project.
     */
    public List<BacklogItem> searchBacklogItems(Project project, String query, int limit) {
        return backlogItemRepository.findByProjectAndTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            project, query, query)
            .stream()
            .limit(limit)
            .collect(Collectors.toList());
    }
}
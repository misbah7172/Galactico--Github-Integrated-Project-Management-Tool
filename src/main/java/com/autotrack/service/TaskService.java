package com.autotrack.service;

import com.autotrack.dto.TaskDTO;
import com.autotrack.model.*;
import com.autotrack.repository.ProjectRepository;
import com.autotrack.repository.TaskRepository;
import com.autotrack.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing tasks.
 */
@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final GitHubService gitHubService;
    private final SlackService slackService;

    public TaskService(TaskRepository taskRepository, 
                      ProjectRepository projectRepository,
                      UserRepository userRepository,
                      NotificationService notificationService,
                      GitHubService gitHubService,
                      SlackService slackService) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.gitHubService = gitHubService;
        this.slackService = slackService;
    }

    /**
     * Get all tasks.
     */
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    /**
     * Get tasks assigned to a user.
     */
    public List<Task> getTasksByAssignee(User assignee) {
        return taskRepository.findByAssigneeOrderByUpdatedAtDesc(assignee);
    }

    /**
     * Count tasks assigned to a user.
     */
    public long countTasksByAssignee(User assignee) {
        return taskRepository.findByAssigneeOrderByUpdatedAtDesc(assignee).size();
    }

    /**
     * Get task by ID.
     */
    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with ID: " + id));
    }

    /**
     * Get tasks by project.
     */
    public List<Task> getTasksByProject(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with ID: " + projectId));
        return taskRepository.findByProjectOrderByUpdatedAtDesc(project);
    }

    /**
     * Get tasks by project and status.
     */
    public List<Task> getTasksByProjectAndStatus(Long projectId, TaskStatus status) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with ID: " + projectId));
        return taskRepository.findByProjectAndStatus(project, status);
    }

    /**
     * Get tasks by assignee.
     */
    public List<Task> getTasksByAssignee(Long userId) {
        User assignee = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        return taskRepository.findByAssigneeOrderByUpdatedAtDesc(assignee);
    }

    /**
     * Get tasks by sprint.
     */
    public List<Task> getTasksBySprint(Sprint sprint) {
        return taskRepository.findBySprintOrderByUpdatedAtDesc(sprint);
    }

    /**
     * Get tasks by sprint ID.
     */
    public List<Task> getTasksBySprintId(Long sprintId) {
        if (sprintId == null) {
            return List.of();
        }
        return taskRepository.findBySprintIdOrderByUpdatedAtDesc(sprintId);
    }

    /**
     * Create a new task.
     */
    @Transactional
    public Task createTask(TaskDTO taskDTO) {
        Project project = projectRepository.findById(taskDTO.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found with ID: " + taskDTO.getProjectId()));
        
        User assignee = null;
        if (taskDTO.getAssigneeId() != null) {
            assignee = userRepository.findById(taskDTO.getAssigneeId())
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + taskDTO.getAssigneeId()));
        }
        
        List<String> tags = taskDTO.getTags() != null ? 
                Arrays.stream(taskDTO.getTags().split(","))
                        .map(String::trim)
                        .filter(tag -> !tag.isEmpty())
                        .collect(Collectors.toList()) : 
                List.of();
        
        Task task = Task.builder()
                .featureCode(taskDTO.getFeatureCode())
                .title(taskDTO.getTitle())
                .status(taskDTO.getStatus())
                .assignee(assignee)
                .project(project)
                .milestone(taskDTO.getMilestone())
                .build();
        
        task.setTags(tags);
        
        Task savedTask = taskRepository.save(task);
        
        // Create GitHub issue
        String issueUrl = gitHubService.createGitHubIssue(savedTask, project);
        if (issueUrl != null) {
            savedTask.setGitHubIssueUrl(issueUrl);
            savedTask = taskRepository.save(savedTask);
        }
        
        // Notify assignee
        if (assignee != null) {
            notificationService.notifyTaskCreated(savedTask);
        }
        
        // Send Slack notification
        if (assignee != null) {
            slackService.sendTaskCreatedNotification(
                savedTask.getTitle(),
                savedTask.getFeatureCode(),
                assignee.getNickname()
            );
        }
        
        return savedTask;
    }

    /**
     * Update an existing task.
     */
    @Transactional
    public Task updateTask(Long id, TaskDTO taskDTO) {
        Task existingTask = getTaskById(id);
        User oldAssignee = existingTask.getAssignee();
        TaskStatus oldStatus = existingTask.getStatus();
        
        // Update basic fields
        existingTask.setFeatureCode(taskDTO.getFeatureCode());
        existingTask.setTitle(taskDTO.getTitle());
        
        // Update status and notify if changed
        if (existingTask.getStatus() != taskDTO.getStatus()) {
            existingTask.setStatus(taskDTO.getStatus());
            notificationService.notifyTaskStatusChanged(existingTask, oldStatus);
        }
        
        // Update assignee and notify if changed
        if (taskDTO.getAssigneeId() != null) {
            User newAssignee = userRepository.findById(taskDTO.getAssigneeId())
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + taskDTO.getAssigneeId()));
            
            if (oldAssignee == null || !oldAssignee.getId().equals(newAssignee.getId())) {
                existingTask.setAssignee(newAssignee);
                notificationService.notifyTaskAssigneeChanged(existingTask, oldAssignee);
            }
        } else if (oldAssignee != null) {
            existingTask.setAssignee(null);
            notificationService.notifyTaskAssigneeChanged(existingTask, oldAssignee);
        }
        
        // Update milestone
        existingTask.setMilestone(taskDTO.getMilestone());
        
        // Update tags
        if (taskDTO.getTags() != null) {
            List<String> tags = Arrays.stream(taskDTO.getTags().split(","))
                    .map(String::trim)
                    .filter(tag -> !tag.isEmpty())
                    .collect(Collectors.toList());
            existingTask.setTags(tags);
        } else {
            existingTask.setTags(List.of());
        }
        
        return taskRepository.save(existingTask);
    }

    /**
     * Update task status.
     */
    @Transactional
    public Task updateTaskStatus(Long id, TaskStatus newStatus) {
        Task task = getTaskById(id);
        TaskStatus oldStatus = task.getStatus();
        
        if (oldStatus != newStatus) {
            task.setStatus(newStatus);
            Task updatedTask = taskRepository.save(task);
            notificationService.notifyTaskStatusChanged(updatedTask, oldStatus);
            
            // Send Slack notification
            if (updatedTask.getAssignee() != null) {
                slackService.sendTaskStatusChangeNotification(
                    updatedTask.getTitle(),
                    updatedTask.getFeatureCode(),
                    oldStatus.toString(),
                    newStatus.toString(),
                    updatedTask.getAssignee().getNickname()
                );
            }
            
            return updatedTask;
        }
        
        return task;
    }

    /**
     * Delete a task.
     */
    @Transactional
    public void deleteTask(Long id) {
        Task task = getTaskById(id);
        taskRepository.delete(task);
    }

    /**
     * Find or create a task based on commit information.
     */
    @Transactional
    public Task findOrCreateTaskFromCommit(Project project, CommitParserService.CommitInfo commitInfo) {
        // Check if task with this feature code already exists
        Optional<Task> existingTask = taskRepository.findByFeatureCodeAndProject(
                commitInfo.getFeatureCode(), project);
        
        if (existingTask.isPresent()) {
            // Update existing task
            Task task = existingTask.get();
            
            // Update status if provided
            if (commitInfo.getStatus() != null) {
                TaskStatus oldStatus = task.getStatus();
                TaskStatus newStatus = TaskStatus.valueOf(commitInfo.getStatus());
                
                if (oldStatus != newStatus) {
                    task.setStatus(newStatus);
                    notificationService.notifyTaskStatusChanged(task, oldStatus);
                }
            }
            
            // Update assignee if provided
            if (commitInfo.getAssigneeNickname() != null) {
                Optional<User> assignee = userRepository.findByNickname(commitInfo.getAssigneeNickname());
                
                if (assignee.isPresent()) {
                    User oldAssignee = task.getAssignee();
                    User newAssignee = assignee.get();
                    
                    if (oldAssignee == null || !oldAssignee.getId().equals(newAssignee.getId())) {
                        task.setAssignee(newAssignee);
                        notificationService.notifyTaskAssigneeChanged(task, oldAssignee);
                    }
                }
            }
            
            // Update tags if provided
            if (commitInfo.getTags() != null && !commitInfo.getTags().isEmpty()) {
                List<String> currentTags = task.getTags();
                List<String> newTags = Arrays.stream(commitInfo.getTags().split(","))
                        .map(String::trim)
                        .filter(tag -> !tag.isEmpty())
                        .collect(Collectors.toList());
                
                // Add new tags
                for (String tag : newTags) {
                    if (!currentTags.contains(tag)) {
                        currentTags.add(tag);
                    }
                }
                
                task.setTags(currentTags);
            }
            
            Task savedTask = taskRepository.save(task);
            
            // Send Slack notification for commit update
            if (commitInfo.getAssigneeNickname() != null) {
                slackService.sendNewCommitNotification(
                    commitInfo.getFeatureCode(),
                    commitInfo.getAssigneeNickname(),
                    commitInfo.getTaskTitle()
                );
            }
            
            return savedTask;
        } else {
            // Create new task
            TaskStatus status = TaskStatus.TODO;
            if (commitInfo.getStatus() != null) {
                status = TaskStatus.valueOf(commitInfo.getStatus());
            } else if (commitInfo.getAssigneeNickname() != null) {
                status = TaskStatus.IN_PROGRESS;
            }
            
            User assignee = null;
            if (commitInfo.getAssigneeNickname() != null) {
                Optional<User> assigneeOpt = userRepository.findByNickname(commitInfo.getAssigneeNickname());
                assignee = assigneeOpt.orElse(null);
            }
            
            List<String> tags = commitInfo.getTags() != null ? 
                    Arrays.stream(commitInfo.getTags().split(","))
                            .map(String::trim)
                            .filter(tag -> !tag.isEmpty())
                            .collect(Collectors.toList()) : 
                    List.of();
            
            Task newTask = Task.builder()
                    .featureCode(commitInfo.getFeatureCode())
                    .title(commitInfo.getTaskTitle())
                    .status(status)
                    .assignee(assignee)
                    .project(project)
                    .build();
            
            newTask.setTags(tags);
            
            Task savedTask = taskRepository.save(newTask);
            
            // Create GitHub issue
            String issueUrl = gitHubService.createGitHubIssue(savedTask, project);
            if (issueUrl != null) {
                savedTask.setGitHubIssueUrl(issueUrl);
                savedTask = taskRepository.save(savedTask);
            }
            
            // Notify assignee
            if (assignee != null) {
                notificationService.notifyTaskCreated(savedTask);
            }
            
            // Send Slack notification for new task from commit
            if (assignee != null) {
                slackService.sendNewCommitNotification(
                    commitInfo.getFeatureCode(),
                    assignee.getNickname(),
                    commitInfo.getTaskTitle()
                );
            }
            
            return savedTask;
        }
    }

    /**
     * Decline a task commit (Team Leader only).
     * Moves the task back to TODO status and notifies the assignee.
     */
    @Transactional
    public Task declineTask(Long taskId, User teamLeader) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            throw new RuntimeException("Task not found with ID: " + taskId);
        }
        
        Task task = taskOpt.get();
        
        // Verify the task is currently in progress
        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new RuntimeException("Only in-progress tasks can be declined");
        }
        
        task.setStatus(TaskStatus.TODO);
        task.setDeclinedBy(teamLeader);
        task.setDeclinedAt(java.time.LocalDateTime.now());
        task.setDeclineReason("Task does not meet quality standards. Please review and improve before resubmitting.");
        
        Task savedTask = taskRepository.save(task);
        
        // Notify the assignee about the decline
        if (task.getAssignee() != null) {
            notificationService.notifyTaskDeclined(savedTask, teamLeader);
        }
        
        // Send Slack notification
        if (task.getAssignee() != null) {
            slackService.sendTaskDeclinedNotification(
                savedTask.getTitle(),
                savedTask.getFeatureCode(),
                task.getAssignee().getNickname(),
                teamLeader.getNickname()
            );
        }
        
        return savedTask;
    }
}

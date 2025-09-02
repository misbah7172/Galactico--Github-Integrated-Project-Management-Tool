package com.autotrack.controller;

import com.autotrack.dto.TaskDTO;
import com.autotrack.model.Project;
import com.autotrack.model.Task;
import com.autotrack.model.TaskStatus;
import com.autotrack.model.User;
import com.autotrack.service.ProjectService;
import com.autotrack.service.TaskService;
import com.autotrack.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for task management.
 */
@Controller
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;
    private final ProjectService projectService;
    private final UserService userService;

    public TaskController(TaskService taskService, ProjectService projectService, UserService userService) {
        this.taskService = taskService;
        this.projectService = projectService;
        this.userService = userService;
    }

    /**
     * Show kanban board for a project.
     */
    @GetMapping("/project/{projectId}")
    public String showKanbanBoard(@PathVariable Long projectId, Model model) {
        Project project = projectService.getProjectById(projectId);
        
        // Get BACKLOG and TODO tasks for the TODO column
        List<Task> backlogTasks = taskService.getTasksByProjectAndStatus(projectId, TaskStatus.BACKLOG);
        List<Task> todoTasks = taskService.getTasksByProjectAndStatus(projectId, TaskStatus.TODO);
        
        // Combine BACKLOG and TODO tasks for the TODO column
        List<Task> combinedTodoTasks = new ArrayList<>();
        combinedTodoTasks.addAll(backlogTasks);
        combinedTodoTasks.addAll(todoTasks);
        
        List<Task> inProgressTasks = taskService.getTasksByProjectAndStatus(projectId, TaskStatus.IN_PROGRESS);
        List<Task> doneTasks = taskService.getTasksByProjectAndStatus(projectId, TaskStatus.DONE);
        
        model.addAttribute("project", project);
        model.addAttribute("todoTasks", combinedTodoTasks);
        model.addAttribute("inProgressTasks", inProgressTasks);
        model.addAttribute("doneTasks", doneTasks);
        
        return "kanban";
    }

    /**
     * Show task details.
     */
    @GetMapping("/{id}")
    public String showTask(@PathVariable Long id, Model model) {
        Task task = taskService.getTaskById(id);
        model.addAttribute("task", task);
        return "task/detail";
    }

    /**
     * Show task creation form.
     */
    @GetMapping("/create")
    @PreAuthorize("hasAuthority('TEAM_LEAD')")
    public String showCreateForm(@RequestParam Long projectId, Model model) {
        Project project = projectService.getProjectById(projectId);
        List<User> teamMembers = project.getTeam().getMembers();
        
        model.addAttribute("taskDTO", new TaskDTO());
        model.addAttribute("project", project);
        model.addAttribute("teamMembers", teamMembers);
        model.addAttribute("statuses", TaskStatus.values());
        
        return "task/create";
    }

    /**
     * Process task creation.
     */
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('TEAM_LEAD')")
    public String createTask(@Valid @ModelAttribute("taskDTO") TaskDTO taskDTO,
                            BindingResult result,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        
        if (result.hasErrors()) {
            Project project = projectService.getProjectById(taskDTO.getProjectId());
            List<User> teamMembers = project.getTeam().getMembers();
            
            model.addAttribute("project", project);
            model.addAttribute("teamMembers", teamMembers);
            model.addAttribute("statuses", TaskStatus.values());
            
            return "task/create";
        }
        
        try {
            Task task = taskService.createTask(taskDTO);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Task " + task.getTitle() + " created successfully!");
            return "redirect:/tasks/project/" + taskDTO.getProjectId();
        } catch (Exception e) {
            Project project = projectService.getProjectById(taskDTO.getProjectId());
            List<User> teamMembers = project.getTeam().getMembers();
            
            model.addAttribute("errorMessage", "Error creating task: " + e.getMessage());
            model.addAttribute("project", project);
            model.addAttribute("teamMembers", teamMembers);
            model.addAttribute("statuses", TaskStatus.values());
            
            return "task/create";
        }
    }

    /**
     * Show task edit form.
     */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, 
                              @AuthenticationPrincipal OAuth2User principal,
                              Model model) {
        
        Task task = taskService.getTaskById(id);
        User currentUser = userService.getCurrentUser(principal);
        
        // Check if user is team lead or the assignee of the task
        if (!currentUser.hasRole("TEAM_LEAD") && 
            !task.getAssignee().getId().equals(currentUser.getId())) {
            return "redirect:/tasks/" + id + "?error=unauthorized";
        }
        
        TaskDTO taskDTO = new TaskDTO();
        taskDTO.setId(task.getId());
        taskDTO.setFeatureCode(task.getFeatureCode());
        taskDTO.setTitle(task.getTitle());
        taskDTO.setStatus(task.getStatus());
        taskDTO.setAssigneeId(task.getAssignee().getId());
        taskDTO.setProjectId(task.getProject().getId());
        taskDTO.setMilestone(task.getMilestone());
        taskDTO.setTags(String.join(",", task.getTags()));
        
        model.addAttribute("taskDTO", taskDTO);
        model.addAttribute("project", task.getProject());
        model.addAttribute("teamMembers", task.getProject().getTeam().getMembers());
        model.addAttribute("statuses", TaskStatus.values());
        
        return "task/edit";
    }

    /**
     * Process task update.
     */
    @PostMapping("/{id}/edit")
    public String updateTask(@PathVariable Long id,
                            @Valid @ModelAttribute("taskDTO") TaskDTO taskDTO,
                            BindingResult result,
                            @AuthenticationPrincipal OAuth2User principal,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        
        Task task = taskService.getTaskById(id);
        User currentUser = userService.getCurrentUser(principal);
        
        // Check if user is team lead or the assignee of the task
        if (!currentUser.hasRole("TEAM_LEAD") && 
            !task.getAssignee().getId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "You are not authorized to edit this task");
            return "redirect:/tasks/" + id;
        }
        
        if (result.hasErrors()) {
            model.addAttribute("project", task.getProject());
            model.addAttribute("teamMembers", task.getProject().getTeam().getMembers());
            model.addAttribute("statuses", TaskStatus.values());
            return "task/edit";
        }
        
        try {
            Task updatedTask = taskService.updateTask(id, taskDTO);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Task " + updatedTask.getTitle() + " updated successfully!");
            return "redirect:/tasks/" + id;
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error updating task: " + e.getMessage());
            model.addAttribute("project", task.getProject());
            model.addAttribute("teamMembers", task.getProject().getTeam().getMembers());
            model.addAttribute("statuses", TaskStatus.values());
            return "task/edit";
        }
    }

    /**
     * Update task status (AJAX).
     */
    @PostMapping("/{id}/status")
    @ResponseBody
    public String updateTaskStatus(@PathVariable Long id, 
                                  @RequestParam TaskStatus status,
                                  @AuthenticationPrincipal OAuth2User principal) {
        
        Task task = taskService.getTaskById(id);
        User currentUser = userService.getCurrentUser(principal);
        
        // Check if user is team lead or the assignee of the task
        if (!currentUser.hasRole("TEAM_LEAD") && 
            !task.getAssignee().getId().equals(currentUser.getId())) {
            return "{\"error\": \"Unauthorized\"}";
        }
        
        try {
            taskService.updateTaskStatus(id, status);
            return "{\"success\": true, \"message\": \"Status updated to " + status + "\"}";
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * Decline task commit (Team Leader only).
     */
    @PostMapping("/api/tasks/{id}/decline")
    @PreAuthorize("hasAuthority('TEAM_LEAD')")
    @ResponseBody
    public String declineTask(@PathVariable Long id, 
                             @RequestBody(required = false) String requestBody,
                             @AuthenticationPrincipal OAuth2User principal) {
        
        try {
            taskService.getTaskById(id); // Verify task exists
            User currentUser = userService.getCurrentUser(principal);
            
            // Verify the current user is a team leader
            if (!currentUser.hasRole("TEAM_LEAD")) {
                return "{\"error\": \"Only team leaders can decline commits\"}";
            }
            
            // Move task back to TODO status
            taskService.declineTask(id, currentUser);
            
            return "{\"success\": true, \"message\": \"Task declined and moved back to TODO. Team member has been notified.\"}";
            
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * Delete task.
     */
    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('TEAM_LEAD')")
    public String deleteTask(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Task task = taskService.getTaskById(id);
        Long projectId = task.getProject().getId();
        
        try {
            taskService.deleteTask(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Task deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error deleting task: " + e.getMessage());
        }
        
        return "redirect:/tasks/project/" + projectId;
    }

    /**
     * Show all tasks assigned to the current user.
     */
    @GetMapping("/assigned")
    public String showAssignedTasks(@AuthenticationPrincipal OAuth2User principal, Model model) {
        User currentUser = userService.getCurrentUser(principal);
        List<Task> assignedTasks = taskService.getTasksByAssignee(currentUser);
        
        model.addAttribute("assignedTasks", assignedTasks);
        model.addAttribute("currentUser", currentUser);
        return "task/assigned";
    }
}

package com.autotrack.controller;

import com.autotrack.model.Project;
import com.autotrack.model.Task;
import com.autotrack.model.TaskStatus;
import com.autotrack.model.User;
import com.autotrack.service.ProjectService;
import com.autotrack.service.TaskService;
import com.autotrack.service.UserService;
import com.autotrack.repository.TaskRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test controller to help populate test data.
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    private final TaskService taskService;
    private final ProjectService projectService;
    private final UserService userService;
    private final TaskRepository taskRepository;

    public TestController(TaskService taskService, ProjectService projectService, UserService userService, TaskRepository taskRepository) {
        this.taskService = taskService;
        this.projectService = projectService;
        this.userService = userService;
        this.taskRepository = taskRepository;
    }

    /**
     * Create sample tasks for testing.
     */
    @PostMapping("/create-sample-tasks/{projectId}")
    public ResponseEntity<Map<String, Object>> createSampleTasks(@PathVariable Long projectId,
                                                                 @AuthenticationPrincipal OAuth2User principal) {
        try {
            Project project = projectService.getProjectById(projectId);
            User currentUser = userService.getCurrentUser(principal);

            Map<String, Object> result = new HashMap<>();
            List<Task> createdTasks = new ArrayList<>();

            // Create tasks with different statuses
            TaskStatus[] statuses = {TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.DONE};
            String[] taskTitles = {"Setup authentication", "Create user interface", "Deploy to production"};
            String[] featureCodes = {"FEATURE02", "FEATURE03", "FEATURE04"};

            for (int i = 0; i < statuses.length; i++) {
                Task task = new Task();
                task.setFeatureCode(featureCodes[i]);
                task.setTitle(taskTitles[i]);
                task.setStatus(statuses[i]);
                task.setAssignee(currentUser);
                task.setProject(project);
                task.setCreatedAt(LocalDateTime.now());
                task.setUpdatedAt(LocalDateTime.now());

                Task savedTask = taskRepository.save(task);
                createdTasks.add(savedTask);
            }

            result.put("success", true);
            result.put("message", "Created " + createdTasks.size() + " sample tasks");
            result.put("tasks", createdTasks.stream().map(task -> {
                Map<String, Object> taskInfo = new HashMap<>();
                taskInfo.put("id", task.getId());
                taskInfo.put("title", task.getTitle());
                taskInfo.put("featureCode", task.getFeatureCode());
                taskInfo.put("status", task.getStatus().toString());
                return taskInfo;
            }).toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResult);
        }
    }
}

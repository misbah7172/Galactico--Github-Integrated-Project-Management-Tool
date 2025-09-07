package com.autotrack.controller;

import com.autotrack.model.User;
import com.autotrack.service.UserService;
import com.autotrack.service.TeamService;
import com.autotrack.service.ProjectService;
import com.autotrack.service.TaskService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for user profile management.
 */
@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;
    private final TeamService teamService;
    private final ProjectService projectService;
    private final TaskService taskService;
    @Autowired
    public ProfileController(UserService userService, TeamService teamService, 
                           ProjectService projectService, TaskService taskService) {
        this.userService = userService;
        this.teamService = teamService;
        this.projectService = projectService;
        this.taskService = taskService;
    }

    /**
     * Show user profile page.
     */
    @GetMapping
    public String showProfile(Model model, @AuthenticationPrincipal OAuth2User principal) {
        User user = userService.getCurrentUser(principal);
        
        // Get user statistics
        model.addAttribute("user", user);
        model.addAttribute("teams", teamService.getTeamsByUser(user));
        model.addAttribute("projects", projectService.getProjectsByUser(user));
        model.addAttribute("teamCount", teamService.getTeamsByUser(user).size());
        model.addAttribute("projectCount", projectService.getProjectsByUser(user).size());
        model.addAttribute("taskCount", taskService.getTasksByAssignee(user.getId()).size());
        model.addAttribute("commitCount", 0); // Will be implemented with GitHub webhook integration
        
        return "profile";
    }
}
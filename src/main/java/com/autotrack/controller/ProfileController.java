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
        var teams = teamService.getTeamsByUser(user);
        var projects = projectService.getProjectsByUser(user);
        
        model.addAttribute("user", user);
        model.addAttribute("teams", teams);
        model.addAttribute("projects", projects);
        model.addAttribute("teamCount", teams.size());
        model.addAttribute("projectCount", projects.size());
        model.addAttribute("taskCount", taskService.getTasksByAssignee(user.getId()).size());
        model.addAttribute("commitCount", 0);
        
        return "profile";
    }
}
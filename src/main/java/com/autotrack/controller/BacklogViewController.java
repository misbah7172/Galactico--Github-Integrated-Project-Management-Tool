package com.autotrack.controller;

import com.autotrack.model.Project;
import com.autotrack.model.User;
import com.autotrack.service.ProjectService;
import com.autotrack.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Controller for backlog management.
 */
@Controller
@RequestMapping("/backlog")
public class BacklogViewController {

    private final ProjectService projectService;
    private final UserService userService;

    public BacklogViewController(ProjectService projectService, UserService userService) {
        this.projectService = projectService;
        this.userService = userService;
    }

    /**
     * Show backlog management page.
     */
    @GetMapping
    public String showBacklogIndex(@AuthenticationPrincipal OAuth2User principal, Model model) {
        User user = userService.getCurrentUser(principal);
        List<Project> projects = projectService.getProjectsByUser(user);
        
        model.addAttribute("projects", projects);
        model.addAttribute("currentUser", user);
        return "backlog/index";
    }
}
package com.autotrack.controller;

import com.autotrack.model.Project;
import com.autotrack.model.Team;
import com.autotrack.model.User;
import com.autotrack.service.ProjectService;
import com.autotrack.service.TeamService;
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

import java.util.List;
import java.util.Map;

/**
 * Controller for project management.
 */
@Controller
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final TeamService teamService;
    private final UserService userService;
    private final LanguageAnalysisService languageAnalysisService;

    public ProjectController(ProjectService projectService, TeamService teamService, 
                           UserService userService, LanguageAnalysisService languageAnalysisService) {
        this.projectService = projectService;
        this.teamService = teamService;
        this.userService = userService;
        this.languageAnalysisService = languageAnalysisService;
    }

    /**
     * Show all projects.
     */
    @GetMapping
    public String listProjects(@AuthenticationPrincipal OAuth2User principal, Model model) {
        User user = userService.getCurrentUser(principal);
        List<Project> projects = projectService.getProjectsByUser(user);
        model.addAttribute("projects", projects);
        model.addAttribute("currentUser", user);
        return "project/list";
    }

    /**
     * Show project creation form.
     */
    @GetMapping("/create")
    @PreAuthorize("hasAuthority('ROLE_MEMBER') or hasAuthority('ROLE_TEAM_LEAD')")
    public String showCreateForm(Model model, @AuthenticationPrincipal OAuth2User principal) {
        User user = userService.getCurrentUser(principal);
        // Only show teams where the user is the owner (can create projects)
        List<Team> teams = teamService.getTeamsByUser(user).stream()
            .filter(team -> teamService.isUserOwnerOfTeam(user, team))
            .toList();
        
        model.addAttribute("projectDTO", new ProjectDTO());
        model.addAttribute("teams", teams);
        return "project/create";
    }

    /**
     * Process project creation.
     */
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('ROLE_MEMBER') or hasAuthority('ROLE_TEAM_LEAD')")
    public String createProject(@Valid @ModelAttribute("projectDTO") ProjectDTO projectDTO,
                                BindingResult result,
                                @AuthenticationPrincipal OAuth2User principal,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        
        if (result.hasErrors()) {
            User user = userService.getCurrentUser(principal);
            List<Team> teams = teamService.getTeamsByUser(user);
            model.addAttribute("teams", teams);
            return "project/create";
        }
        
        try {
            User currentUser = userService.getCurrentUser(principal);
            Project project = projectService.createProject(projectDTO, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Project " + project.getName() + " created successfully!");
            return "redirect:/projects/" + project.getId();
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error creating project: " + e.getMessage());
            User user = userService.getCurrentUser(principal);
            List<Team> teams = teamService.getTeamsByUser(user);
            model.addAttribute("teams", teams);
            return "project/create";
        }
    }

    /**
     * Show project details.
     */
    @GetMapping("/{id}")
    public String showProject(@PathVariable Long id, Model model, @AuthenticationPrincipal OAuth2User principal) {
        User currentUser = userService.getCurrentUser(principal);
        Project project = projectService.getProjectById(id);
        List<TeamMemberContributionDTO> memberContributions = projectService.getTeamMemberContributions(id);
        
        // Get language statistics for the project
        Map<String, Double> languageStats = languageAnalysisService.getLanguageStatistics(project);
        
        model.addAttribute("project", project);
        model.addAttribute("memberContributions", memberContributions);
        model.addAttribute("languageStats", languageStats);
        model.addAttribute("languageAnalysisService", languageAnalysisService); // For color lookup in template
        model.addAttribute("currentUser", currentUser);
        return "project/detail";
    }

    /**
     * Show project analytics page.
     */
    @GetMapping("/{id}/analytics")
    public String showProjectAnalytics(@PathVariable Long id, Model model, @AuthenticationPrincipal OAuth2User principal) {
        User currentUser = userService.getCurrentUser(principal);
        Project project = projectService.getProjectById(id);
        
        model.addAttribute("project", project);
        model.addAttribute("currentUser", currentUser);
        return "project/analytics";
    }

    /**
     * Show project edit form.
     */
    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('ROLE_TEAM_LEAD')")
    public String showEditForm(@PathVariable Long id, Model model) {
        Project project = projectService.getProjectById(id);
        ProjectDTO projectDTO = new ProjectDTO();
        projectDTO.setId(project.getId());
        projectDTO.setName(project.getName());
        projectDTO.setGitHubRepoUrl(project.getGitHubRepoUrl());
        projectDTO.setGitHubAccessToken(project.getGitHubAccessToken());
        projectDTO.setTeamId(project.getTeam().getId());
        
        model.addAttribute("projectDTO", projectDTO);
        model.addAttribute("teams", teamService.getAllTeams());
        return "project/edit";
    }

    /**
     * Show project team change form.
     */
    @GetMapping("/{id}/change-team")
    public String showChangeTeamForm(@PathVariable Long id, Model model, @AuthenticationPrincipal OAuth2User principal) {
        User currentUser = userService.getCurrentUser(principal);
        Project project = projectService.getProjectById(id);
        
        // Check authorization
        boolean isProjectOwner = project.getOwner().equals(currentUser);
        boolean isCurrentTeamOwner = project.getTeam() != null && 
                                   project.getTeam().getOwner().equals(currentUser);
        
        if (!isProjectOwner && !isCurrentTeamOwner) {
            model.addAttribute("errorMessage", "You don't have permission to change this project's team");
            return "redirect:/projects/" + id;
        }
        
        List<Team> availableTeams = teamService.getTeamsByUser(currentUser);
        List<com.autotrack.model.ProjectTeamHistory> teamHistory = projectService.getProjectTeamHistory(id);
        
        model.addAttribute("project", project);
        model.addAttribute("availableTeams", availableTeams);
        model.addAttribute("teamHistory", teamHistory);
        model.addAttribute("currentUser", currentUser);
        return "project/change-team";
    }

    /**
     * Process project team change.
     */
    @PostMapping("/{id}/change-team")
    public String changeProjectTeam(@PathVariable Long id,
                                  @RequestParam Long newTeamId,
                                  @AuthenticationPrincipal OAuth2User principal,
                                  RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.getCurrentUser(principal);
            Project updatedProject = projectService.changeProjectTeam(id, newTeamId, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Project team changed successfully to " + updatedProject.getTeam().getName());
            return "redirect:/projects/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to change team: " + e.getMessage());
            return "redirect:/projects/" + id + "/change-team";
        }
    }

    /**
     * Process project update.
     */
    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('ROLE_TEAM_LEAD')")
    public String updateProject(@PathVariable Long id,
                               @Valid @ModelAttribute("projectDTO") ProjectDTO projectDTO,
                               BindingResult result,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        
        if (result.hasErrors()) {
            model.addAttribute("teams", teamService.getAllTeams());
            return "project/edit";
        }
        
        try {
            Project project = projectService.updateProject(id, projectDTO);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Project " + project.getName() + " updated successfully!");
            return "redirect:/projects/" + project.getId();
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error updating project: " + e.getMessage());
            model.addAttribute("teams", teamService.getAllTeams());
            return "project/edit";
        }
    }

    /**
     * Remove team from project.
     */
    @PostMapping("/{id}/remove-team")
    @PreAuthorize("hasAuthority('ROLE_TEAM_LEAD')")
    public String removeTeamFromProject(@PathVariable Long id,
                                      @AuthenticationPrincipal OAuth2User principal,
                                      RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.getCurrentUser(principal);
            Project project = projectService.removeTeamFromProject(id, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Team removed from project " + project.getName() + " successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error removing team: " + e.getMessage());
        }
        return "redirect:/projects/" + id;
    }
    
    /**
     * Delete project (soft delete) - Project creator or team leader only.
     */
    @PostMapping("/{id}/delete")
    public String deleteProject(@PathVariable Long id,
                               @AuthenticationPrincipal OAuth2User principal,
                               RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.getCurrentUser(principal);
            teamService.deleteProject(id, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Project deleted successfully");
            return "redirect:/projects";
        } catch (IllegalAccessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/projects/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete project: " + e.getMessage());
            return "redirect:/projects/" + id;
        }
    }
}

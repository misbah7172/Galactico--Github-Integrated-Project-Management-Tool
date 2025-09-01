package com.autotrack.controller;

import com.autotrack.dto.TeamDTO;
import com.autotrack.model.Project;
import com.autotrack.model.Team;
import com.autotrack.model.User;
import com.autotrack.service.TeamService;
import com.autotrack.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for team management.
 */
@Controller
@RequestMapping("/teams")
public class TeamController {

    private final TeamService teamService;
    private final UserService userService;

    public TeamController(TeamService teamService, UserService userService) {
        this.teamService = teamService;
        this.userService = userService;
    }

    /**
     * Show all teams.
     */
    @GetMapping
    public String listTeams(@AuthenticationPrincipal OAuth2User principal, Model model) {
        User user = userService.getCurrentUser(principal);
        List<Team> teams = teamService.getTeamsByUser(user);
        model.addAttribute("teams", teams);
        model.addAttribute("currentUser", user);
        return "team/list";
    }

    /**
     * Show team creation form.
     */
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("teamDTO", new TeamDTO());
        model.addAttribute("allUsers", userService.getAllUsers());
        return "team/create";
    }

    /**
     * Process team creation.
     */
    @PostMapping("/create")
    public String createTeam(@Valid @ModelAttribute("teamDTO") TeamDTO teamDTO,
                            BindingResult result,
                            @AuthenticationPrincipal OAuth2User principal,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        
        if (result.hasErrors()) {
            model.addAttribute("allUsers", userService.getAllUsers());
            return "team/create";
        }
        
        try {
            User currentUser = userService.getCurrentUser(principal);
            Team team = teamService.createTeam(teamDTO, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Team " + team.getName() + " created successfully! Now you can invite members.");
            return "redirect:/teams/create?teamId=" + team.getId();
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error creating team: " + e.getMessage());
            model.addAttribute("allUsers", userService.getAllUsers());
            return "team/create";
        }
    }

    /**
     * Show team details.
     */
    @GetMapping("/{id}")
    public String showTeam(@PathVariable Long id, @AuthenticationPrincipal OAuth2User principal, Model model) {
        Team team = teamService.getTeamById(id);
        User currentUser = userService.getCurrentUser(principal);
        model.addAttribute("team", team);
        model.addAttribute("currentUser", currentUser);
        return "team/detail";
    }

    /**
     * Show team edit form.
     */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Team team = teamService.getTeamById(id);
        TeamDTO teamDTO = new TeamDTO();
        teamDTO.setId(team.getId());
        teamDTO.setName(team.getName());
        teamDTO.setDescription(team.getDescription());
        teamDTO.setGithubOrganizationUrl(team.getGithubOrganizationUrl());
        
        List<Long> memberIds = team.getMembers().stream()
                .map(User::getId)
                .toList();
        teamDTO.setMemberIds(memberIds);
        
        model.addAttribute("teamDTO", teamDTO);
        model.addAttribute("teamMembers", team.getMembers());
        model.addAttribute("allUsers", userService.getAllUsers());
        return "team/edit";
    }

    /**
     * Process team update.
     */
    @PostMapping("/{id}/edit")
    public String updateTeam(@PathVariable Long id,
                            @Valid @ModelAttribute("teamDTO") TeamDTO teamDTO,
                            BindingResult result,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        
        if (result.hasErrors()) {
            model.addAttribute("allUsers", userService.getAllUsers());
            return "team/edit";
        }
        
        try {
            Team team = teamService.updateTeam(id, teamDTO);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Team " + team.getName() + " updated successfully!");
            return "redirect:/teams/" + team.getId();
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error updating team: " + e.getMessage());
            model.addAttribute("allUsers", userService.getAllUsers());
            return "team/edit";
        }
    }

    /**
     * Check team projects before deletion (AJAX endpoint)
     */
    @GetMapping("/{id}/check-projects")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkTeamProjects(@PathVariable Long id) {
        try {
            List<Project> projects = teamService.getTeamActiveProjects(id);
            Map<String, Object> response = new HashMap<>();
            response.put("hasProjects", !projects.isEmpty());
            response.put("projectCount", projects.size());
            response.put("projects", projects.stream().map(p -> 
                Map.of("id", p.getId(), "name", p.getName())
            ).toList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Delete team (soft delete) - Team leader only.
     */
    @PostMapping("/{id}/delete")
    public String deleteTeam(@PathVariable Long id,
                           @AuthenticationPrincipal OAuth2User principal,
                           RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.getCurrentUser(principal);
            teamService.deleteTeam(id, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Team deleted successfully");
            return "redirect:/teams";
        } catch (IllegalAccessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/teams/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete team: " + e.getMessage());
            return "redirect:/teams/" + id;
        }
    }
    
    /**
     * Leave team - Member leaves voluntarily.
     */
    @PostMapping("/{id}/leave")
    public String leaveTeam(@PathVariable Long id,
                          @AuthenticationPrincipal OAuth2User principal,
                          RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.getCurrentUser(principal);
            teamService.leaveTeam(id, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "You have left the team successfully");
            return "redirect:/teams";
        } catch (IllegalAccessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/teams/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to leave team: " + e.getMessage());
            return "redirect:/teams/" + id;
        }
    }
    
    /**
     * Kick member from team - Team leader only.
     */
    @PostMapping("/{teamId}/kick/{memberId}")
    public String kickMember(@PathVariable Long teamId,
                           @PathVariable Long memberId,
                           @RequestParam(required = false) String reason,
                           @AuthenticationPrincipal OAuth2User principal,
                           RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.getCurrentUser(principal);
            teamService.kickMember(teamId, memberId, currentUser, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Member removed from team successfully");
            return "redirect:/teams/" + teamId + "/members";
        } catch (IllegalAccessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/teams/" + teamId + "/members";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to remove member: " + e.getMessage());
            return "redirect:/teams/" + teamId + "/members";
        }
    }
    
    /**
     * Transfer team leadership - Current leader only.
     */
    @PostMapping("/{teamId}/transfer-leadership/{newLeaderId}")
    public String transferLeadership(@PathVariable Long teamId,
                                   @PathVariable Long newLeaderId,
                                   @AuthenticationPrincipal OAuth2User principal,
                                   RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.getCurrentUser(principal);
            teamService.transferLeadership(teamId, newLeaderId, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Team leadership transferred successfully");
            return "redirect:/teams/" + teamId;
        } catch (IllegalAccessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/teams/" + teamId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to transfer leadership: " + e.getMessage());
            return "redirect:/teams/" + teamId;
        }
    }

    /**
     * Show add members form for existing team.
     */
    @GetMapping("/{id}/add-members")
    public String showAddMembersForm(@PathVariable Long id, 
                                   @AuthenticationPrincipal OAuth2User principal, 
                                   Model model) {
        User currentUser = userService.getCurrentUser(principal);
        Team team = teamService.getTeamById(id);
        
        // Check if user is team owner
        if (!team.getOwner().getId().equals(currentUser.getId())) {
            model.addAttribute("errorMessage", "Only team owners can add members");
            return "redirect:/teams/" + id;
        }
        
        model.addAttribute("team", team);
        model.addAttribute("currentUser", currentUser);
        return "team/add-members";
    }
}

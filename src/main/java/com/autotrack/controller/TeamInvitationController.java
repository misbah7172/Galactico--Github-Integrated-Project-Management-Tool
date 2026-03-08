package com.autotrack.controller;

import com.autotrack.dto.TeamInvitationDTO;
import com.autotrack.model.User;
import com.autotrack.service.TeamInvitationService;
import com.autotrack.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/team-invitations")
public class TeamInvitationController {

    private final TeamInvitationService teamInvitationService;
    private final UserService userService;

    public TeamInvitationController(TeamInvitationService teamInvitationService, UserService userService) {
        this.teamInvitationService = teamInvitationService;
        this.userService = userService;
    }

    /**
     * Send team invitation via AJAX
     */
    @PostMapping("/send")
    @ResponseBody
    public String sendInvitation(@RequestParam Long teamId,
                               @RequestParam String githubUrl,
                               @RequestParam(required = false) String message,
                               @AuthenticationPrincipal OAuth2User principal) {
        try {
            User currentUser = getCurrentUser(principal);
            teamInvitationService.sendInvitation(teamId, githubUrl, message, currentUser);
            return "success";
        } catch (Exception e) {
            return "error: " + e.getMessage();
        }
    }

    /**
     * Accept team invitation
     */
    @PostMapping("/{id}/accept")
    public String acceptInvitation(@PathVariable Long id,
                                 @AuthenticationPrincipal OAuth2User principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser(principal);
            teamInvitationService.acceptInvitation(id, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Team invitation accepted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/notifications";
    }

    /**
     * Reject team invitation
     */
    @PostMapping("/{id}/reject")
    public String rejectInvitation(@PathVariable Long id,
                                 @AuthenticationPrincipal OAuth2User principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser(principal);
            teamInvitationService.rejectInvitation(id, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Team invitation rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/notifications";
    }

    /**
     * Get invitations for a team (AJAX)
     */
    @GetMapping("/team/{teamId}")
    @ResponseBody
    public List<TeamInvitationDTO> getTeamInvitations(@PathVariable Long teamId,
                                                     @AuthenticationPrincipal OAuth2User principal) {
        User currentUser = getCurrentUser(principal);
        return teamInvitationService.getInvitationsForTeam(teamId, currentUser);
    }

    /**
     * Get pending invitations for current user (AJAX)
     */
    @GetMapping("/pending")
    @ResponseBody
    public List<TeamInvitationDTO> getPendingInvitations(@AuthenticationPrincipal OAuth2User principal) {
        User currentUser = getCurrentUser(principal);
        return teamInvitationService.getPendingInvitationsForUser(currentUser);
    }

    /**
     * Get count of pending invitations for current user (AJAX)
     */
    @GetMapping("/pending/count")
    @ResponseBody
    public long getPendingInvitationsCount(@AuthenticationPrincipal OAuth2User principal) {
        User currentUser = getCurrentUser(principal);
        return teamInvitationService.countPendingInvitationsForUser(currentUser);
    }    /**
     * Helper method to get current user from OAuth2User
     */
    private User getCurrentUser(OAuth2User principal) {
        Object idAttribute = principal.getAttribute("id");
        if (idAttribute == null) {
            throw new RuntimeException("GitHub ID not found in user attributes");
        }
        String gitHubId = idAttribute.toString();
        return userService.getUserByGitHubId(gitHubId);
    }
}

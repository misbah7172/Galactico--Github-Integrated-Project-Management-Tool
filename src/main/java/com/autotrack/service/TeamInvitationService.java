package com.autotrack.service;

import com.autotrack.dto.TeamInvitationDTO;
import com.autotrack.model.Team;
import com.autotrack.model.TeamInvitation;
import com.autotrack.model.User;
import com.autotrack.repository.TeamInvitationRepository;
import com.autotrack.repository.TeamRepository;
import com.autotrack.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class TeamInvitationService {    private final TeamInvitationRepository teamInvitationRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public TeamInvitationService(TeamInvitationRepository teamInvitationRepository,
                               TeamRepository teamRepository,
                               UserRepository userRepository,
                               NotificationService notificationService) {
        this.teamInvitationRepository = teamInvitationRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * Send a team invitation to a user by their GitHub URL
     */
    public TeamInvitationDTO sendInvitation(Long teamId, String inviteeGithubUrl, String message, User inviter) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        // Check if user is team owner or member
        if (!isUserAuthorizedToInvite(team, inviter)) {
            throw new RuntimeException("You are not authorized to send invitations for this team");
        }

        // Normalize GitHub URL
        String normalizedGithubUrl = normalizeGithubUrl(inviteeGithubUrl);

        // Check if invitation already exists
        Optional<TeamInvitation> existingInvitation = teamInvitationRepository
                .findPendingInvitationByTeamAndGithubUrl(teamId, normalizedGithubUrl);
        
        if (existingInvitation.isPresent()) {
            throw new RuntimeException("Invitation already sent to this user for this team");
        }

        // Create invitation
        TeamInvitation invitation = new TeamInvitation(team, inviter, normalizedGithubUrl, message);
        
        // Check if the user is already registered
        Optional<User> existingUser = findUserByGithubUrl(normalizedGithubUrl);
        if (existingUser.isPresent()) {
            invitation.setInvitee(existingUser.get());
            // Create notification for the user
            notificationService.createTeamInvitationNotification(existingUser.get(), invitation);
        }

        invitation = teamInvitationRepository.save(invitation);
        return new TeamInvitationDTO(invitation);
    }

    /**
     * Get all pending invitations for a user
     */
    public List<TeamInvitationDTO> getPendingInvitationsForUser(User user) {
        String githubUrl = user.getGithubUrl();
        List<TeamInvitation> invitations = teamInvitationRepository
                .findPendingInvitationsForUser(user, githubUrl);
        
        return invitations.stream()
                .map(TeamInvitationDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Accept a team invitation
     */
    public TeamInvitationDTO acceptInvitation(Long invitationId, User user) {
        TeamInvitation invitation = teamInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        // Verify user is the intended recipient
        if (!isUserTheInvitee(invitation, user)) {
            throw new RuntimeException("You are not authorized to respond to this invitation");
        }

        if (!invitation.isPending()) {
            throw new RuntimeException("This invitation has already been responded to");
        }

        // Accept the invitation
        invitation.setStatus(TeamInvitation.InvitationStatus.ACCEPTED);
        invitation.setInvitee(user);

        // Add user to team
        Team team = invitation.getTeam();
        if (!team.getMembers().contains(user)) {
            team.getMembers().add(user);
            user.getTeams().add(team);
            teamRepository.save(team);
        }

        invitation = teamInvitationRepository.save(invitation);

        // Create notification for team owner
        notificationService.createInvitationResponseNotification(
                invitation.getInviter(), invitation, true);

        return new TeamInvitationDTO(invitation);
    }

    /**
     * Reject a team invitation
     */
    public TeamInvitationDTO rejectInvitation(Long invitationId, User user) {
        TeamInvitation invitation = teamInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        // Verify user is the intended recipient
        if (!isUserTheInvitee(invitation, user)) {
            throw new RuntimeException("You are not authorized to respond to this invitation");
        }

        if (!invitation.isPending()) {
            throw new RuntimeException("This invitation has already been responded to");
        }

        // Reject the invitation
        invitation.setStatus(TeamInvitation.InvitationStatus.REJECTED);
        invitation.setInvitee(user);

        invitation = teamInvitationRepository.save(invitation);

        // Create notification for team owner
        notificationService.createInvitationResponseNotification(
                invitation.getInviter(), invitation, false);

        return new TeamInvitationDTO(invitation);
    }

    /**
     * Get all invitations sent by a user
     */
    public List<TeamInvitationDTO> getInvitationsSentByUser(User user) {
        List<TeamInvitation> invitations = teamInvitationRepository.findByInviter(user);
        return invitations.stream()
                .map(TeamInvitationDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Get all invitations for a specific team
     */
    public List<TeamInvitationDTO> getInvitationsForTeam(Long teamId, User requester) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));

        if (!isUserAuthorizedToViewInvitations(team, requester)) {
            throw new RuntimeException("You are not authorized to view invitations for this team");
        }

        List<TeamInvitation> invitations = teamInvitationRepository.findByTeamId(teamId);
        return invitations.stream()
                .map(TeamInvitationDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Count pending invitations for a user
     */
    public long countPendingInvitationsForUser(User user) {
        String githubUrl = user.getGithubUrl();
        return teamInvitationRepository.countPendingInvitationsForUser(user, githubUrl);
    }

    /**
     * Link pending invitations to a user when they register
     */
    public void linkInvitationsToNewUser(User user) {
        String githubUrl = user.getGithubUrl();
        List<TeamInvitation> pendingInvitations = teamInvitationRepository
                .findByInviteeGithubUrlAndInviteeIsNull(githubUrl);

        for (TeamInvitation invitation : pendingInvitations) {
            invitation.setInvitee(user);
            teamInvitationRepository.save(invitation);
            
            // Create notification for the newly registered user
            notificationService.createTeamInvitationNotification(user, invitation);
        }
    }

    // Helper methods

    private boolean isUserAuthorizedToInvite(Team team, User user) {
        return team.getOwner().equals(user) || team.getMembers().contains(user);
    }

    private boolean isUserAuthorizedToViewInvitations(Team team, User user) {
        return team.getOwner().equals(user) || team.getMembers().contains(user);
    }

    private boolean isUserTheInvitee(TeamInvitation invitation, User user) {
        return invitation.getInvitee() != null && invitation.getInvitee().equals(user) ||
               invitation.getInviteeGithubUrl().equals(user.getGithubUrl());
    }

    private String normalizeGithubUrl(String githubUrl) {
        if (githubUrl == null || githubUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("GitHub URL cannot be empty");
        }
        
        githubUrl = githubUrl.trim();
        
        // If it's just a username, convert to full URL
        if (!githubUrl.startsWith("http")) {
            githubUrl = "https://github.com/" + githubUrl;
        }
        
        // Ensure it's a valid GitHub URL
        if (!githubUrl.startsWith("https://github.com/")) {
            throw new IllegalArgumentException("Invalid GitHub URL format");
        }
        
        return githubUrl;
    }

    private Optional<User> findUserByGithubUrl(String githubUrl) {
        // Extract username from GitHub URL
        String username = githubUrl.replace("https://github.com/", "");
        return userRepository.findByNickname(username);
    }
}

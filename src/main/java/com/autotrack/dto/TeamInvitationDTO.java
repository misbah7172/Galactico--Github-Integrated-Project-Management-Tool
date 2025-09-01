package com.autotrack.dto;

import com.autotrack.model.TeamInvitation;
import java.time.LocalDateTime;

/**
 * DTO for team invitation data transfer.
 */
public class TeamInvitationDTO {
    
    private Long id;
    private Long teamId;
    private String teamName;
    private String inviterName;
    private String inviterGithubUrl;
    private String inviteeGithubUrl;
    private String inviteeName;
    private String status;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    // Constructors
    public TeamInvitationDTO() {}

    public TeamInvitationDTO(TeamInvitation invitation) {
        this.id = invitation.getId();
        this.teamId = invitation.getTeam().getId();
        this.teamName = invitation.getTeam().getName();
        this.inviterName = invitation.getInviter().getNickname();
        this.inviterGithubUrl = invitation.getInviter().getGithubUrl();
        this.inviteeGithubUrl = invitation.getInviteeGithubUrl();
        this.inviteeName = invitation.getInvitee() != null ? invitation.getInvitee().getNickname() : null;
        this.status = invitation.getStatus().name();
        this.message = invitation.getMessage();
        this.createdAt = invitation.getCreatedAt();
        this.respondedAt = invitation.getRespondedAt();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getInviterName() {
        return inviterName;
    }

    public void setInviterName(String inviterName) {
        this.inviterName = inviterName;
    }

    public String getInviterGithubUrl() {
        return inviterGithubUrl;
    }

    public void setInviterGithubUrl(String inviterGithubUrl) {
        this.inviterGithubUrl = inviterGithubUrl;
    }

    public String getInviteeGithubUrl() {
        return inviteeGithubUrl;
    }

    public void setInviteeGithubUrl(String inviteeGithubUrl) {
        this.inviteeGithubUrl = inviteeGithubUrl;
    }

    public String getInviteeName() {
        return inviteeName;
    }

    public void setInviteeName(String inviteeName) {
        this.inviteeName = inviteeName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    // Helper methods
    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public boolean isAccepted() {
        return "ACCEPTED".equals(status);
    }

    public boolean isRejected() {
        return "REJECTED".equals(status);
    }
}

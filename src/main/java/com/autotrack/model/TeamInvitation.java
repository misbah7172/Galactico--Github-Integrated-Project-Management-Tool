package com.autotrack.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a team invitation.
 */
@Entity
@Table(name = "team_invitations")
public class TeamInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;
    
    @ManyToOne
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter; // The user who sent the invitation
    
    @Column(name = "invitee_github_url", nullable = false)
    private String inviteeGithubUrl; // GitHub profile URL of invitee
    
    @ManyToOne
    @JoinColumn(name = "invitee_id")
    private User invitee; // The user who received the invitation (null until they register)
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvitationStatus status = InvitationStatus.PENDING;
    
    @Column(name = "message")
    private String message; // Optional invitation message
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    // Constructors
    public TeamInvitation() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public TeamInvitation(Team team, User inviter, String inviteeGithubUrl, String message) {
        this();
        this.team = team;
        this.inviter = inviter;
        this.inviteeGithubUrl = inviteeGithubUrl;
        this.message = message;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public User getInviter() {
        return inviter;
    }

    public void setInviter(User inviter) {
        this.inviter = inviter;
    }

    public String getInviteeGithubUrl() {
        return inviteeGithubUrl;
    }

    public void setInviteeGithubUrl(String inviteeGithubUrl) {
        this.inviteeGithubUrl = inviteeGithubUrl;
    }

    public User getInvitee() {
        return invitee;
    }

    public void setInvitee(User invitee) {
        this.invitee = invitee;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public void setStatus(InvitationStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
        if (status != InvitationStatus.PENDING) {
            this.respondedAt = LocalDateTime.now();
        }
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    // Helper methods
    public boolean isPending() {
        return status == InvitationStatus.PENDING;
    }

    public boolean isAccepted() {
        return status == InvitationStatus.ACCEPTED;
    }

    public boolean isRejected() {
        return status == InvitationStatus.REJECTED;
    }

    public enum InvitationStatus {
        PENDING,
        ACCEPTED,
        REJECTED
    }
}

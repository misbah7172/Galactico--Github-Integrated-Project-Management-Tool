package com.autotrack.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a team member removal (kick or leave).
 */
@Entity
@Table(name = "team_member_removals")
public class TeamMemberRemoval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "removed_by_id", nullable = false)
    private User removedBy;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "removal_type", nullable = false)
    private RemovalType removalType;
    
    @Column(name = "removal_reason", columnDefinition = "TEXT")
    private String removalReason;
    
    @Column(name = "removed_at", nullable = false)
    private LocalDateTime removedAt;
    
    @Column(name = "contributions_removed", nullable = false)
    private Boolean contributionsRemoved = false;

    // Constructors
    public TeamMemberRemoval() {}

    public TeamMemberRemoval(Team team, User user, User removedBy, RemovalType removalType, 
                           String removalReason, LocalDateTime removedAt) {
        this.team = team;
        this.user = user;
        this.removedBy = removedBy;
        this.removalType = removalType;
        this.removalReason = removalReason;
        this.removedAt = removedAt;
        this.contributionsRemoved = false;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public User getRemovedBy() { return removedBy; }
    public void setRemovedBy(User removedBy) { this.removedBy = removedBy; }

    public RemovalType getRemovalType() { return removalType; }
    public void setRemovalType(RemovalType removalType) { this.removalType = removalType; }

    public String getRemovalReason() { return removalReason; }
    public void setRemovalReason(String removalReason) { this.removalReason = removalReason; }

    public LocalDateTime getRemovedAt() { return removedAt; }
    public void setRemovedAt(LocalDateTime removedAt) { this.removedAt = removedAt; }

    public Boolean getContributionsRemoved() { return contributionsRemoved; }
    public void setContributionsRemoved(Boolean contributionsRemoved) { this.contributionsRemoved = contributionsRemoved; }

    // Builder pattern
    public static TeamMemberRemovalBuilder builder() {
        return new TeamMemberRemovalBuilder();
    }

    public static class TeamMemberRemovalBuilder {
        private Team team;
        private User user;
        private User removedBy;
        private RemovalType removalType;
        private String removalReason;
        private LocalDateTime removedAt;

        public TeamMemberRemovalBuilder team(Team team) { this.team = team; return this; }
        public TeamMemberRemovalBuilder user(User user) { this.user = user; return this; }
        public TeamMemberRemovalBuilder removedBy(User removedBy) { this.removedBy = removedBy; return this; }
        public TeamMemberRemovalBuilder removalType(RemovalType removalType) { this.removalType = removalType; return this; }
        public TeamMemberRemovalBuilder removalReason(String removalReason) { this.removalReason = removalReason; return this; }
        public TeamMemberRemovalBuilder removedAt(LocalDateTime removedAt) { this.removedAt = removedAt; return this; }

        public TeamMemberRemoval build() {
            TeamMemberRemoval removal = new TeamMemberRemoval();
            removal.team = this.team;
            removal.user = this.user;
            removal.removedBy = this.removedBy;
            removal.removalType = this.removalType;
            removal.removalReason = this.removalReason;
            removal.removedAt = this.removedAt != null ? this.removedAt : LocalDateTime.now();
            removal.contributionsRemoved = false;
            return removal;
        }
    }

    @PrePersist
    protected void onCreate() {
        if (removedAt == null) {
            removedAt = LocalDateTime.now();
        }
    }
}

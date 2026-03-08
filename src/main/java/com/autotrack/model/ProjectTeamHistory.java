package com.autotrack.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing the history of team changes for a project
 */
@Entity
@Table(name = "project_team_history")
public class ProjectTeamHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_team_id")
    private Team previousTeam;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_team_id", nullable = false)
    private Team newTeam;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id", nullable = false)
    private User changedBy;
    
    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;
    
    @Column(name = "change_reason")
    private String changeReason;
    
    // Constructors
    public ProjectTeamHistory() {}
    
    public ProjectTeamHistory(Project project, Team previousTeam, Team newTeam, 
                             User changedBy, LocalDateTime changedAt, String changeReason) {
        this.project = project;
        this.previousTeam = previousTeam;
        this.newTeam = newTeam;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
        this.changeReason = changeReason;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    
    public Team getPreviousTeam() { return previousTeam; }
    public void setPreviousTeam(Team previousTeam) { this.previousTeam = previousTeam; }
    
    public Team getNewTeam() { return newTeam; }
    public void setNewTeam(Team newTeam) { this.newTeam = newTeam; }
    
    public User getChangedBy() { return changedBy; }
    public void setChangedBy(User changedBy) { this.changedBy = changedBy; }
    
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
    
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
}

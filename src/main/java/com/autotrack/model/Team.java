package com.autotrack.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a team.
 */
@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "github_organization_url")
    private String githubOrganizationUrl;
    
    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    
    @ManyToMany
    @JoinTable(
        name = "team_members",
        joinColumns = @JoinColumn(name = "team_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> members = new ArrayList<>();
    
    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Project> projects = new ArrayList<>();
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Deletion tracking fields
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @ManyToOne
    @JoinColumn(name = "deleted_by_id")
    private User deletedBy;

    // Constructors
    public Team() {}

    public Team(Long id, String name, String description, String githubOrganizationUrl, User owner, List<User> members, List<Project> projects, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.githubOrganizationUrl = githubOrganizationUrl;
        this.owner = owner;
        this.members = members != null ? members : new ArrayList<>();
        this.projects = projects != null ? projects : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getGithubOrganizationUrl() { return githubOrganizationUrl; }
    public void setGithubOrganizationUrl(String githubOrganizationUrl) { this.githubOrganizationUrl = githubOrganizationUrl; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public List<User> getMembers() { return members; }
    public void setMembers(List<User> members) { this.members = members; }

    public List<Project> getProjects() { return projects; }
    public void setProjects(List<Project> projects) { this.projects = projects; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Deletion tracking getters and setters
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public User getDeletedBy() { return deletedBy; }
    public void setDeletedBy(User deletedBy) { this.deletedBy = deletedBy; }

    // Utility methods
    public boolean isDeleted() { return deletedAt != null; }
    
    public void setDeleted(boolean deleted) {
        if (deleted) {
            this.deletedAt = LocalDateTime.now();
        } else {
            this.deletedAt = null;
            this.deletedBy = null;
        }
    }
    
    // Get team leader (same as owner)
    public User getLeader() { return owner; }
    public void setLeader(User leader) { this.owner = leader; }

    // Builder pattern
    public static TeamBuilder builder() {
        return new TeamBuilder();
    }

    public static class TeamBuilder {
        private Long id;
        private String name;
        private String description;
        private String githubOrganizationUrl;
        private User owner;
        private List<User> members = new ArrayList<>();
        private List<Project> projects = new ArrayList<>();
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public TeamBuilder id(Long id) { this.id = id; return this; }
        public TeamBuilder name(String name) { this.name = name; return this; }
        public TeamBuilder description(String description) { this.description = description; return this; }
        public TeamBuilder githubOrganizationUrl(String githubOrganizationUrl) { this.githubOrganizationUrl = githubOrganizationUrl; return this; }
        public TeamBuilder owner(User owner) { this.owner = owner; return this; }
        public TeamBuilder members(List<User> members) { this.members = members; return this; }
        public TeamBuilder projects(List<Project> projects) { this.projects = projects; return this; }
        public TeamBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public TeamBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Team build() {
            return new Team(id, name, description, githubOrganizationUrl, owner, members, projects, createdAt, updatedAt);
        }
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

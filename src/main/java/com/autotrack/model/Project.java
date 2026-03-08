package com.autotrack.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a project.
 */
@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "github_repo_url", nullable = false)
    private String gitHubRepoUrl;
    
    @Column(name = "github_repo_id")
    private String gitHubRepoId;
    
    @Column(name = "github_access_token")
    private String gitHubAccessToken;
    
    @Column(name = "webhook_secret")
    private String webhookSecret;
    
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
    
    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    
    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;
    
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks = new ArrayList<>();
    
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Commit> commits = new ArrayList<>();

    // Constructors
    public Project() {}

    public Project(Long id, String name, String gitHubRepoUrl, String gitHubRepoId, String gitHubAccessToken,
                   String webhookSecret, LocalDateTime createdAt, LocalDateTime updatedAt, User owner, Team team,
                   List<Task> tasks, List<Commit> commits) {
        this.id = id;
        this.name = name;
        this.gitHubRepoUrl = gitHubRepoUrl;
        this.gitHubRepoId = gitHubRepoId;
        this.gitHubAccessToken = gitHubAccessToken;
        this.webhookSecret = webhookSecret;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.owner = owner;
        this.team = team;
        this.tasks = tasks != null ? tasks : new ArrayList<>();
        this.commits = commits != null ? commits : new ArrayList<>();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGitHubRepoUrl() { return gitHubRepoUrl; }
    public void setGitHubRepoUrl(String gitHubRepoUrl) { this.gitHubRepoUrl = gitHubRepoUrl; }

    public String getGitHubRepoId() { return gitHubRepoId; }
    public void setGitHubRepoId(String gitHubRepoId) { this.gitHubRepoId = gitHubRepoId; }

    public String getGitHubAccessToken() { return gitHubAccessToken; }
    public void setGitHubAccessToken(String gitHubAccessToken) { this.gitHubAccessToken = gitHubAccessToken; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    public List<Task> getTasks() { return tasks; }
    public void setTasks(List<Task> tasks) { this.tasks = tasks; }

    public List<Commit> getCommits() { return commits; }
    public void setCommits(List<Commit> commits) { this.commits = commits; }

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
    
    // Get project creator (same as owner)
    public User getCreatedBy() { return owner; }

    // Builder pattern
    public static ProjectBuilder builder() {
        return new ProjectBuilder();
    }

    public static class ProjectBuilder {
        private Long id;
        private String name;
        private String gitHubRepoUrl;
        private String gitHubRepoId;
        private String gitHubAccessToken;
        private String webhookSecret;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private User owner;
        private Team team;
        private List<Task> tasks = new ArrayList<>();
        private List<Commit> commits = new ArrayList<>();

        public ProjectBuilder id(Long id) { this.id = id; return this; }
        public ProjectBuilder name(String name) { this.name = name; return this; }
        public ProjectBuilder gitHubRepoUrl(String gitHubRepoUrl) { this.gitHubRepoUrl = gitHubRepoUrl; return this; }
        public ProjectBuilder gitHubRepoId(String gitHubRepoId) { this.gitHubRepoId = gitHubRepoId; return this; }
        public ProjectBuilder gitHubAccessToken(String gitHubAccessToken) { this.gitHubAccessToken = gitHubAccessToken; return this; }
        public ProjectBuilder webhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; return this; }
        public ProjectBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public ProjectBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public ProjectBuilder owner(User owner) { this.owner = owner; return this; }
        public ProjectBuilder team(Team team) { this.team = team; return this; }
        public ProjectBuilder tasks(List<Task> tasks) { this.tasks = tasks; return this; }
        public ProjectBuilder commits(List<Commit> commits) { this.commits = commits; return this; }

        public Project build() {
            return new Project(id, name, gitHubRepoUrl, gitHubRepoId, gitHubAccessToken, webhookSecret,
                             createdAt, updatedAt, owner, team, tasks, commits);
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

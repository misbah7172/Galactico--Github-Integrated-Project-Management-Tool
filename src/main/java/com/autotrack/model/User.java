package com.autotrack.model;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Entity representing a user.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String nickname;
    
    @Column(unique = true)
    private String email;
    
    @Column(name = "github_id", nullable = false, unique = true)
    private String gitHubId;
    
    @Column(name = "avatar_url")
    private String avatarUrl;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private List<Role> roles = new ArrayList<>();
    
    @ManyToMany(mappedBy = "members")
    private List<Team> teams = new ArrayList<>();
    
    @OneToMany(mappedBy = "assignee", cascade = CascadeType.ALL)
    private List<Task> assignedTasks = new ArrayList<>();
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public User() {}

    public User(Long id, String nickname, String email, String gitHubId, String avatarUrl, 
                List<Role> roles, List<Team> teams, List<Task> assignedTasks) {
        this.id = id;
        this.nickname = nickname;
        this.email = email;
        this.gitHubId = gitHubId;
        this.avatarUrl = avatarUrl;
        this.roles = roles != null ? roles : new ArrayList<>();
        this.teams = teams != null ? teams : new ArrayList<>();
        this.assignedTasks = assignedTasks != null ? assignedTasks : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getGitHubId() { return gitHubId; }
    public void setGitHubId(String gitHubId) { this.gitHubId = gitHubId; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public List<Role> getRoles() { return roles; }
    public void setRoles(List<Role> roles) { this.roles = roles; }

    public List<Team> getTeams() { return teams; }
    public void setTeams(List<Team> teams) { this.teams = teams; }

    public List<Task> getAssignedTasks() { return assignedTasks; }
    public void setAssignedTasks(List<Task> assignedTasks) { this.assignedTasks = assignedTasks; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Builder pattern
    public static UserBuilder builder() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private Long id;
        private String nickname;
        private String email;
        private String gitHubId;
        private String avatarUrl;
        private List<Role> roles = new ArrayList<>();
        private List<Team> teams = new ArrayList<>();
        private List<Task> assignedTasks = new ArrayList<>();
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public UserBuilder id(Long id) { this.id = id; return this; }
        public UserBuilder nickname(String nickname) { this.nickname = nickname; return this; }
        public UserBuilder email(String email) { this.email = email; return this; }
        public UserBuilder gitHubId(String gitHubId) { this.gitHubId = gitHubId; return this; }
        public UserBuilder avatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; return this; }
        public UserBuilder roles(List<Role> roles) { this.roles = roles; return this; }
        public UserBuilder teams(List<Team> teams) { this.teams = teams; return this; }
        public UserBuilder assignedTasks(List<Task> assignedTasks) { this.assignedTasks = assignedTasks; return this; }
        public UserBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public UserBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
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
    
    /**
     * Get user authorities for Spring Security.
     */
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .collect(Collectors.toList());
    }
    
    /**
     * Check if user has a specific role.
     */
    public boolean hasRole(String roleName) {
        return roles.stream()
                .anyMatch(role -> role.name().equals(roleName));
    }
    
    /**
     * Get GitHub profile URL for this user.
     */
    public String getGithubUrl() {
        return "https://github.com/" + nickname;
    }
    
    /**
     * Get full name (currently same as nickname since we don't store separate first/last names).
     */
    public String getFullName() {
        return nickname != null ? nickname : "Unknown User";
    }
}

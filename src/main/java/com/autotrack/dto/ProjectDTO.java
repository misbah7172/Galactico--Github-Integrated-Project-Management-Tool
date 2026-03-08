package com.autotrack.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Data transfer object for Project.
 */
public class ProjectDTO {
    
    private Long id;
    
    @NotBlank(message = "Project name is required")
    private String name;
    
    @NotBlank(message = "GitHub repository URL is required")
    private String gitHubRepoUrl;
    
    private String gitHubRepoId;
    
    private String gitHubAccessToken;
    
    private Long teamId; // Optional - can be assigned later

    // Constructors
    public ProjectDTO() {}

    public ProjectDTO(Long id, String name, String gitHubRepoUrl, String gitHubRepoId, String gitHubAccessToken, Long teamId) {
        this.id = id;
        this.name = name;
        this.gitHubRepoUrl = gitHubRepoUrl;
        this.gitHubRepoId = gitHubRepoId;
        this.gitHubAccessToken = gitHubAccessToken;
        this.teamId = teamId;
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

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
}

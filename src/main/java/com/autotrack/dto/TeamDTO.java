package com.autotrack.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Data transfer object for Team.
 */
public class TeamDTO {
    
    private Long id;
    
    @NotBlank(message = "Team name is required")
    private String name;
    
    private String description;
    
    private String githubOrganizationUrl;
    
    private List<Long> memberIds;

    // Constructors
    public TeamDTO() {}

    public TeamDTO(Long id, String name, String description, String githubOrganizationUrl, List<Long> memberIds) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.githubOrganizationUrl = githubOrganizationUrl;
        this.memberIds = memberIds;
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

    public List<Long> getMemberIds() { return memberIds; }
    public void setMemberIds(List<Long> memberIds) { this.memberIds = memberIds; }
}

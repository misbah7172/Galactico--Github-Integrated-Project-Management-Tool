package com.autotrack.dto;

/**
 * DTO for team member contribution statistics.
 */
public class TeamMemberContributionDTO {
    
    private Long userId;
    private String memberName;
    private String githubUsername;
    private String avatarUrl;
    private int totalCommits;
    private int totalTasks;
    private int completedTasks;
    private double contributionPercentage;
    
    // Constructors
    public TeamMemberContributionDTO() {}
    
    public TeamMemberContributionDTO(Long userId, String memberName, String githubUsername, String avatarUrl, 
                                   int totalCommits, int totalTasks, int completedTasks, double contributionPercentage) {
        this.userId = userId;
        this.memberName = memberName;
        this.githubUsername = githubUsername;
        this.avatarUrl = avatarUrl;
        this.totalCommits = totalCommits;
        this.totalTasks = totalTasks;
        this.completedTasks = completedTasks;
        this.contributionPercentage = contributionPercentage;
    }
    
    // Getters and Setters
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getMemberName() {
        return memberName;
    }
    
    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }
    
    public String getGithubUsername() {
        return githubUsername;
    }
    
    public void setGithubUsername(String githubUsername) {
        this.githubUsername = githubUsername;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
    
    public int getTotalCommits() {
        return totalCommits;
    }
    
    public void setTotalCommits(int totalCommits) {
        this.totalCommits = totalCommits;
    }
    
    public int getTotalTasks() {
        return totalTasks;
    }
    
    public void setTotalTasks(int totalTasks) {
        this.totalTasks = totalTasks;
    }
    
    public int getCompletedTasks() {
        return completedTasks;
    }
    
    public void setCompletedTasks(int completedTasks) {
        this.completedTasks = completedTasks;
    }
    
    public double getContributionPercentage() {
        return contributionPercentage;
    }
    
    public void setContributionPercentage(double contributionPercentage) {
        this.contributionPercentage = contributionPercentage;
    }
    
    /**
     * Get task completion percentage for this member.
     */
    public double getTaskCompletionPercentage() {
        if (totalTasks == 0) {
            return 0.0;
        }
        return (double) completedTasks / totalTasks * 100.0;
    }
}

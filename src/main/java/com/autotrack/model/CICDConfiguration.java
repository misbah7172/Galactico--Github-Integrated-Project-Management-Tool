package com.autotrack.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entity representing CI/CD configuration for a project.
 */
@Entity
@Table(name = "cicd_configurations")
public class CICDConfiguration {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    @Column(name = "project_type", nullable = false)
    private String projectType;
    
    @Column(name = "test_command")
    private String testCommand;
    
    @Column(name = "build_command")
    private String buildCommand;
    
    @Column(name = "deploy_strategy", nullable = false)
    private String deployStrategy;
    
    @Column(name = "pipeline_content", columnDefinition = "TEXT")
    private String pipelineContent;
    
    @Column(name = "configuration_json", columnDefinition = "TEXT")
    private String configurationJson;
    
    @Column(name = "local_pipeline_path")
    private String localPipelinePath;
    
    @Column(name = "local_config_path")
    private String localConfigPath;
    
    @Column(name = "github_pipeline_path")
    private String githubPipelinePath;
    
    @Column(name = "github_config_path")
    private String githubConfigPath;
    
    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "last_pipeline_run")
    private LocalDateTime lastPipelineRun;
    
    @Column(name = "last_pipeline_status")
    private String lastPipelineStatus; // success, failure, running, pending
    
    @Column(name = "pipeline_run_count")
    private Integer pipelineRunCount = 0;

    // Constructors
    public CICDConfiguration() {}
    
    public CICDConfiguration(Long id, Project project, String projectType, String testCommand, 
                           String buildCommand, String deployStrategy, String pipelineContent,
                           String configurationJson, String localPipelinePath, String localConfigPath,
                           String githubPipelinePath, String githubConfigPath, LocalDateTime generatedAt,
                           LocalDateTime updatedAt, Boolean isActive, LocalDateTime lastPipelineRun,
                           String lastPipelineStatus, Integer pipelineRunCount) {
        this.id = id;
        this.project = project;
        this.projectType = projectType;
        this.testCommand = testCommand;
        this.buildCommand = buildCommand;
        this.deployStrategy = deployStrategy;
        this.pipelineContent = pipelineContent;
        this.configurationJson = configurationJson;
        this.localPipelinePath = localPipelinePath;
        this.localConfigPath = localConfigPath;
        this.githubPipelinePath = githubPipelinePath;
        this.githubConfigPath = githubConfigPath;
        this.generatedAt = generatedAt;
        this.updatedAt = updatedAt;
        this.isActive = isActive;
        this.lastPipelineRun = lastPipelineRun;
        this.lastPipelineStatus = lastPipelineStatus;
        this.pipelineRunCount = pipelineRunCount;
    }

    // Builder pattern
    public static CICDConfigurationBuilder builder() {
        return new CICDConfigurationBuilder();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    public String getTestCommand() {
        return testCommand;
    }

    public void setTestCommand(String testCommand) {
        this.testCommand = testCommand;
    }

    public String getBuildCommand() {
        return buildCommand;
    }

    public void setBuildCommand(String buildCommand) {
        this.buildCommand = buildCommand;
    }

    public String getDeployStrategy() {
        return deployStrategy;
    }

    public void setDeployStrategy(String deployStrategy) {
        this.deployStrategy = deployStrategy;
    }

    public String getPipelineContent() {
        return pipelineContent;
    }

    public void setPipelineContent(String pipelineContent) {
        this.pipelineContent = pipelineContent;
    }

    public String getConfigurationJson() {
        return configurationJson;
    }

    public void setConfigurationJson(String configurationJson) {
        this.configurationJson = configurationJson;
    }

    public String getLocalPipelinePath() {
        return localPipelinePath;
    }

    public void setLocalPipelinePath(String localPipelinePath) {
        this.localPipelinePath = localPipelinePath;
    }

    public String getLocalConfigPath() {
        return localConfigPath;
    }

    public void setLocalConfigPath(String localConfigPath) {
        this.localConfigPath = localConfigPath;
    }

    public String getGithubPipelinePath() {
        return githubPipelinePath;
    }

    public void setGithubPipelinePath(String githubPipelinePath) {
        this.githubPipelinePath = githubPipelinePath;
    }

    public String getGithubConfigPath() {
        return githubConfigPath;
    }

    public void setGithubConfigPath(String githubConfigPath) {
        this.githubConfigPath = githubConfigPath;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getLastPipelineRun() {
        return lastPipelineRun;
    }

    public void setLastPipelineRun(LocalDateTime lastPipelineRun) {
        this.lastPipelineRun = lastPipelineRun;
    }

    public String getLastPipelineStatus() {
        return lastPipelineStatus;
    }

    public void setLastPipelineStatus(String lastPipelineStatus) {
        this.lastPipelineStatus = lastPipelineStatus;
    }

    public Integer getPipelineRunCount() {
        return pipelineRunCount;
    }

    public void setPipelineRunCount(Integer pipelineRunCount) {
        this.pipelineRunCount = pipelineRunCount;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    @PrePersist
    public void prePersist() {
        if (this.generatedAt == null) {
            this.generatedAt = LocalDateTime.now();
        }
    }

    // Builder class
    public static class CICDConfigurationBuilder {
        private Long id;
        private Project project;
        private String projectType;
        private String testCommand;
        private String buildCommand;
        private String deployStrategy;
        private String pipelineContent;
        private String configurationJson;
        private String localPipelinePath;
        private String localConfigPath;
        private String githubPipelinePath;
        private String githubConfigPath;
        private LocalDateTime generatedAt;
        private LocalDateTime updatedAt;
        private Boolean isActive = true;
        private LocalDateTime lastPipelineRun;
        private String lastPipelineStatus;
        private Integer pipelineRunCount = 0;

        public CICDConfigurationBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public CICDConfigurationBuilder project(Project project) {
            this.project = project;
            return this;
        }

        public CICDConfigurationBuilder projectType(String projectType) {
            this.projectType = projectType;
            return this;
        }

        public CICDConfigurationBuilder testCommand(String testCommand) {
            this.testCommand = testCommand;
            return this;
        }

        public CICDConfigurationBuilder buildCommand(String buildCommand) {
            this.buildCommand = buildCommand;
            return this;
        }

        public CICDConfigurationBuilder deployStrategy(String deployStrategy) {
            this.deployStrategy = deployStrategy;
            return this;
        }

        public CICDConfigurationBuilder pipelineContent(String pipelineContent) {
            this.pipelineContent = pipelineContent;
            return this;
        }

        public CICDConfigurationBuilder configurationJson(String configurationJson) {
            this.configurationJson = configurationJson;
            return this;
        }

        public CICDConfigurationBuilder localPipelinePath(String localPipelinePath) {
            this.localPipelinePath = localPipelinePath;
            return this;
        }

        public CICDConfigurationBuilder localConfigPath(String localConfigPath) {
            this.localConfigPath = localConfigPath;
            return this;
        }

        public CICDConfigurationBuilder githubPipelinePath(String githubPipelinePath) {
            this.githubPipelinePath = githubPipelinePath;
            return this;
        }

        public CICDConfigurationBuilder githubConfigPath(String githubConfigPath) {
            this.githubConfigPath = githubConfigPath;
            return this;
        }

        public CICDConfigurationBuilder generatedAt(LocalDateTime generatedAt) {
            this.generatedAt = generatedAt;
            return this;
        }

        public CICDConfigurationBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public CICDConfigurationBuilder isActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public CICDConfigurationBuilder lastPipelineRun(LocalDateTime lastPipelineRun) {
            this.lastPipelineRun = lastPipelineRun;
            return this;
        }

        public CICDConfigurationBuilder lastPipelineStatus(String lastPipelineStatus) {
            this.lastPipelineStatus = lastPipelineStatus;
            return this;
        }

        public CICDConfigurationBuilder pipelineRunCount(Integer pipelineRunCount) {
            this.pipelineRunCount = pipelineRunCount;
            return this;
        }

        public CICDConfiguration build() {
            return new CICDConfiguration(id, project, projectType, testCommand, buildCommand, 
                                       deployStrategy, pipelineContent, configurationJson, 
                                       localPipelinePath, localConfigPath, githubPipelinePath,
                                       githubConfigPath, generatedAt, updatedAt, isActive, 
                                       lastPipelineRun, lastPipelineStatus, pipelineRunCount);
        }
    }
}

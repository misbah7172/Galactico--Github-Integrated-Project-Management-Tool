package com.autotrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for CI/CD pipeline configuration.
 */
public class CICDConfigDTO {
    
    @NotBlank(message = "Project name is required")
    private String projectName;
    
    @NotBlank(message = "Project type is required")
    private String projectType; // node, python, react, docker, java, custom
    
    private String testCommand;
    
    private String buildCommand;
    
    @NotNull(message = "Deploy strategy is required")
    private String deployStrategy; // none, staging, production, docker, aws-lambda
    
    // Constructors
    public CICDConfigDTO() {}
    
    public CICDConfigDTO(String projectName, String projectType, String testCommand, 
                        String buildCommand, String deployStrategy) {
        this.projectName = projectName;
        this.projectType = projectType;
        this.testCommand = testCommand;
        this.buildCommand = buildCommand;
        this.deployStrategy = deployStrategy;
    }
    
    // Getters and Setters
    public String getProjectName() {
        return projectName;
    }
    
    public void setProjectName(String projectName) {
        this.projectName = projectName;
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
    
    @Override
    public String toString() {
        return "CICDConfigDTO{" +
                "projectName='" + projectName + '\'' +
                ", projectType='" + projectType + '\'' +
                ", testCommand='" + testCommand + '\'' +
                ", buildCommand='" + buildCommand + '\'' +
                ", deployStrategy='" + deployStrategy + '\'' +
                '}';
    }
}

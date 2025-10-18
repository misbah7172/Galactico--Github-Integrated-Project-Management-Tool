package com.autotrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Enhanced DTO for multi-language CI/CD pipeline configuration.
 */
public class MultiLanguageCICDConfigDTO {
    
    @NotBlank(message = "Project name is required")
    private String projectName;
    
    @NotNull(message = "Project architecture is required")
    private String projectArchitecture; // monolith, microservices, full-stack, extension
    
    @NotNull(message = "At least one language/component must be selected")
    private List<LanguageComponent> components;
    
    @NotNull(message = "Deploy strategy is required")
    private String deployStrategy; // none, staging, production, docker, aws-lambda, heroku, vercel
    
    private Map<String, String> environmentVariables;
    
    private List<String> secrets; // List of required secrets
    
    private String dockerConfiguration; // Custom docker setup
    
    private String deploymentTarget; // aws, heroku, vercel, github-pages, docker-hub
    
    /**
     * Represents a language/technology component in the project
     */
    public static class LanguageComponent {
        @NotBlank(message = "Language type is required")
        private String language; // react, nodejs, spring-boot, php, python, typescript, database
        
        private String directory; // relative path in repo (e.g., "frontend", "backend", "extension")
        
        private String buildCommand;
        
        private String testCommand;
        
        private String startCommand; // for services that need to run
        
        private String version; // language/framework version
        
        private List<String> dependencies; // package managers, additional tools
        
        private Map<String, String> environmentSetup; // environment-specific setup
        
        private boolean isMainComponent; // primary component for deployment
        
        // Constructors
        public LanguageComponent() {}
        
        public LanguageComponent(String language, String directory, String buildCommand, 
                               String testCommand, boolean isMainComponent) {
            this.language = language;
            this.directory = directory;
            this.buildCommand = buildCommand;
            this.testCommand = testCommand;
            this.isMainComponent = isMainComponent;
        }
        
        // Getters and Setters
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        
        public String getDirectory() { return directory; }
        public void setDirectory(String directory) { this.directory = directory; }
        
        public String getBuildCommand() { return buildCommand; }
        public void setBuildCommand(String buildCommand) { this.buildCommand = buildCommand; }
        
        public String getTestCommand() { return testCommand; }
        public void setTestCommand(String testCommand) { this.testCommand = testCommand; }
        
        public String getStartCommand() { return startCommand; }
        public void setStartCommand(String startCommand) { this.startCommand = startCommand; }
        
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        
        public List<String> getDependencies() { return dependencies; }
        public void setDependencies(List<String> dependencies) { this.dependencies = dependencies; }
        
        public Map<String, String> getEnvironmentSetup() { return environmentSetup; }
        public void setEnvironmentSetup(Map<String, String> environmentSetup) { this.environmentSetup = environmentSetup; }
        
        public boolean isMainComponent() { return isMainComponent; }
        public void setMainComponent(boolean mainComponent) { isMainComponent = mainComponent; }
    }
    
    // Constructors
    public MultiLanguageCICDConfigDTO() {}
    
    // Getters and Setters
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    
    public String getProjectArchitecture() { return projectArchitecture; }
    public void setProjectArchitecture(String projectArchitecture) { this.projectArchitecture = projectArchitecture; }
    
    public List<LanguageComponent> getComponents() { return components; }
    public void setComponents(List<LanguageComponent> components) { this.components = components; }
    
    public String getDeployStrategy() { return deployStrategy; }
    public void setDeployStrategy(String deployStrategy) { this.deployStrategy = deployStrategy; }
    
    public Map<String, String> getEnvironmentVariables() { return environmentVariables; }
    public void setEnvironmentVariables(Map<String, String> environmentVariables) { this.environmentVariables = environmentVariables; }
    
    public List<String> getSecrets() { return secrets; }
    public void setSecrets(List<String> secrets) { this.secrets = secrets; }
    
    public String getDockerConfiguration() { return dockerConfiguration; }
    public void setDockerConfiguration(String dockerConfiguration) { this.dockerConfiguration = dockerConfiguration; }
    
    public String getDeploymentTarget() { return deploymentTarget; }
    public void setDeploymentTarget(String deploymentTarget) { this.deploymentTarget = deploymentTarget; }
}

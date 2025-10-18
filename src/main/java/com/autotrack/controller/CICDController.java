package com.autotrack.controller;

import com.autotrack.dto.CICDConfigDTO;
import com.autotrack.dto.MultiLanguageCICDConfigDTO;
import com.autotrack.model.CICDConfiguration;
import com.autotrack.model.Project;
import com.autotrack.service.CICDGeneratorService;
import com.autotrack.service.GitHubService;
import com.autotrack.service.MultiLanguagePipelineGenerator;
import com.autotrack.service.ProjectService;
import com.autotrack.service.UserService;
import com.autotrack.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for CI/CD pipeline management.
 */
@Controller
@RequestMapping("/projects/{projectId}/cicd")
public class CICDController {
    
    @Autowired
    private CICDGeneratorService cicdGeneratorService;
    
    @Autowired
    private MultiLanguagePipelineGenerator multiLanguagePipelineGenerator;
    
    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private GitHubService gitHubService;
    
    /**
     * Show CI/CD configuration form.
     */
    @GetMapping
    // @PreAuthorize("hasAuthority('TEAM_LEAD') or hasAuthority('MEMBER')")
    public String showCICDConfig(@PathVariable Long projectId, Model model, 
                                @AuthenticationPrincipal OAuth2User principal) {
        User currentUser = userService.getCurrentUser(principal);
        Project project = projectService.getProjectById(projectId);
        
        // Debug: Log user's roles for troubleshooting
        System.out.println("Current user: " + currentUser.getNickname());
        System.out.println("User roles: " + currentUser.getRoles());
        
        // Check if user has access to the project
        if (!isUserAuthorizedForProject(currentUser, project)) {
            return "redirect:/projects?error=unauthorized";
        }
        
        // Get existing CI/CD configuration if any
        CICDConfiguration existingConfig = cicdGeneratorService.getCICDConfigByProject(project);
        
        // Create DTO for form
        CICDConfigDTO cicdConfigDTO = new CICDConfigDTO();
        if (existingConfig != null) {
            cicdConfigDTO.setProjectName(project.getName());
            cicdConfigDTO.setProjectType(existingConfig.getProjectType());
            cicdConfigDTO.setTestCommand(existingConfig.getTestCommand());
            cicdConfigDTO.setBuildCommand(existingConfig.getBuildCommand());
            cicdConfigDTO.setDeployStrategy(existingConfig.getDeployStrategy());
        } else {
            cicdConfigDTO.setProjectName(project.getName());
            cicdConfigDTO.setProjectType("java"); // Default to Java since this is a Spring Boot project
            cicdConfigDTO.setDeployStrategy("none"); // Default
        }
        
        model.addAttribute("project", project);
        model.addAttribute("cicdConfigDTO", cicdConfigDTO);
        model.addAttribute("existingConfig", existingConfig);
        model.addAttribute("currentUser", currentUser);
        
        // Add GitHub repository information if available
        try {
            if (project.getGitHubRepoUrl() != null && !project.getGitHubRepoUrl().isEmpty()) {
                JsonNode repoInfo = gitHubService.getRepository(currentUser, project.getGitHubRepoUrl());
                if (repoInfo != null) {
                    // Convert JsonNode to a simple Map for easier template access
                    Map<String, String> repoData = new HashMap<>();
                    repoData.put("full_name", repoInfo.has("full_name") ? repoInfo.get("full_name").asText() : "");
                    repoData.put("html_url", repoInfo.has("html_url") ? repoInfo.get("html_url").asText() : "");
                    repoData.put("description", repoInfo.has("description") ? repoInfo.get("description").asText() : "");
                    model.addAttribute("githubRepository", repoData);
                    System.out.println("DEBUG: Added githubRepository to model: " + repoData);
                }
            }
        } catch (Exception e) {
            // Log the error but don't fail the page load
            System.err.println("Failed to fetch GitHub repository info: " + e.getMessage());
        }
        
        return "project/cicd";
    }
    
    /**
     * Process CI/CD configuration form submission.
     */
    @PostMapping
    @PreAuthorize("hasRole('TEAM_LEAD') or hasRole('MEMBER')")
    public String createOrUpdateCICDConfig(@PathVariable Long projectId,
                                          @ModelAttribute("cicdConfigDTO") CICDConfigDTO cicdConfigDTO,
                                          @AuthenticationPrincipal OAuth2User principal,
                                          RedirectAttributes redirectAttributes,
                                          Model model) {
        
        User currentUser = userService.getCurrentUser(principal);
        Project project = projectService.getProjectById(projectId);
        
        // Auto-populate project name if not set
        if (cicdConfigDTO.getProjectName() == null || cicdConfigDTO.getProjectName().isEmpty()) {
            cicdConfigDTO.setProjectName(project.getName());
        }
        
        // Debug: Log that POST request was received
        System.out.println("=== CI/CD POST Request Received ===");
        System.out.println("Project ID: " + projectId);
        System.out.println("Project Name: " + cicdConfigDTO.getProjectName());
        System.out.println("Project Type: " + cicdConfigDTO.getProjectType());
        System.out.println("Deploy Strategy: " + cicdConfigDTO.getDeployStrategy());
        System.out.println("Test Command: " + cicdConfigDTO.getTestCommand());
        System.out.println("Build Command: " + cicdConfigDTO.getBuildCommand());
        
        // Manually validate the DTO after auto-population
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<CICDConfigDTO>> violations = validator.validate(cicdConfigDTO);
        
        if (!violations.isEmpty()) {
            System.out.println("Has Validation Errors: true");
            System.out.println("Validation Errors: " + violations);
        } else {
            System.out.println("Has Validation Errors: false");
        }
        
        // Check if user has access to the project
        if (!isUserAuthorizedForProject(currentUser, project)) {
            return "redirect:/projects?error=unauthorized";
        }
        
        if (!violations.isEmpty()) {
            CICDConfiguration existingConfig = cicdGeneratorService.getCICDConfigByProject(project);
            model.addAttribute("project", project);
            model.addAttribute("existingConfig", existingConfig);
            model.addAttribute("currentUser", currentUser);
            
            // Add validation errors to model for display
            for (ConstraintViolation<CICDConfigDTO> violation : violations) {
                model.addAttribute("error", violation.getMessage());
            }
            
            return "project/cicd";
        }
        
        try {
            // Check if configuration already exists
            CICDConfiguration existingConfig = cicdGeneratorService.getCICDConfigByProject(project);
            
            CICDConfiguration savedConfig;
            if (existingConfig != null) {
                savedConfig = cicdGeneratorService.updateCICDPipeline(project, cicdConfigDTO);
            } else {
                savedConfig = cicdGeneratorService.generateCICDPipeline(project, cicdConfigDTO);
            }
            
            // Create detailed success message based on what was accomplished
            StringBuilder successMsg = new StringBuilder();
            boolean isUpdate = existingConfig != null;
            
            if (isUpdate) {
                successMsg.append("CI/CD pipeline updated successfully for project ").append(project.getName());
            } else {
                successMsg.append("CI/CD pipeline created successfully for project ").append(project.getName());
            }
            
            // Add GitHub-specific messaging
            if (project.getGitHubAccessToken() != null && !project.getGitHubAccessToken().isEmpty()) {
                if (savedConfig.getGithubPipelinePath() != null) {
                    successMsg.append(". 🚀 Pipeline has been pushed to GitHub Actions and is ready to use!");
                    redirectAttributes.addFlashAttribute("githubSuccess", true);
                    redirectAttributes.addFlashAttribute("githubUrl", savedConfig.getGithubPipelinePath());
                } else {
                    successMsg.append(". ⚠️ Pipeline was generated but could not be pushed to GitHub. Please check your repository access token.");
                    redirectAttributes.addFlashAttribute("githubWarning", true);
                }
            } else {
                successMsg.append(". Pipeline saved locally. Connect GitHub repository to enable automatic push to GitHub Actions.");
                redirectAttributes.addFlashAttribute("localOnly", true);
            }
            
            redirectAttributes.addFlashAttribute("successMessage", successMsg.toString());
            return "redirect:/projects/" + projectId + "/cicd";
            
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Failed to create/update CI/CD pipeline: " + e.getMessage());
            CICDConfiguration existingConfig = cicdGeneratorService.getCICDConfigByProject(project);
            model.addAttribute("project", project);
            model.addAttribute("existingConfig", existingConfig);
            model.addAttribute("currentUser", currentUser);
            
            // Add GitHub repository information for error page
            try {
                if (project.getGitHubRepoUrl() != null && !project.getGitHubRepoUrl().isEmpty()) {
                    JsonNode repoInfo = gitHubService.getRepository(currentUser, project.getGitHubRepoUrl());
                    if (repoInfo != null) {
                        // Convert JsonNode to a simple Map for easier template access
                        Map<String, String> repoData = new HashMap<>();
                        repoData.put("full_name", repoInfo.has("full_name") ? repoInfo.get("full_name").asText() : "");
                        repoData.put("html_url", repoInfo.has("html_url") ? repoInfo.get("html_url").asText() : "");
                        repoData.put("description", repoInfo.has("description") ? repoInfo.get("description").asText() : "");
                        model.addAttribute("githubRepository", repoData);
                        System.out.println("DEBUG: Added githubRepository to model (POST): " + repoData);
                    }
                }
            } catch (Exception githubException) {
                // Ignore GitHub API errors during error handling
                System.err.println("Failed to fetch GitHub repository info during error handling: " + githubException.getMessage());
            }
            
            return "project/cicd";
        }
    }
    
    /**
     * Delete CI/CD configuration.
     */
    @PostMapping("/delete")
    @PreAuthorize("hasRole('TEAM_LEAD') or hasRole('MEMBER')")
    public String deleteCICDConfig(@PathVariable Long projectId,
                                  @AuthenticationPrincipal OAuth2User principal,
                                  RedirectAttributes redirectAttributes) {
        
        User currentUser = userService.getCurrentUser(principal);
        Project project = projectService.getProjectById(projectId);
        
        // Check if user has access to the project
        if (!isUserAuthorizedForProject(currentUser, project)) {
            return "redirect:/projects?error=unauthorized";
        }
        
        try {
            cicdGeneratorService.deleteCICDPipeline(project);
            redirectAttributes.addFlashAttribute("successMessage", 
                "CI/CD pipeline deleted successfully for project " + project.getName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Failed to delete CI/CD pipeline: " + e.getMessage());
        }
        
        return "redirect:/projects/" + projectId + "/cicd";
    }
    
    /**
     * REST API endpoint for creating CI/CD pipeline via API.
     */
    @PostMapping("/api")
    @ResponseBody
    @PreAuthorize("hasRole('TEAM_LEAD') or hasRole('MEMBER')")
    public ResponseEntity<Map<String, Object>> createCICDPipelineAPI(@PathVariable Long projectId,
                                                                    @Valid @RequestBody CICDConfigDTO cicdConfigDTO,
                                                                    @AuthenticationPrincipal OAuth2User principal) {
        try {
            User currentUser = userService.getCurrentUser(principal);
            Project project = projectService.getProjectById(projectId);
            
            // Check if user has access to the project
            if (!isUserAuthorizedForProject(currentUser, project)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Unauthorized access to project"));
            }
            
            CICDConfiguration savedConfig = cicdGeneratorService.generateCICDPipeline(project, cicdConfigDTO);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "CI/CD pipeline created successfully");
            response.put("configId", savedConfig.getId());
            response.put("projectType", savedConfig.getProjectType());
            response.put("deployStrategy", savedConfig.getDeployStrategy());
            response.put("generatedAt", savedConfig.getGeneratedAt().toString());
            response.put("githubPipelinePath", savedConfig.getGithubPipelinePath());
            response.put("localPipelinePath", savedConfig.getLocalPipelinePath());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to create CI/CD pipeline: " + e.getMessage()));
        }
    }
    
    /**
     * REST API endpoint for getting CI/CD configuration.
     */
    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCICDConfigAPI(@PathVariable Long projectId,
                                                               @AuthenticationPrincipal OAuth2User principal) {
        try {
            User currentUser = userService.getCurrentUser(principal);
            Project project = projectService.getProjectById(projectId);
            
            // Check if user has access to the project
            if (!isUserAuthorizedForProject(currentUser, project)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "Unauthorized access to project"));
            }
            
            CICDConfiguration config = cicdGeneratorService.getCICDConfigByProject(project);
            
            if (config == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "No CI/CD configuration found for this project"));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("configId", config.getId());
            response.put("projectType", config.getProjectType());
            response.put("testCommand", config.getTestCommand());
            response.put("buildCommand", config.getBuildCommand());
            response.put("deployStrategy", config.getDeployStrategy());
            response.put("generatedAt", config.getGeneratedAt().toString());
            response.put("updatedAt", config.getUpdatedAt() != null ? config.getUpdatedAt().toString() : null);
            response.put("isActive", config.getIsActive());
            response.put("lastPipelineRun", config.getLastPipelineRun() != null ? config.getLastPipelineRun().toString() : null);
            response.put("lastPipelineStatus", config.getLastPipelineStatus());
            response.put("pipelineRunCount", config.getPipelineRunCount());
            response.put("githubPipelinePath", config.getGithubPipelinePath());
            response.put("localPipelinePath", config.getLocalPipelinePath());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to get CI/CD configuration: " + e.getMessage()));
        }
    }
    
    /**
     * Check if user is authorized to access the project.
     */
    private boolean isUserAuthorizedForProject(User user, Project project) {
        // User can access if they are the project owner, team lead, or team member
        return project.getOwner().getId().equals(user.getId()) || 
               (project.getTeam() != null && project.getTeam().getMembers().contains(user));
    }

    /**
     * Show multi-language CI/CD configuration form.
     */
    @GetMapping("/multi-language")
    public String showMultiLanguageCICDConfig(@PathVariable Long projectId, Model model,
                                            @AuthenticationPrincipal OAuth2User principal) {
        User currentUser = userService.getCurrentUser(principal);
        Project project = projectService.getProjectById(projectId);
        
        // Check if user has access to the project
        if (!isUserAuthorizedForProject(currentUser, project)) {
            return "redirect:/projects?error=unauthorized";
        }
        
        model.addAttribute("project", project);
        model.addAttribute("multiLanguageConfig", new MultiLanguageCICDConfigDTO());
        model.addAttribute("projectTypes", new String[]{"MONOLITH", "MICROSERVICES", "FULLSTACK", "EXTENSION"});
        model.addAttribute("supportedLanguages", new String[]{"NODE_JS", "REACT", "TYPESCRIPT", "PYTHON", "JAVA", "SPRING_BOOT", "PHP"});
        model.addAttribute("deploymentStrategies", new String[]{"none", "staging", "production", "docker", "aws-lambda"});
        
        return "cicd/multi-language-config";
    }

    /**
     * Configure multi-language CI/CD pipeline.
     */
    @PostMapping("/multi-language")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> configureMultiLanguageCICD(
            @PathVariable Long projectId,
            @RequestBody @Valid MultiLanguageCICDConfigDTO config,
            @AuthenticationPrincipal OAuth2User principal) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            User currentUser = userService.getCurrentUser(principal);
            Project project = projectService.getProjectById(projectId);
            
            // Check if user has access to the project
            if (!isUserAuthorizedForProject(currentUser, project)) {
                response.put("success", false);
                response.put("message", "Unauthorized access to project");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            
            // Set project name from the project entity
            config.setProjectName(project.getName());
            
            // Generate the multi-language workflow
            String workflowContent = multiLanguagePipelineGenerator.generateWorkflow(config);
            
            // Create GitHub repository if it doesn't exist
            String repoName = project.getGitHubRepoId();
            if (repoName == null || repoName.isEmpty()) {
                repoName = project.getName().toLowerCase().replaceAll("[^a-z0-9-]", "-");
                // Note: Repository creation would require GitHub API integration
                // For now, we'll assume the repository exists or needs to be created manually
            }
            
            // Push workflow to GitHub using existing GitHubService
            String workflowFileName = "ci-cd-multi-language.yml";
            String commitMessage = "Add multi-language CI/CD pipeline configuration";
            
            String pushResult = gitHubService.createOrUpdateFile(project, ".github/workflows/" + workflowFileName, 
                                                               workflowContent, commitMessage);
            
            if (pushResult != null) {
                // Save configuration to database (you may want to create a new entity for multi-language configs)
                response.put("success", true);
                response.put("message", "Multi-language CI/CD pipeline configured successfully!");
                response.put("workflowPath", ".github/workflows/" + workflowFileName);
                response.put("repositoryUrl", project.getGitHubRepoUrl());
                response.put("workflowUrl", project.getGitHubRepoUrl().replace(".git", "") + "/actions");
            } else {
                response.put("success", false);
                response.put("message", "Failed to push workflow to GitHub repository");
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error configuring multi-language CI/CD: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get available project architectures for multi-language setup.
     */
    @GetMapping("/architectures")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getProjectArchitectures() {
        Map<String, Object> response = new HashMap<>();
        
        Map<String, String> architectures = new HashMap<>();
        architectures.put("MONOLITH", "Single application with multiple languages in one repository");
        architectures.put("MICROSERVICES", "Multiple independent services with different languages");
        architectures.put("FULLSTACK", "Separate frontend and backend applications");
        architectures.put("EXTENSION", "Browser extension or IDE extension project");
        
        response.put("architectures", architectures);
        
        Map<String, String> languages = new HashMap<>();
        languages.put("NODE_JS", "Node.js with npm/yarn");
        languages.put("REACT", "React frontend application");
        languages.put("TYPESCRIPT", "TypeScript applications");
        languages.put("PYTHON", "Python applications with pip/poetry");
        languages.put("JAVA", "Java applications with Maven/Gradle");
        languages.put("SPRING_BOOT", "Spring Boot applications");
        languages.put("PHP", "PHP applications with Composer");
        
        response.put("languages", languages);
        
        return ResponseEntity.ok(response);
    }
}

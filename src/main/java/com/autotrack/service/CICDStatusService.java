package com.autotrack.service;

import com.autotrack.model.CICDConfiguration;
import com.autotrack.model.Project;
import com.autotrack.repository.CICDConfigurationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Service for handling CI/CD pipeline status updates from GitHub webhooks.
 */
@Service
public class CICDStatusService {
    
    private static final Logger logger = LoggerFactory.getLogger(CICDStatusService.class);
    
    @Autowired
    private CICDConfigurationRepository cicdConfigRepository;
    
    @Autowired
    private SlackService slackService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Process GitHub Actions workflow run webhook.
     */
    @Transactional
    public void processWorkflowRunWebhook(Map<String, Object> payload) {
        try {
            JsonNode payloadNode = objectMapper.valueToTree(payload);
            
            // Extract workflow run information
            JsonNode workflowRun = payloadNode.get("workflow_run");
            if (workflowRun == null) {
                logger.warn("Invalid workflow run webhook payload: workflow_run not found");
                return;
            }
            
            String action = payloadNode.get("action").asText();
            String status = workflowRun.get("status").asText(); // queued, in_progress, completed
            String conclusion = workflowRun.has("conclusion") ? workflowRun.get("conclusion").asText() : null; // success, failure, cancelled, etc.
            String workflowName = workflowRun.get("name").asText();
            String runUrl = workflowRun.get("html_url").asText();
            
            // Get repository information
            JsonNode repository = payloadNode.get("repository");
            String repoId = repository.get("id").asText();
            String repoName = repository.get("name").asText();
            
            logger.info("Received workflow run webhook - Action: {}, Status: {}, Conclusion: {}, Workflow: {}, Repo: {}", 
                       action, status, conclusion, workflowName, repoName);
            
            // Only process CI/CD Pipeline workflows
            if (!"CI/CD Pipeline".equals(workflowName)) {
                logger.debug("Ignoring workflow run for non-CI/CD workflow: {}", workflowName);
                return;
            }
            
            // Find CI/CD configuration by repository
            Optional<CICDConfiguration> configOpt = findCICDConfigByRepository(repoId, repoName);
            if (configOpt.isEmpty()) {
                logger.warn("No CI/CD configuration found for repository: {}", repoName);
                return;
            }
            
            CICDConfiguration config = configOpt.get();
            
            // Update CI/CD configuration status
            updateCICDStatus(config, action, status, conclusion, runUrl);
            
            // Send notifications
            sendStatusNotification(config, action, status, conclusion, runUrl);
            
        } catch (Exception e) {
            logger.error("Error processing workflow run webhook", e);
        }
    }
    
    /**
     * Update CI/CD configuration status.
     */
    private void updateCICDStatus(CICDConfiguration config, String action, String status, String conclusion, String runUrl) {
        boolean shouldUpdate = false;
        
        if ("requested".equals(action) || "in_progress".equals(action)) {
            config.setLastPipelineStatus("running");
            config.setLastPipelineRun(LocalDateTime.now());
            shouldUpdate = true;
        } else if ("completed".equals(action)) {
            if (conclusion != null) {
                switch (conclusion) {
                    case "success":
                        config.setLastPipelineStatus("success");
                        break;
                    case "failure":
                    case "cancelled":
                    case "timed_out":
                        config.setLastPipelineStatus("failure");
                        break;
                    default:
                        config.setLastPipelineStatus("unknown");
                }
                config.setPipelineRunCount(config.getPipelineRunCount() + 1);
                shouldUpdate = true;
            }
        }
        
        if (shouldUpdate) {
            config.setUpdatedAt(LocalDateTime.now());
            cicdConfigRepository.save(config);
            logger.info("Updated CI/CD status for project {} - Status: {}, Run count: {}", 
                       config.getProject().getName(), config.getLastPipelineStatus(), config.getPipelineRunCount());
        }
    }
    
    /**
     * Send status notification to Slack.
     */
    private void sendStatusNotification(CICDConfiguration config, String action, String status, String conclusion, String runUrl) {
        try {
            String projectName = config.getProject().getName();
            String emoji = getStatusEmoji(action, status, conclusion);
            String message;
            
            if ("requested".equals(action) || "in_progress".equals(action)) {
                message = String.format("%s CI/CD Pipeline started for project '%s' (%s)", 
                                      emoji, projectName, config.getProjectType());
            } else if ("completed".equals(action)) {
                String result = conclusion != null ? conclusion : "unknown";
                message = String.format("%s CI/CD Pipeline %s for project '%s' (%s) - Run #%d", 
                                      emoji, result, projectName, config.getProjectType(), config.getPipelineRunCount());
                if (runUrl != null) {
                    message += "\n🔗 View details: " + runUrl;
                }
            } else {
                return; // Don't send notification for other actions
            }
            
            slackService.sendMessage(message);
            
        } catch (Exception e) {
            logger.error("Failed to send CI/CD status notification", e);
        }
    }
    
    /**
     * Get appropriate emoji for status.
     */
    private String getStatusEmoji(String action, String status, String conclusion) {
        if ("requested".equals(action) || "in_progress".equals(action)) {
            return "🚀";
        } else if ("completed".equals(action)) {
            if ("success".equals(conclusion)) {
                return "✅";
            } else if ("failure".equals(conclusion)) {
                return "❌";
            } else if ("cancelled".equals(conclusion)) {
                return "⏹️";
            } else {
                return "⚠️";
            }
        }
        return "ℹ️";
    }
    
    /**
     * Find CI/CD configuration by repository information.
     */
    private Optional<CICDConfiguration> findCICDConfigByRepository(String repoId, String repoName) {
        // First try to find by GitHub repository ID
        return cicdConfigRepository.findByIsActiveOrderByGeneratedAtDesc(true)
                .stream()
                .filter(config -> {
                    Project project = config.getProject();
                    if (project.getGitHubRepoId() != null && project.getGitHubRepoId().equals(repoId)) {
                        return true;
                    }
                    // Fallback: match by repository name in URL
                    if (project.getGitHubRepoUrl() != null) {
                        return project.getGitHubRepoUrl().contains("/" + repoName) || 
                               project.getGitHubRepoUrl().endsWith("/" + repoName + ".git");
                    }
                    return false;
                })
                .findFirst();
    }
    
    /**
     * Get CI/CD statistics for dashboard.
     */
    public Map<String, Object> getCICDStatistics() {
        return Map.of(
            "totalConfigurations", cicdConfigRepository.findByIsActiveOrderByGeneratedAtDesc(true).size(),
            "successfulRuns", cicdConfigRepository.findByLastPipelineStatusAndIsActive("success", true).size(),
            "failedRuns", cicdConfigRepository.findByLastPipelineStatusAndIsActive("failure", true).size(),
            "runningPipelines", cicdConfigRepository.findByLastPipelineStatusAndIsActive("running", true).size(),
            "neverRun", cicdConfigRepository.findByLastPipelineStatusAndIsActive(null, true).size()
        );
    }
    
    /**
     * Get project type distribution.
     */
    public Map<String, Long> getProjectTypeDistribution() {
        return cicdConfigRepository.countActiveConfigurationsByProjectType()
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                    arr -> (String) arr[0],
                    arr -> (Long) arr[1]
                ));
    }
    
    /**
     * Get deploy strategy distribution.
     */
    public Map<String, Long> getDeployStrategyDistribution() {
        return cicdConfigRepository.countActiveConfigurationsByDeployStrategy()
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                    arr -> (String) arr[0],
                    arr -> (Long) arr[1]
                ));
    }
}

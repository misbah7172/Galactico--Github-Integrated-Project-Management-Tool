package com.autotrack.service;

import com.autotrack.model.Commit;
import com.autotrack.model.Project;
import com.autotrack.model.Task;
import com.autotrack.repository.CommitRepository;
import com.autotrack.repository.ProjectRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

/**
 * Service for handling GitHub webhooks.
 */
@Service
public class WebhookService {

    private final ProjectRepository projectRepository;
    private final CommitRepository commitRepository;
    private final CommitParserService commitParserService;
    private final TaskService taskService;
    private final GitHubCommitAnalysisService commitAnalysisService;
    private final ObjectMapper objectMapper;

    @Autowired
    public WebhookService(ProjectRepository projectRepository,
                         CommitRepository commitRepository,
                         CommitParserService commitParserService,
                         TaskService taskService,
                         GitHubCommitAnalysisService commitAnalysisService) {
        this.projectRepository = projectRepository;
        this.commitRepository = commitRepository;
        this.commitParserService = commitParserService;
        this.taskService = taskService;
        this.commitAnalysisService = commitAnalysisService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Process a GitHub webhook payload.
     */
    @Transactional
    public void processWebhook(Map<String, Object> payload, String signature) {
        try {
            // Convert payload to JsonNode for easier access
            JsonNode payloadNode = objectMapper.valueToTree(payload);
            
            // Get repository information
            JsonNode repository = payloadNode.get("repository");
            if (repository == null) {
                throw new RuntimeException("Invalid webhook payload: repository not found");
            }
            
            String repoId = repository.get("id").asText();
            
            // Find project by GitHub repository ID
            Optional<Project> projectOpt = projectRepository.findByGitHubRepoId(repoId);
            if (projectOpt.isEmpty()) {
                // Try to find by URL
                String repoUrl = repository.get("html_url").asText();
                projectOpt = projectRepository.findAll().stream()
                        .filter(p -> p.getGitHubRepoUrl() != null && 
                                  (p.getGitHubRepoUrl().equals(repoUrl) || 
                                   p.getGitHubRepoUrl().equals(repoUrl + ".git")))
                        .findFirst();
                
                if (projectOpt.isEmpty()) {
                    throw new RuntimeException("No project found for repository: " + repoUrl);
                }
            }
            
            Project project = projectOpt.get();
            
            // Verify webhook signature if secret is set
            if (project.getWebhookSecret() != null && !project.getWebhookSecret().isEmpty() && signature != null) {
                verifySignature(objectMapper.writeValueAsString(payload), signature, project.getWebhookSecret());
            }
            
            // Process commits
            JsonNode commits = payloadNode.get("commits");
            if (commits != null && commits.isArray()) {
                for (JsonNode commitNode : commits) {
                    processCommit(commitNode, project);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error processing webhook: " + e.getMessage(), e);
        }
    }

    /**
     * Process a single commit from the webhook payload.
     */
    private void processCommit(JsonNode commitNode, Project project) {
        String sha = commitNode.get("id").asText();
        String message = commitNode.get("message").asText();
        String url = commitNode.get("url").asText();
        
        // Skip if commit already exists
        if (commitRepository.findByShaAndProject(sha, project).isPresent()) {
            return;
        }
        
        // Get author information
        JsonNode author = commitNode.get("author");
        String authorName = author.get("name").asText();
        String authorEmail = author.get("email").asText();
        
        // Parse timestamp
        String timestamp = commitNode.get("timestamp").asText();
        LocalDateTime committedAt = ZonedDateTime.parse(timestamp).toLocalDateTime();
        
        // Analyze commit for code statistics
        GitHubCommitAnalysisService.CommitStats stats = commitAnalysisService.extractStatsFromWebhookPayload(commitNode);
        
        // Parse commit message
        Optional<CommitParserService.CommitInfo> commitInfoOpt = commitParserService.parseCommitMessage(message);
        
        if (commitInfoOpt.isPresent()) {
            CommitParserService.CommitInfo commitInfo = commitInfoOpt.get();
            
            // Find or create task
            Task task = taskService.findOrCreateTaskFromCommit(project, commitInfo);
            
            // Create commit record with code statistics
            Commit commit = Commit.builder()
                    .sha(sha)
                    .message(message)
                    .authorName(authorName)
                    .authorEmail(authorEmail)
                    .committedAt(committedAt)
                    .gitHubUrl(url)
                    .linesAdded(stats.getLinesAdded())
                    .linesModified(stats.getLinesModified())
                    .linesDeleted(stats.getLinesDeleted())
                    .filesChanged(stats.getFilesChanged())
                    .task(task)
                    .project(project)
                    .build();
            
            commitRepository.save(commit);
        } else {
            // Still save the commit with code statistics, but without task association
            Commit commit = Commit.builder()
                    .sha(sha)
                    .message(message)
                    .authorName(authorName)
                    .authorEmail(authorEmail)
                    .committedAt(committedAt)
                    .gitHubUrl(url)
                    .linesAdded(stats.getLinesAdded())
                    .linesModified(stats.getLinesModified())
                    .linesDeleted(stats.getLinesDeleted())
                    .filesChanged(stats.getFilesChanged())
                    .project(project)
                    .build();
            
            commitRepository.save(commit);
        }
    }

    /**
     * Verify the GitHub webhook signature.
     */
    private void verifySignature(String payload, String signature, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec signingKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(signingKey);
        byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        
        String expectedSignature = "sha256=" + HexFormat.of().formatHex(rawHmac);
        
        if (!signature.equals(expectedSignature)) {
            throw new RuntimeException("Invalid webhook signature");
        }
    }
}

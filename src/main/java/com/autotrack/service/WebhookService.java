package com.autotrack.service;

import com.autotrack.model.*;
import com.autotrack.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final FileChangeMetricsRepository fileChangeMetricsRepository;
    private final ContributorStatsRepository contributorStatsRepository;
    private final BackgroundDataProcessingService backgroundDataProcessingService;
    private final AnalyticsCacheService analyticsCacheService;
    private final ObjectMapper objectMapper;

    public WebhookService(ProjectRepository projectRepository,
                         CommitRepository commitRepository,
                         CommitParserService commitParserService,
                         TaskService taskService,
                         GitHubCommitAnalysisService commitAnalysisService,
                         FileChangeMetricsRepository fileChangeMetricsRepository,
                         ContributorStatsRepository contributorStatsRepository,
                         BackgroundDataProcessingService backgroundDataProcessingService,
                         AnalyticsCacheService analyticsCacheService) {
        this.projectRepository = projectRepository;
        this.commitRepository = commitRepository;
        this.commitParserService = commitParserService;
        this.taskService = taskService;
        this.commitAnalysisService = commitAnalysisService;
        this.fileChangeMetricsRepository = fileChangeMetricsRepository;
        this.contributorStatsRepository = contributorStatsRepository;
        this.backgroundDataProcessingService = backgroundDataProcessingService;
        this.analyticsCacheService = analyticsCacheService;
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
        
        Commit commit;
        if (commitInfoOpt.isPresent()) {
            CommitParserService.CommitInfo commitInfo = commitInfoOpt.get();
            
            // Find or create task
            Task task = taskService.findOrCreateTaskFromCommit(project, commitInfo);
            
            // Create commit record with code statistics
            commit = Commit.builder()
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
        } else {
            // Still save the commit with code statistics, but without task association
            commit = Commit.builder()
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
        }
        
        // Save the commit
        commit = commitRepository.save(commit);
        
        // Process file-level changes for detailed analytics
        processFileChanges(commitNode, commit, project);
        
        // Update contributor statistics
        updateContributorStats(commit, stats, project);
        
        // Trigger background processing for advanced analytics
        backgroundDataProcessingService.processCommitAsync(commit);
        
        // Invalidate cache for this project to ensure fresh data
        analyticsCacheService.invalidateProjectCache(project);
    }
    
    /**
     * Process individual file changes from the commit payload
     */
    private void processFileChanges(JsonNode commitNode, Commit commit, Project project) {
        try {
            // Process added files
            JsonNode added = commitNode.get("added");
            if (added != null && added.isArray()) {
                for (JsonNode fileNode : added) {
                    String fileName = fileNode.asText();
                    createFileChangeMetric(commit, project, fileName, FileChangeMetrics.ChangeType.ADDED, 0, 0, 0);
                }
            }
            
            // Process removed files
            JsonNode removed = commitNode.get("removed");
            if (removed != null && removed.isArray()) {
                for (JsonNode fileNode : removed) {
                    String fileName = fileNode.asText();
                    createFileChangeMetric(commit, project, fileName, FileChangeMetrics.ChangeType.DELETED, 0, 0, 0);
                }
            }
            
            // Process modified files
            JsonNode modified = commitNode.get("modified");
            if (modified != null && modified.isArray()) {
                for (JsonNode fileNode : modified) {
                    String fileName = fileNode.asText();
                    // For webhook payload, we don't have detailed line counts per file
                    // We'll estimate based on total commit changes divided by number of files
                    int estimatedLinesAdded = commit.getFilesChanged() > 0 ? commit.getLinesAdded() / commit.getFilesChanged() : 0;
                    int estimatedLinesModified = commit.getFilesChanged() > 0 ? commit.getLinesModified() / commit.getFilesChanged() : 0;
                    int estimatedLinesDeleted = commit.getFilesChanged() > 0 ? commit.getLinesDeleted() / commit.getFilesChanged() : 0;
                    
                    createFileChangeMetric(commit, project, fileName, FileChangeMetrics.ChangeType.MODIFIED, 
                                         estimatedLinesAdded, estimatedLinesModified, estimatedLinesDeleted);
                }
            }
            
        } catch (Exception e) {
            // Log error but don't fail the commit processing
            System.err.println("Error processing file changes for commit " + commit.getSha() + ": " + e.getMessage());
        }
    }
    
    /**
     * Create a FileChangeMetrics record
     */
    private void createFileChangeMetric(Commit commit, Project project, String fileName, 
                                      FileChangeMetrics.ChangeType changeType, int linesAdded, int linesModified, int linesDeleted) {
        FileChangeMetrics fileMetrics = new FileChangeMetrics();
        fileMetrics.setCommit(commit);
        fileMetrics.setProject(project);
        fileMetrics.setFileName(fileName);
        fileMetrics.setFilePath(fileName);
        fileMetrics.setChangeType(changeType);
        fileMetrics.setLinesAdded(linesAdded);
        fileMetrics.setLinesModified(linesModified);
        fileMetrics.setLinesDeleted(linesDeleted);
        
        fileChangeMetricsRepository.save(fileMetrics);
    }
    
    /**
     * Update contributor statistics
     */
    private void updateContributorStats(Commit commit, GitHubCommitAnalysisService.CommitStats stats, Project project) {
        try {
            String email = commit.getAuthorEmail();
            
            // Find existing contributor stats or create new one
            Optional<ContributorStats> existingStatsOpt = contributorStatsRepository.findByProjectAndContributorEmail(project, email);
            
            ContributorStats contributorStats;
            if (existingStatsOpt.isPresent()) {
                contributorStats = existingStatsOpt.get();
                
                // Update existing stats
                contributorStats.setTotalCommits(contributorStats.getTotalCommits() + 1);
                contributorStats.setTotalLinesAdded(contributorStats.getTotalLinesAdded() + stats.getLinesAdded());
                contributorStats.setTotalLinesModified(contributorStats.getTotalLinesModified() + stats.getLinesModified());
                contributorStats.setTotalLinesDeleted(contributorStats.getTotalLinesDeleted() + stats.getLinesDeleted());
                contributorStats.setTotalFilesChanged(contributorStats.getTotalFilesChanged() + stats.getFilesChanged());
                contributorStats.setLastCommitDate(commit.getCommittedAt());
                
            } else {
                // Create new contributor stats
                contributorStats = new ContributorStats(project, commit.getAuthorName(), email);
                contributorStats.setTotalCommits(1);
                contributorStats.setTotalLinesAdded(stats.getLinesAdded());
                contributorStats.setTotalLinesModified(stats.getLinesModified());
                contributorStats.setTotalLinesDeleted(stats.getLinesDeleted());
                contributorStats.setTotalFilesChanged(stats.getFilesChanged());
                contributorStats.setFirstCommitDate(commit.getCommittedAt());
                contributorStats.setLastCommitDate(commit.getCommittedAt());
            }
            
            // Calculate and update metrics (this will trigger the @PreUpdate/@PrePersist method)
            contributorStatsRepository.save(contributorStats);
            
        } catch (Exception e) {
            // Log error but don't fail the commit processing
            System.err.println("Error updating contributor stats for commit " + commit.getSha() + ": " + e.getMessage());
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

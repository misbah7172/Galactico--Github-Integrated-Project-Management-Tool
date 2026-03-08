package com.autotrack.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for analyzing GitHub commit statistics
 */
@Service
public class GitHubCommitAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(GitHubCommitAnalysisService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Data class to hold commit statistics
     */
    public static class CommitStats {
        private final int linesAdded;
        private final int linesModified;
        private final int linesDeleted;
        private final int filesChanged;
        
        public CommitStats(int linesAdded, int linesModified, int linesDeleted, int filesChanged) {
            this.linesAdded = linesAdded;
            this.linesModified = linesModified;
            this.linesDeleted = linesDeleted;
            this.filesChanged = filesChanged;
        }
        
        public int getLinesAdded() { return linesAdded; }
        public int getLinesModified() { return linesModified; }
        public int getLinesDeleted() { return linesDeleted; }
        public int getFilesChanged() { return filesChanged; }
        
        @Override
        public String toString() {
            return String.format("CommitStats{added=%d, modified=%d, deleted=%d, files=%d}", 
                               linesAdded, linesModified, linesDeleted, filesChanged);
        }
    }
    
    /**
     * Extract commit statistics from webhook payload
     * GitHub webhook payload contains added, removed, and modified arrays in each commit
     */
    public CommitStats extractStatsFromWebhookPayload(JsonNode commitNode) {
        try {
            // GitHub webhook provides added, removed, and modified file arrays
            JsonNode added = commitNode.get("added");
            JsonNode removed = commitNode.get("removed");
            JsonNode modified = commitNode.get("modified");
            
            // Count files for fallback, but prioritize API call for accurate stats
            int estimatedFilesChanged = 0;
            if (added != null && added.isArray()) {
                estimatedFilesChanged += added.size();
            }
            if (removed != null && removed.isArray()) {
                estimatedFilesChanged += removed.size();
            }
            if (modified != null && modified.isArray()) {
                estimatedFilesChanged += modified.size();
            }
            
            // For webhooks, we'll use GitHub API URL to get detailed stats
            String commitUrl = commitNode.get("url").asText();
            CommitStats apiStats = fetchCommitStatsFromAPI(commitUrl);
            
            // If API call failed, return basic file count stats
            if (apiStats.getLinesAdded() == 0 && apiStats.getLinesDeleted() == 0 && 
                apiStats.getLinesModified() == 0 && apiStats.getFilesChanged() == 0) {
                return new CommitStats(0, 0, 0, estimatedFilesChanged);
            }
            
            return apiStats;
            
        } catch (Exception e) {
            logger.warn("Could not extract commit stats from webhook payload: {}", e.getMessage());
            return new CommitStats(0, 0, 0, 0);
        }
    }
    
    /**
     * Fetch detailed commit statistics from GitHub API
     */
    public CommitStats fetchCommitStatsFromAPI(String commitApiUrl) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(commitApiUrl);
            request.setHeader("Accept", "application/vnd.github.v3+json");
            request.setHeader("User-Agent", "Galactico-App");
            
            return httpClient.execute(request, response -> {
                String responseBody = EntityUtils.toString(response.getEntity());
                JsonNode commitData = objectMapper.readTree(responseBody);
                
                return parseCommitStats(commitData);
            });
            
        } catch (Exception e) {
            logger.error("Error fetching commit stats from GitHub API: {}", e.getMessage());
            return new CommitStats(0, 0, 0, 0);
        }
    }
    
    /**
     * Parse commit statistics from GitHub API response
     */
    private CommitStats parseCommitStats(JsonNode commitData) {
        try {
            JsonNode stats = commitData.get("stats");
            if (stats == null) {
                return new CommitStats(0, 0, 0, 0);
            }
            
            int additions = stats.has("additions") ? stats.get("additions").asInt() : 0;
            int deletions = stats.has("deletions") ? stats.get("deletions").asInt() : 0;
            
            // Calculate modified lines (lines that were changed, not just added or deleted)
            JsonNode files = commitData.get("files");
            int totalModified = 0;
            int filesChanged = 0;
            
            if (files != null && files.isArray()) {
                filesChanged = files.size();
                
                for (JsonNode file : files) {
                    int fileAdditions = file.has("additions") ? file.get("additions").asInt() : 0;
                    int fileDeletions = file.has("deletions") ? file.get("deletions").asInt() : 0;
                    int fileChanges = file.has("changes") ? file.get("changes").asInt() : 0;
                    
                    // Modified lines = total changes - pure additions - pure deletions
                    // This gives us lines that were actually modified (not just added or removed)
                    int fileModified = Math.max(0, fileChanges - fileAdditions - fileDeletions);
                    totalModified += fileModified;
                }
            }
            
            logger.debug("Parsed commit stats: +{} -{} ~{} files:{}", 
                        additions, deletions, totalModified, filesChanged);
            
            return new CommitStats(additions, totalModified, deletions, filesChanged);
            
        } catch (Exception e) {
            logger.error("Error parsing commit stats: {}", e.getMessage());
            return new CommitStats(0, 0, 0, 0);
        }
    }
    
    /**
     * Calculate commit impact score based on lines changed
     * This can be used for contribution analysis
     */
    public int calculateCommitImpactScore(CommitStats stats) {
        // Weight: additions = 1 point, modifications = 2 points, deletions = 0.5 points
        double score = (stats.getLinesAdded() * 1.0) + 
                      (stats.getLinesModified() * 2.0) + 
                      (stats.getLinesDeleted() * 0.5);
        
        return (int) Math.round(score);
    }
    
    /**
     * Categorize commit size based on total lines changed
     */
    public String categorizeCommitSize(CommitStats stats) {
        int totalLines = stats.getLinesAdded() + stats.getLinesModified() + stats.getLinesDeleted();
        
        if (totalLines == 0) return "Empty";
        if (totalLines <= 10) return "Small";
        if (totalLines <= 50) return "Medium";
        if (totalLines <= 200) return "Large";
        return "Huge";
    }
}

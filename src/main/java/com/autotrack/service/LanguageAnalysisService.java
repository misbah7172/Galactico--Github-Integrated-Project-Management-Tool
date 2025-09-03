package com.autotrack.service;

import com.autotrack.model.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for analyzing programming languages used in a project repository.
 */
@Service
@SuppressWarnings("rawtypes")
public class LanguageAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(LanguageAnalysisService.class);

    @Value("${github.api.base-url}")
    private String githubApiBaseUrl;

    private final RestTemplate restTemplate;

    public LanguageAnalysisService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Get language statistics for a project from GitHub API.
     * 
     * @param project The project to analyze
     * @return Map of language names to their byte counts
     */
    public Map<String, Long> getProjectLanguages(Project project) {
        if (project == null || project.getGitHubRepoUrl() == null) {
            logger.warn("Project or GitHub repo URL is null");
            return Collections.emptyMap();
        }

        try {
            String repoInfo = extractRepoInfo(project.getGitHubRepoUrl());
            if (repoInfo == null) {
                logger.warn("Could not extract repository info from URL: {}", project.getGitHubRepoUrl());
                return Collections.emptyMap();
            }

            String apiUrl = githubApiBaseUrl + "/repos/" + repoInfo + "/languages";
            
            HttpHeaders headers = new HttpHeaders();
            if (project.getGitHubAccessToken() != null && !project.getGitHubAccessToken().isEmpty()) {
                headers.set("Authorization", "token " + project.getGitHubAccessToken());
            }
            headers.set("Accept", "application/vnd.github.v3+json");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            logger.debug("Fetching languages from GitHub API: {}", apiUrl);
            ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl, 
                HttpMethod.GET, 
                entity, 
                Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> languageData = response.getBody();
                Map<String, Long> languages = new HashMap<>();
                
                if (languageData != null) {
                    for (Map.Entry<String, Object> entry : languageData.entrySet()) {
                        String language = entry.getKey();
                        Long bytes = Long.valueOf(entry.getValue().toString());
                        languages.put(language, bytes);
                    }
                }
                
                logger.info("Successfully fetched {} languages for project: {}", languages.size(), project.getName());
                return languages;
            } else {
                logger.warn("Failed to fetch languages. Status: {}", response.getStatusCode());
                return Collections.emptyMap();
            }

        } catch (Exception e) {
            logger.error("Error fetching project languages for project {}: {}", project.getName(), e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Convert language byte counts to percentages.
     * 
     * @param languageBytes Map of language names to byte counts
     * @return Map of language names to their percentages
     */
    public Map<String, Double> calculateLanguagePercentages(Map<String, Long> languageBytes) {
        if (languageBytes.isEmpty()) {
            return Collections.emptyMap();
        }

        long totalBytes = languageBytes.values().stream().mapToLong(Long::longValue).sum();
        
        return languageBytes.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> (entry.getValue().doubleValue() / totalBytes) * 100.0,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }

    /**
     * Get language statistics with percentages, sorted by usage.
     * 
     * @param project The project to analyze
     * @return Map of language names to their percentages (sorted by usage desc)
     */
    public Map<String, Double> getLanguageStatistics(Project project) {
        Map<String, Long> languageBytes = getProjectLanguages(project);
        return calculateLanguagePercentages(languageBytes);
    }

    /**
     * Get the color associated with a programming language for display.
     * 
     * @param language The programming language name
     * @return Hex color code for the language
     */
    public String getLanguageColor(String language) {
        Map<String, String> languageColors = new HashMap<>();
        languageColors.put("Java", "#b07219");
        languageColors.put("JavaScript", "#f1e05a");
        languageColors.put("HTML", "#e34c26");
        languageColors.put("CSS", "#563d7c");
        languageColors.put("Python", "#3572A5");
        languageColors.put("TypeScript", "#2b7489");
        languageColors.put("C++", "#f34b7d");
        languageColors.put("C", "#555555");
        languageColors.put("C#", "#239120");
        languageColors.put("PHP", "#4F5D95");
        languageColors.put("Ruby", "#701516");
        languageColors.put("Go", "#00ADD8");
        languageColors.put("Rust", "#dea584");
        languageColors.put("Swift", "#ffac45");
        languageColors.put("Kotlin", "#F18E33");
        languageColors.put("Scala", "#c22d40");
        languageColors.put("R", "#198CE7");
        languageColors.put("MATLAB", "#e16737");
        languageColors.put("Shell", "#89e051");
        languageColors.put("Dockerfile", "#384d54");
        
        return languageColors.getOrDefault(language, "#6f42c1"); // Default purple color
    }

    /**
     * Extract repository owner/name from GitHub URL.
     * 
     * @param githubUrl The GitHub repository URL
     * @return "owner/repo" string or null if invalid
     */
    private String extractRepoInfo(String githubUrl) {
        if (githubUrl == null || githubUrl.isEmpty()) {
            return null;
        }

        // Handle various GitHub URL formats
        String url = githubUrl.trim();
        
        // Remove .git suffix if present
        if (url.endsWith(".git")) {
            url = url.substring(0, url.length() - 4);
        }
        
        // Extract owner/repo from URL
        String[] patterns = {
            "https://github.com/([^/]+)/([^/]+)/?",
            "git@github.com:([^/]+)/([^/]+)\\.git",
            "git@github.com:([^/]+)/([^/]+)"
        };
        
        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(url);
            if (m.matches()) {
                return m.group(1) + "/" + m.group(2);
            }
        }
        
        logger.warn("Could not parse GitHub URL: {}", githubUrl);
        return null;
    }
}

package com.autotrack.service;

import com.autotrack.model.Project;
import com.autotrack.model.Task;
import com.autotrack.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service for GitHub API interactions.
 */
@Service
public class GitHubService {
    
    private static final Logger logger = LoggerFactory.getLogger(GitHubService.class);
    private static final String GITHUB_API_URL = "https://api.github.com";
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Webhook Management ──────────────────────────────────────────

    /**
     * Register a webhook on the GitHub repository for this project.
     * Automatically configures push + workflow_run events.
     *
     * @param project The project to register the webhook for
     * @param webhookBaseUrl The base URL of this app (e.g., https://galactico-app.azurewebsites.net)
     * @return Map with "success" boolean, "message" string, and optionally "hookId"
     */
    public Map<String, Object> registerWebhook(Project project, String webhookBaseUrl) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (project.getGitHubAccessToken() == null || project.getGitHubAccessToken().isEmpty()) {
            result.put("success", false);
            result.put("message", "No GitHub access token configured for this project. Please add your GitHub token first.");
            return result;
        }

        String[] parts = parseGitHubRepoUrl(project.getGitHubRepoUrl());
        if (parts == null) {
            result.put("success", false);
            result.put("message", "Invalid GitHub repository URL.");
            return result;
        }

        String owner = parts[0];
        String repo = parts[1];
        String payloadUrl = webhookBaseUrl.replaceAll("/+$", "") + "/api/webhooks/github";

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Check if webhook already exists
            Map<String, Object> existing = findExistingWebhook(httpClient, owner, repo, payloadUrl, project.getGitHubAccessToken());
            if (existing != null) {
                result.put("success", true);
                result.put("message", "Webhook is already configured and active.");
                result.put("hookId", existing.get("id"));
                result.put("alreadyExists", true);
                return result;
            }

            // Create webhook
            ObjectNode hookJson = objectMapper.createObjectNode();
            hookJson.put("name", "web");
            hookJson.put("active", true);

            ObjectNode config = objectMapper.createObjectNode();
            config.put("url", payloadUrl);
            config.put("content_type", "json");
            config.put("secret", project.getWebhookSecret());
            config.put("insecure_ssl", "0");
            hookJson.set("config", config);

            var eventsArray = objectMapper.createArrayNode();
            eventsArray.add("push");
            eventsArray.add("workflow_run");
            hookJson.set("events", eventsArray);

            HttpPost httpPost = new HttpPost(GITHUB_API_URL + "/repos/" + owner + "/" + repo + "/hooks");
            httpPost.setHeader("Authorization", "token " + project.getGitHubAccessToken());
            httpPost.setHeader("Accept", "application/vnd.github.v3+json");
            httpPost.setEntity(new StringEntity(objectMapper.writeValueAsString(hookJson), ContentType.APPLICATION_JSON));

            return httpClient.execute(httpPost, response -> {
                Map<String, Object> res = new LinkedHashMap<>();
                int status = response.getCode();
                if (status == 201) {
                    JsonNode responseJson = objectMapper.readTree(response.getEntity().getContent());
                    res.put("success", true);
                    res.put("message", "Webhook registered successfully! Commits will now be tracked automatically.");
                    res.put("hookId", responseJson.get("id").asLong());
                    logger.info("Webhook registered for {}/{} (hook ID: {})", owner, repo, responseJson.get("id").asLong());
                } else if (status == 404) {
                    res.put("success", false);
                    res.put("message", "Repository not found or access denied. Make sure your token has 'admin:repo_hook' permission.");
                } else if (status == 422) {
                    res.put("success", true);
                    res.put("message", "Webhook already exists for this repository.");
                    res.put("alreadyExists", true);
                } else {
                    String body = "";
                    try { body = new String(response.getEntity().getContent().readAllBytes()); } catch (Exception ignored) {}
                    res.put("success", false);
                    res.put("message", "GitHub API error (HTTP " + status + "): " + body);
                    logger.error("Webhook registration failed for {}/{}: HTTP {} - {}", owner, repo, status, body);
                }
                return res;
            });
        } catch (IOException e) {
            logger.error("Error registering webhook for project {}: {}", project.getName(), e.getMessage());
            result.put("success", false);
            result.put("message", "Connection error: " + e.getMessage());
            return result;
        }
    }

    /**
     * Check the status of webhooks on the GitHub repository.
     *
     * @return Map with webhook status info
     */
    public Map<String, Object> getWebhookStatus(Project project, String webhookBaseUrl) {
        Map<String, Object> status = new LinkedHashMap<>();

        if (project.getGitHubAccessToken() == null || project.getGitHubAccessToken().isEmpty()) {
            status.put("configured", false);
            status.put("message", "No GitHub access token.");
            return status;
        }

        String[] parts = parseGitHubRepoUrl(project.getGitHubRepoUrl());
        if (parts == null) {
            status.put("configured", false);
            status.put("message", "Invalid repo URL.");
            return status;
        }

        String owner = parts[0];
        String repo = parts[1];
        String payloadUrl = webhookBaseUrl.replaceAll("/+$", "") + "/api/webhooks/github";

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            Map<String, Object> existing = findExistingWebhook(httpClient, owner, repo, payloadUrl, project.getGitHubAccessToken());
            if (existing != null) {
                status.put("configured", true);
                status.put("active", existing.get("active"));
                status.put("hookId", existing.get("id"));
                status.put("events", existing.get("events"));
                status.put("lastResponse", existing.get("lastResponse"));
                status.put("message", "Webhook is active and receiving events.");
            } else {
                status.put("configured", false);
                status.put("message", "No webhook found. Click 'Setup Webhook' to configure automatically.");
            }
        } catch (IOException e) {
            status.put("configured", false);
            status.put("message", "Could not check webhook status: " + e.getMessage());
        }

        return status;
    }

    /**
     * Delete the Galactico webhook from the GitHub repository.
     */
    public Map<String, Object> deleteWebhook(Project project, String webhookBaseUrl) {
        Map<String, Object> result = new LinkedHashMap<>();

        String[] parts = parseGitHubRepoUrl(project.getGitHubRepoUrl());
        if (parts == null || project.getGitHubAccessToken() == null) {
            result.put("success", false);
            result.put("message", "Missing repo URL or access token.");
            return result;
        }

        String owner = parts[0];
        String repo = parts[1];
        String payloadUrl = webhookBaseUrl.replaceAll("/+$", "") + "/api/webhooks/github";

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            Map<String, Object> existing = findExistingWebhook(httpClient, owner, repo, payloadUrl, project.getGitHubAccessToken());
            if (existing == null) {
                result.put("success", true);
                result.put("message", "No webhook found to delete.");
                return result;
            }

            long hookId = ((Number) existing.get("id")).longValue();
            HttpDelete httpDelete = new HttpDelete(GITHUB_API_URL + "/repos/" + owner + "/" + repo + "/hooks/" + hookId);
            httpDelete.setHeader("Authorization", "token " + project.getGitHubAccessToken());
            httpDelete.setHeader("Accept", "application/vnd.github.v3+json");

            return httpClient.execute(httpDelete, response -> {
                Map<String, Object> res = new LinkedHashMap<>();
                if (response.getCode() == 204) {
                    res.put("success", true);
                    res.put("message", "Webhook deleted successfully.");
                } else {
                    res.put("success", false);
                    res.put("message", "Failed to delete webhook (HTTP " + response.getCode() + ").");
                }
                return res;
            });
        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
            return result;
        }
    }

    /**
     * Find an existing Galactico webhook on the repo by matching payload URL.
     */
    private Map<String, Object> findExistingWebhook(CloseableHttpClient httpClient, String owner, String repo, String payloadUrl, String token) throws IOException {
        HttpGet httpGet = new HttpGet(GITHUB_API_URL + "/repos/" + owner + "/" + repo + "/hooks");
        httpGet.setHeader("Authorization", "token " + token);
        httpGet.setHeader("Accept", "application/vnd.github.v3+json");

        return httpClient.execute(httpGet, response -> {
            if (response.getCode() != 200) return null;
            JsonNode hooks = objectMapper.readTree(response.getEntity().getContent());
            for (JsonNode hook : hooks) {
                JsonNode config = hook.get("config");
                if (config != null && config.has("url")) {
                    String hookUrl = config.get("url").asText();
                    if (hookUrl.equals(payloadUrl)) {
                        Map<String, Object> found = new LinkedHashMap<>();
                        found.put("id", hook.get("id").asLong());
                        found.put("active", hook.get("active").asBoolean());
                        if (hook.has("events")) {
                            var events = new java.util.ArrayList<String>();
                            hook.get("events").forEach(e -> events.add(e.asText()));
                            found.put("events", events);
                        }
                        if (hook.has("last_response")) {
                            JsonNode lr = hook.get("last_response");
                            Map<String, Object> lastResp = new LinkedHashMap<>();
                            lastResp.put("code", lr.has("code") ? lr.get("code").asInt() : null);
                            lastResp.put("status", lr.has("status") ? lr.get("status").asText() : null);
                            lastResp.put("message", lr.has("message") ? lr.get("message").asText() : null);
                            found.put("lastResponse", lastResp);
                        }
                        return found;
                    }
                }
            }
            return null;
        });
    }
    
    /**
     * Create a GitHub issue for a task.
     * 
     * @param task The task to create an issue for
     * @param project The project containing the task
     * @return The URL of the created issue, or null if creation failed
     */
    public String createGitHubIssue(Task task, Project project) {
        if (project.getGitHubAccessToken() == null || project.getGitHubRepoUrl() == null) {
            return null;
        }
        
        // Extract owner and repo from GitHub URL
        String repoUrl = project.getGitHubRepoUrl();
        String[] parts = repoUrl.replace("https://github.com/", "").split("/");
        if (parts.length < 2) {
            return null;
        }
        
        String owner = parts[0];
        String repo = parts[1];
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Create issue object
            ObjectNode issueJson = objectMapper.createObjectNode();
            issueJson.put("title", task.getFeatureCode() + ": " + task.getTitle());
            
            // Create description with task details
            StringBuilder description = new StringBuilder();
            description.append("## Task Details\n\n");
            description.append("- **Feature Code:** ").append(task.getFeatureCode()).append("\n");
            description.append("- **Status:** ").append(task.getStatus()).append("\n");
            
            if (task.getAssignee() != null) {
                description.append("- **Assignee:** ").append(task.getAssignee().getNickname()).append("\n");
            }
            
            if (task.getMilestone() != null && !task.getMilestone().isEmpty()) {
                description.append("- **Milestone:** ").append(task.getMilestone()).append("\n");
            }
            
            if (!task.getTags().isEmpty()) {
                description.append("- **Tags:** ").append(String.join(", ", task.getTags())).append("\n");
            }
            
            description.append("\n*This issue was automatically created by AutoTrack.*");
            issueJson.put("body", description.toString());
            
            // Create labels based on tags
            if (!task.getTags().isEmpty()) {
                JsonNode labelsNode = objectMapper.createArrayNode();
                for (String tag : task.getTags()) {
                    ((com.fasterxml.jackson.databind.node.ArrayNode) labelsNode).add(tag);
                }
                issueJson.set("labels", labelsNode);
            }
            
            // Create HTTP request
            HttpPost httpPost = new HttpPost(GITHUB_API_URL + "/repos/" + owner + "/" + repo + "/issues");
            httpPost.setHeader("Authorization", "token " + project.getGitHubAccessToken());
            httpPost.setHeader("Accept", "application/vnd.github.v3+json");
            httpPost.setEntity(new StringEntity(objectMapper.writeValueAsString(issueJson), ContentType.APPLICATION_JSON));
            
            // Execute request
            String result = httpClient.execute(httpPost, response -> {
                if (response.getCode() == 201) {
                    JsonNode responseJson = objectMapper.readTree(response.getEntity().getContent());
                    return responseJson.get("html_url").asText();
                }
                return null;
            });
            return result;
        } catch (IOException e) {
            // Log error
            System.err.println("Error creating GitHub issue: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Create or update a file in GitHub repository.
     * 
     * @param project The project containing the repository
     * @param filePath Path to the file in the repository (e.g., ".github/workflows/ci.yml")
     * @param content The file content
     * @param commitMessage The commit message
     * @return The URL of the created/updated file, or null if operation failed
     */
    public String createOrUpdateFile(Project project, String filePath, String content, String commitMessage) {
        if (project.getGitHubAccessToken() == null || project.getGitHubAccessToken().isEmpty()) {
            System.err.println("No GitHub access token available for project: " + project.getName());
            return null;
        }
        
        try {
            // Parse repository URL to get owner and repo name
            String repoUrl = project.getGitHubRepoUrl();
            if (repoUrl == null || repoUrl.isEmpty()) {
                System.err.println("No GitHub repository URL found for project: " + project.getName());
                return null;
            }
            
            String[] parts = parseGitHubRepoUrl(repoUrl);
            if (parts == null) {
                return null;
            }
            String owner = parts[0];
            String repo = parts[1];
            
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                // First, try to get the existing file to get its SHA (required for updates)
                String existingFileSha = getFileSha(httpClient, owner, repo, filePath, project.getGitHubAccessToken());
                
                // Prepare the request body
                ObjectNode requestJson = objectMapper.createObjectNode();
                requestJson.put("message", commitMessage);
                requestJson.put("content", java.util.Base64.getEncoder().encodeToString(content.getBytes()));
                
                if (existingFileSha != null) {
                    // File exists, include SHA for update
                    requestJson.put("sha", existingFileSha);
                }
                
                // Create HTTP request
                HttpPut httpPut = new HttpPut(GITHUB_API_URL + "/repos/" + owner + "/" + repo + "/contents/" + filePath);
                httpPut.setHeader("Authorization", "token " + project.getGitHubAccessToken());
                httpPut.setHeader("Accept", "application/vnd.github.v3+json");
                httpPut.setEntity(new StringEntity(objectMapper.writeValueAsString(requestJson), ContentType.APPLICATION_JSON));
                
                // Execute request
                String result = httpClient.execute(httpPut, response -> {
                    if (response.getCode() == 200 || response.getCode() == 201) {
                        try {
                            JsonNode responseJson = objectMapper.readTree(response.getEntity().getContent());
                            return responseJson.get("content").get("html_url").asText();
                        } catch (Exception e) {
                            System.err.println("Error parsing GitHub API response: " + e.getMessage());
                            return "File updated successfully";
                        }
                    } else {
                        System.err.println("GitHub API request failed with status: " + response.getCode());
                        return null;
                    }
                });
                return result;
            }
        } catch (IOException e) {
            System.err.println("Error creating/updating GitHub file: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get the SHA of an existing file in the repository.
     */
    private String getFileSha(CloseableHttpClient httpClient, String owner, String repo, String filePath, String token) {
        try {
            HttpGet httpGet = new HttpGet(GITHUB_API_URL + "/repos/" + owner + "/" + repo + "/contents/" + filePath);
            httpGet.setHeader("Authorization", "token " + token);
            httpGet.setHeader("Accept", "application/vnd.github.v3+json");
            
            return httpClient.execute(httpGet, response -> {
                if (response.getCode() == 200) {
                    try {
                        JsonNode responseJson = objectMapper.readTree(response.getEntity().getContent());
                        return responseJson.get("sha").asText();
                    } catch (Exception e) {
                        return null;
                    }
                }
                return null; // File doesn't exist
            });
        } catch (IOException e) {
            return null; // File doesn't exist or error occurred
        }
    }
    
    /**
     * Parse GitHub repository URL to extract owner and repository name.
     */
    private String[] parseGitHubRepoUrl(String repoUrl) {
        try {
            // Handle different GitHub URL formats
            String cleanUrl = repoUrl;
            if (cleanUrl.endsWith(".git")) {
                cleanUrl = cleanUrl.substring(0, cleanUrl.length() - 4);
            }
            
            // Extract from https://github.com/owner/repo or git@github.com:owner/repo.git
            if (cleanUrl.contains("github.com/")) {
                String[] urlParts = cleanUrl.split("github.com/")[1].split("/");
                if (urlParts.length >= 2) {
                    return new String[]{urlParts[0], urlParts[1]};
                }
            } else if (cleanUrl.contains("github.com:")) {
                String[] urlParts = cleanUrl.split("github.com:")[1].split("/");
                if (urlParts.length >= 2) {
                    return new String[]{urlParts[0], urlParts[1]};
                }
            }
            
            System.err.println("Unable to parse GitHub repository URL: " + repoUrl);
            return null;
        } catch (Exception e) {
            System.err.println("Error parsing GitHub repository URL: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get repository information from GitHub API.
     * 
     * @param user The user making the request
     * @param repoUrl The GitHub repository URL
     * @return Repository information as JsonNode, or null if failed
     */
    public JsonNode getRepository(User user, String repoUrl) {
        if (user == null || repoUrl == null || repoUrl.isEmpty()) {
            return null;
        }
        
        String[] parts = parseGitHubRepoUrl(repoUrl);
        if (parts == null) {
            return null;
        }
        String owner = parts[0];
        String repo = parts[1];
        
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String apiUrl = String.format("https://api.github.com/repos/%s/%s", owner, repo);
            HttpGet httpGet = new HttpGet(apiUrl);
            
            // GitHub API doesn't require authentication for public repositories
            // For private repos, would need project's access token
            httpGet.setHeader("Accept", "application/vnd.github.v3+json");
            httpGet.setHeader("User-Agent", "AutoTrack-Application");
            
            ObjectMapper objectMapper = new ObjectMapper();
            
            return httpClient.execute(httpGet, response -> {
                if (response.getCode() == 200) {
                    try {
                        return objectMapper.readTree(response.getEntity().getContent());
                    } catch (Exception e) {
                        System.err.println("Error parsing repository response: " + e.getMessage());
                        return null;
                    }
                } else {
                    System.err.println("Failed to get repository info. Status: " + response.getCode());
                    return null;
                }
            });
        } catch (Exception e) {
            System.err.println("Error fetching repository info: " + e.getMessage());
            return null;
        }
    }
}

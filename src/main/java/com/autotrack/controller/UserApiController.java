package com.autotrack.controller;
import com.autotrack.model.User;
import com.autotrack.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserApiController {
    private final UserService userService;
    private final RestTemplate restTemplate;

    public UserApiController(UserService userService) {
        this.userService = userService;
        this.restTemplate = new RestTemplate();
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @AuthenticationPrincipal OAuth2User principal,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String githubId = null;
            
            // First try OAuth2 authentication
            if (principal != null) {
                githubId = principal.getAttribute("login");
            } else {
                // Fallback to Bearer token authentication
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    
                    // Validate GitHub token and get user info
                    Map<String, Object> githubUser = validateGitHubToken(token);
                    if (githubUser != null) {
                        githubId = (String) githubUser.get("login");
                    } else {
                        response.put("error", "Invalid GitHub token");
                        return ResponseEntity.status(401).body(response);
                    }
                } else {
                    response.put("error", "Not authenticated - no OAuth2 session or Bearer token");
                    return ResponseEntity.status(401).body(response);
                }
            }
            
            if (githubId == null || githubId.isEmpty()) {
                response.put("error", "Unable to determine GitHub ID");
                return ResponseEntity.status(401).body(response);
            }

            User user = userService.getUserByGitHubId(githubId);
            if (user == null) {
                // For extension users, create a basic user if they don't exist
                user = userService.createUser(githubId, githubId, githubId + "@github.com", null);
            }

            response.put("success", true);
            response.put("user", userToMap(user));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("error", "Failed to fetch user: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    private Map<String, Object> validateGitHubToken(String token) {
        try {
            // Create headers with Authorization
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.set("Accept", "application/vnd.github.v3+json");
            headers.set("User-Agent", "AutoTrack-Extension");

            // Create request entity
            RequestEntity<Void> requestEntity = new RequestEntity<>(
                headers, 
                HttpMethod.GET, 
                URI.create("https://api.github.com/user")
            );

            // Make the API call
            ResponseEntity<Map> apiResponse = restTemplate.exchange(requestEntity, Map.class);
            
            if (apiResponse.getStatusCode().is2xxSuccessful() && apiResponse.getBody() != null) {
                Map<String, Object> userInfo = apiResponse.getBody();
                
                // Validate that we have required fields
                if (userInfo.get("login") != null) {
                    return userInfo;
                }
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("Error validating GitHub token: " + e.getMessage());
            return null;
        }
    }

    private Map<String, Object> userToMap(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("nickname", user.getNickname());
        userMap.put("email", user.getEmail() != null ? user.getEmail() : "");
        userMap.put("githubId", user.getGitHubId());
        userMap.put("avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "");
        return userMap;
    }
}

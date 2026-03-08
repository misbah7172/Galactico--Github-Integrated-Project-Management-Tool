package com.autotrack.controller;

import com.autotrack.model.Project;
import com.autotrack.model.User;
import com.autotrack.service.ProjectService;
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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectApiController {
    
    private final ProjectService projectService;
    private final UserService userService;
    private final RestTemplate restTemplate;

    public ProjectApiController(ProjectService projectService, UserService userService) {
        this.projectService = projectService;
        this.userService = userService;
        this.restTemplate = new RestTemplate();
    }

    @GetMapping("/user-projects")
    public ResponseEntity<List<Project>> getUserProjects(
            @AuthenticationPrincipal OAuth2User principal,
            HttpServletRequest request) {
        try {
            // Try to get current user from OAuth2
            User currentUser = getCurrentUser(principal, request);
            
            // If OAuth2 user not available, try to get from session
            if (currentUser == null) {
                Object sessionUser = request.getSession().getAttribute("currentUser");
                if (sessionUser instanceof User) {
                    currentUser = (User) sessionUser;
                }
            }
            
            // If still no user, return empty list instead of 401 to avoid blocking the UI
            if (currentUser == null) {
                System.out.println("No authenticated user found for project list request");
                return ResponseEntity.ok(List.of()); // Return empty list
            }
            
            List<Project> userProjects = projectService.getProjectsByUser(currentUser);
            return ResponseEntity.ok(userProjects);
            
        } catch (Exception e) {
            System.err.println("Error in getUserProjects: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(List.of()); // Return empty list instead of 500 error
        }
    }

    private User getCurrentUser(OAuth2User principal, HttpServletRequest request) {
        String githubId = null;
        
        if (principal != null) {
            githubId = principal.getAttribute("login");
        } else {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                Map<String, Object> githubUser = validateGitHubToken(token);
                if (githubUser != null) {
                    githubId = (String) githubUser.get("login");
                }
            }
        }
        
        if (githubId != null) {
            try {
                return userService.getUserByGitHubId(githubId);
            } catch (Exception e) {
                return userService.createUser(githubId, githubId, githubId + "@github.com", null);
            }
        }
        
        return null;
    }

    private Map<String, Object> validateGitHubToken(String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.set("Accept", "application/vnd.github.v3+json");
            headers.set("User-Agent", "AutoTrack-Extension");

            RequestEntity<Void> requestEntity = new RequestEntity<>(
                headers, 
                HttpMethod.GET, 
                URI.create("https://api.github.com/user")
            );

            ResponseEntity<Map> apiResponse = restTemplate.exchange(requestEntity, Map.class);
            
            if (apiResponse.getStatusCode().is2xxSuccessful() && apiResponse.getBody() != null) {
                Map<String, Object> userInfo = apiResponse.getBody();
                
                if (userInfo.get("login") != null) {
                    return userInfo;
                }
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
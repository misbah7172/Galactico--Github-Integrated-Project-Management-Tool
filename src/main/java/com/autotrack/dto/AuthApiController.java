package com.autotrack.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(@AuthenticationPrincipal OAuth2User principal) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (principal == null) {
                response.put("authenticated", false);
                response.put("message", "Not authenticated");
                return ResponseEntity.ok(response);
            }
            String githubId = principal.getAttribute("login");
            String name = principal.getAttribute("name");
            String avatarUrl = principal.getAttribute("avatar_url");
            response.put("authenticated", true);
            response.put("githubId", githubId);
            response.put("name", name);
            response.put("avatarUrl", avatarUrl);
            response.put("message", "Successfully authenticated");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("authenticated", false);
            response.put("error", "Failed to get authentication status: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    @PostMapping("/github")
    public ResponseEntity<Map<String, Object>> authenticateWithGitHub(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String accessToken = request.get("accessToken");
            String username = request.get("username");
            String email = request.get("email");
            
            if (accessToken == null || username == null) {
                response.put("success", false);
                response.put("message", "Missing required parameters");
                return ResponseEntity.badRequest().body(response);
            }
            
            // For now, just return success with a mock token
            // In production, you would verify the GitHub token and create a JWT
            response.put("success", true);
            response.put("token", "mock-jwt-token-" + username);
            response.put("user", Map.of(
                "username", username,
                "email", email != null ? email : username + "@github.com",
                "avatarUrl", "https://github.com/" + username + ".png"
            ));
            response.put("message", "Authentication successful");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Authentication failed: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}

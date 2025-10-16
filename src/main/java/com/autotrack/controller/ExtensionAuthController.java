package com.autotrack.controller;

import com.autotrack.model.User;
import com.autotrack.service.UserService;
import com.autotrack.service.ExtensionAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Authentication API controller for VS Code extension integration.
 * Handles extension authentication with Galactico backend.
 */
@RestController
@RequestMapping("/api/auth")
public class ExtensionAuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private ExtensionAuthService authService;

    /**
     * Generate authentication URL for VS Code extension
     */
    @GetMapping("/login-url")
    public ResponseEntity<Map<String, Object>> getLoginUrl() {
        // Generate a unique state parameter for security with extension prefix
        String state = "extension_" + UUID.randomUUID().toString();
        String baseUrl = System.getenv("APP_BASE_URL") != null ? 
            System.getenv("APP_BASE_URL") : "https://misbah7172.loca.lt";
        String loginUrl = baseUrl + "/oauth2/authorization/github?state=" + state;
        
        Map<String, Object> response = new HashMap<>();
        response.put("loginUrl", loginUrl);
        response.put("state", state);
        response.put("callbackUrl", baseUrl + "/api/auth/token-display");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Token display page - accessed after successful OAuth authentication
     */
    @GetMapping("/token-display")
    public ResponseEntity<String> displayToken(@AuthenticationPrincipal OAuth2User principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(401)
                    .body("User not authenticated. Please complete GitHub OAuth first.");
            }

            // Get user info from GitHub OAuth
            String githubUsername = principal.getAttribute("login");
            String name = principal.getAttribute("name");
            Object githubIdObj = principal.getAttribute("id");
            
            if (githubIdObj == null || githubUsername == null) {
                return ResponseEntity.badRequest().body("Invalid GitHub user data");
            }
            
            String githubId = githubIdObj.toString();

            // Find user in database by GitHub ID
            User user = userService.getUserByGitHubId(githubId);
            if (user == null) {
                // If not found by GitHub ID, try by username  
                user = userService.getUserByNickname(githubUsername);
            }
            
            if (user == null) {
                return ResponseEntity.status(404)
                    .body("User not found in Galactico database. Please ensure your GitHub account is registered.");
            }

            // Generate extension access token
            String extensionToken = authService.generateToken(user);

            // Return beautiful token display page
            String htmlResponse = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Galactico Extension Authentication</title>
                    <style>
                        body { 
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; 
                            text-align: center; 
                            padding: 30px; 
                            background: linear-gradient(135deg, rgb(102, 126, 234) 0%%, rgb(118, 75, 162) 100%%);
                            color: white;
                            margin: 0;
                            min-height: 100vh;
                            display: flex;
                            flex-direction: column;
                            justify-content: center;
                        }
                        .container {
                            background: rgba(255, 255, 255, 0.1);
                            backdrop-filter: blur(10px);
                            border-radius: 15px;
                            padding: 40px;
                            max-width: 600px;
                            margin: 0 auto;
                            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
                        }
                        .success { color: rgb(74, 222, 128); font-size: 24px; margin-bottom: 20px; }
                        .token-section { 
                            background: rgba(0, 0, 0, 0.2); 
                            padding: 20px; 
                            border-radius: 10px; 
                            margin: 20px 0;
                            border: 2px dashed rgb(74, 222, 128);
                        }
                        .token { 
                            font-family: 'Courier New', monospace; 
                            font-size: 14px;
                            word-break: break-all;
                            background: rgba(255, 255, 255, 0.1);
                            padding: 15px;
                            border-radius: 8px;
                            margin: 10px 0;
                            border: 1px solid rgba(255, 255, 255, 0.2);
                            user-select: all;
                        }
                        .copy-btn {
                            background: rgb(74, 222, 128);
                            color: black;
                            border: none;
                            padding: 12px 20px;
                            border-radius: 8px;
                            cursor: pointer;
                            font-weight: bold;
                            margin: 10px;
                            transition: all 0.3s;
                        }
                        .copy-btn:hover {
                            background: rgb(34, 197, 94);
                            transform: scale(1.05);
                        }
                        .instructions {
                            font-size: 16px;
                            margin: 20px 0;
                            line-height: 1.6;
                        }
                        .highlight {
                            color: rgb(251, 191, 36);
                            font-weight: bold;
                        }
                        .pulse {
                            animation: pulse 2s infinite;
                        }
                        @keyframes pulse {
                            0%% { opacity: 1; }
                            50%% { opacity: 0.7; }
                            100%% { opacity: 1; }
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1 class="success">🎉 Authentication Successful!</h1>
                        <p class="instructions">Your Galactico extension has been authenticated successfully.</p>
                        <p><strong>User:</strong> %s (%s)</p>
                        
                        <div class="token-section pulse">
                            <h3 class="highlight">📋 Your Access Token</h3>
                            <p>Copy this token and paste it into VS Code:</p>
                            <div class="token" id="token">%s</div>
                            <button class="copy-btn" onclick="copyToken()">📋 Copy Token</button>
                            <div id="copyStatus" style="margin-top: 10px; color: rgb(74, 222, 128); display: none;">
                                ✅ Token copied to clipboard!
                            </div>
                        </div>
                        
                        <div class="instructions">
                            <p><strong>Next Steps:</strong></p>
                            <p>1. Return to VS Code</p>
                            <p>2. Paste the token when prompted</p>
                            <p>3. Start using your Galactico dashboard!</p>
                        </div>
                        
                        <p><small>You can close this window after copying the token.</small></p>
                    </div>
                    
                    <script>
                        function copyToken() {
                            const token = document.getElementById('token').textContent;
                            navigator.clipboard.writeText(token).then(() => {
                                document.getElementById('copyStatus').style.display = 'block';
                                const btn = document.querySelector('.copy-btn');
                                btn.textContent = '✅ Copied!';
                                btn.style.background = 'rgb(34, 197, 94)';
                                setTimeout(() => {
                                    btn.textContent = '📋 Copy Token';
                                    btn.style.background = 'rgb(74, 222, 128)';
                                }, 2000);
                            }).catch(() => {
                                alert('Failed to copy token. Please select and copy manually.');
                            });
                        }
                        
                        // Auto-copy token to clipboard on page load
                        window.onload = function() {
                            copyToken();
                        };
                        
                        // Make token selectable by clicking
                        document.getElementById('token').onclick = function() {
                            window.getSelection().selectAllChildren(this);
                        };
                    </script>
                </body>
                </html>
                """;

            return ResponseEntity.ok()
                .header("Content-Type", "text/html")
                .body(String.format(htmlResponse, name, githubUsername, extensionToken));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body("Authentication failed: " + e.getMessage());
        }
    }

    /**
     * Test endpoint to simulate token display (for debugging only)
     */
    @GetMapping("/test-token")
    public ResponseEntity<String> testToken() {
        try {
            // Create a test user for demonstration
            User testUser = new User();
            testUser.setId(1L);
            testUser.setNickname("testuser");
            testUser.setGitHubId("123456");
            
            // Generate extension access token
            String extensionToken = authService.generateToken(testUser);

            // Return beautiful token display page
            String htmlResponse = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Galactico Extension Authentication</title>
                    <style>
                        body { 
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; 
                            text-align: center; 
                            padding: 30px; 
                            background: linear-gradient(135deg, rgb(102, 126, 234) 0%%, rgb(118, 75, 162) 100%%);
                            color: white;
                            margin: 0;
                            min-height: 100vh;
                            display: flex;
                            flex-direction: column;
                            justify-content: center;
                        }
                        .container {
                            background: rgba(255, 255, 255, 0.1);
                            backdrop-filter: blur(10px);
                            border-radius: 15px;
                            padding: 40px;
                            max-width: 600px;
                            margin: 0 auto;
                            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
                        }
                        .success { color: rgb(74, 222, 128); font-size: 24px; margin-bottom: 20px; }
                        .token-section { 
                            background: rgba(0, 0, 0, 0.2); 
                            padding: 20px; 
                            border-radius: 10px; 
                            margin: 20px 0;
                            border: 2px dashed rgb(74, 222, 128);
                        }
                        .token { 
                            font-family: 'Courier New', monospace; 
                            font-size: 14px;
                            word-break: break-all;
                            background: rgba(255, 255, 255, 0.1);
                            padding: 15px;
                            border-radius: 8px;
                            margin: 10px 0;
                            border: 1px solid rgba(255, 255, 255, 0.2);
                            user-select: all;
                        }
                        .copy-btn {
                            background: rgb(74, 222, 128);
                            color: black;
                            border: none;
                            padding: 12px 20px;
                            border-radius: 8px;
                            cursor: pointer;
                            font-weight: bold;
                            margin: 10px;
                            transition: all 0.3s;
                        }
                        .copy-btn:hover {
                            background: rgb(34, 197, 94);
                            transform: scale(1.05);
                        }
                        .instructions {
                            font-size: 16px;
                            margin: 20px 0;
                            line-height: 1.6;
                        }
                        .highlight {
                            color: rgb(251, 191, 36);
                            font-weight: bold;
                        }
                        .pulse {
                            animation: pulse 2s infinite;
                        }
                        @keyframes pulse {
                            0%% { opacity: 1; }
                            50%% { opacity: 0.7; }
                            100%% { opacity: 1; }
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1 class="success">🎉 Authentication Successful! (Test Mode)</h1>
                        <p class="instructions">Your Galactico extension has been authenticated successfully.</p>
                        <p><strong>User:</strong> %s (%s)</p>
                        
                        <div class="token-section pulse">
                            <h3 class="highlight">📋 Your Access Token</h3>
                            <p>Copy this token and paste it into VS Code:</p>
                            <div class="token" id="token">%s</div>
                            <button class="copy-btn" onclick="copyToken()">📋 Copy Token</button>
                            <div id="copyStatus" style="margin-top: 10px; color: rgb(74, 222, 128); display: none;">
                                ✅ Token copied to clipboard!
                            </div>
                        </div>
                        
                        <div class="instructions">
                            <p><strong>Next Steps:</strong></p>
                            <p>1. Return to VS Code</p>
                            <p>2. Paste the token when prompted</p>
                            <p>3. Start using your Galactico dashboard!</p>
                        </div>
                        
                        <p><small>You can close this window after copying the token.</small></p>
                    </div>
                    
                    <script>
                        function copyToken() {
                            const token = document.getElementById('token').textContent;
                            navigator.clipboard.writeText(token).then(() => {
                                document.getElementById('copyStatus').style.display = 'block';
                                const btn = document.querySelector('.copy-btn');
                                btn.textContent = '✅ Copied!';
                                btn.style.background = 'rgb(34, 197, 94)';
                                setTimeout(() => {
                                    btn.textContent = '📋 Copy Token';
                                    btn.style.background = 'rgb(74, 222, 128)';
                                }, 2000);
                            }).catch(() => {
                                alert('Failed to copy token. Please select and copy manually.');
                            });
                        }
                        
                        // Auto-copy token to clipboard on page load
                        window.onload = function() {
                            copyToken();
                        };
                        
                        // Make token selectable by clicking
                        document.getElementById('token').onclick = function() {
                            window.getSelection().selectAllChildren(this);
                        };
                    </script>
                </body>
                </html>
                """;

            return ResponseEntity.ok()
                .header("Content-Type", "text/html")
                .body(String.format(htmlResponse, "Test User", testUser.getNickname(), extensionToken));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body("Authentication failed: " + e.getMessage());
        }
    }

    /**
     * Handle OAuth callback and generate extension token
     */
    @GetMapping("/callback")
    public ResponseEntity<String> handleCallback(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String code,
            @AuthenticationPrincipal OAuth2User principal) {
        
        try {
            // Verify this is an extension authentication request
            if (state == null || !state.startsWith("extension_")) {
                return ResponseEntity.status(400)
                    .body("Invalid extension authentication request");
            }

            if (principal == null) {
                // Redirect to GitHub OAuth if not authenticated
                return ResponseEntity.status(302)
                    .header("Location", "/oauth2/authorization/github?state=" + state)
                    .body("Redirecting to GitHub authentication...");
            }

            // Get user info from GitHub OAuth
            String githubUsername = principal.getAttribute("login");
            String name = principal.getAttribute("name");
            Object githubIdObj = principal.getAttribute("id");
            
            if (githubIdObj == null || githubUsername == null) {
                return ResponseEntity.badRequest().body("Invalid GitHub user data");
            }
            
            String githubId = githubIdObj.toString();

            // Find user in database by GitHub ID
            User user = userService.getUserByGitHubId(githubId);
            if (user == null) {
                // If not found by GitHub ID, try by username  
                user = userService.getUserByNickname(githubUsername);
            }
            
            if (user == null) {
                return ResponseEntity.status(404)
                    .body("User not found in Galactico database. Please ensure your GitHub account is registered.");
            }

            // Generate extension access token
            String extensionToken = authService.generateToken(user);

            // Return success page with token for extension to capture
            String htmlResponse = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Galactico Extension Authentication</title>
                    <style>
                        body { 
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; 
                            text-align: center; 
                            padding: 30px; 
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            color: white;
                            margin: 0;
                            min-height: 100vh;
                            display: flex;
                            flex-direction: column;
                            justify-content: center;
                        }
                        .container {
                            background: rgba(255, 255, 255, 0.1);
                            backdrop-filter: blur(10px);
                            border-radius: 15px;
                            padding: 40px;
                            max-width: 600px;
                            margin: 0 auto;
                            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
                        }
                        .success { color: #4ade80; font-size: 24px; margin-bottom: 20px; }
                        .token-section { 
                            background: rgba(0, 0, 0, 0.2); 
                            padding: 20px; 
                            border-radius: 10px; 
                            margin: 20px 0;
                            border: 2px dashed #4ade80;
                        }
                        .token { 
                            font-family: 'Courier New', monospace; 
                            font-size: 14px;
                            word-break: break-all;
                            background: rgba(255, 255, 255, 0.1);
                            padding: 15px;
                            border-radius: 8px;
                            margin: 10px 0;
                            border: 1px solid rgba(255, 255, 255, 0.2);
                            user-select: all;
                        }
                        .copy-btn {
                            background: #4ade80;
                            color: black;
                            border: none;
                            padding: 12px 20px;
                            border-radius: 8px;
                            cursor: pointer;
                            font-weight: bold;
                            margin: 10px;
                            transition: all 0.3s;
                        }
                        .copy-btn:hover {
                            background: #22c55e;
                            transform: scale(1.05);
                        }
                        .instructions {
                            font-size: 16px;
                            margin: 20px 0;
                            line-height: 1.6;
                        }
                        .highlight {
                            color: #fbbf24;
                            font-weight: bold;
                        }
                        .pulse {
                            animation: pulse 2s infinite;
                        }
                        @keyframes pulse {
                            0% { opacity: 1; }
                            50% { opacity: 0.7; }
                            100% { opacity: 1; }
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1 class="success">🎉 Authentication Successful!</h1>
                        <p class="instructions">Your Galactico extension has been authenticated successfully.</p>
                        <p><strong>User:</strong> %s (%s)</p>
                        
                        <div class="token-section pulse">
                            <h3 class="highlight">📋 Your Access Token</h3>
                            <p>Copy this token and paste it into VS Code:</p>
                            <div class="token" id="token">%s</div>
                            <button class="copy-btn" onclick="copyToken()">📋 Copy Token</button>
                            <div id="copyStatus" style="margin-top: 10px; color: #4ade80; display: none;">
                                ✅ Token copied to clipboard!
                            </div>
                        </div>
                        
                        <div class="instructions">
                            <p><strong>Next Steps:</strong></p>
                            <p>1. Return to VS Code</p>
                            <p>2. Paste the token when prompted</p>
                            <p>3. Start using your Galactico dashboard!</p>
                        </div>
                        
                        <p><small>You can close this window after copying the token.</small></p>
                    </div>
                    
                    <script>
                        function copyToken() {
                            const token = document.getElementById('token').textContent;
                            navigator.clipboard.writeText(token).then(() => {
                                document.getElementById('copyStatus').style.display = 'block';
                                const btn = document.querySelector('.copy-btn');
                                btn.textContent = '✅ Copied!';
                                btn.style.background = '#22c55e';
                                setTimeout(() => {
                                    btn.textContent = '📋 Copy Token';
                                    btn.style.background = '#4ade80';
                                }, 2000);
                            }).catch(() => {
                                alert('Failed to copy token. Please select and copy manually.');
                            });
                        }
                        
                        // Auto-copy token to clipboard on page load
                        window.onload = function() {
                            copyToken();
                        };
                        
                        // Make token selectable by clicking
                        document.getElementById('token').onclick = function() {
                            window.getSelection().selectAllChildren(this);
                        };
                    </script>
                </body>
                </html>
                """.formatted(name, githubUsername, extensionToken);

            return ResponseEntity.ok()
                .header("Content-Type", "text/html")
                .body(htmlResponse);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body("Authentication failed: " + e.getMessage());
        }
    }

    /**
     * Validate extension token and get user info
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            User user = authService.authenticateUser(authHeader);
            if (user == null) {
                return ResponseEntity.status(401).build();
            }

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getNickname());
            userInfo.put("email", user.getEmail());
            userInfo.put("githubId", user.getGitHubId());
            userInfo.put("authenticated", true);

            return ResponseEntity.ok(userInfo);

        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }

    /**
     * Revoke extension token (logout)
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authService.extractTokenFromHeader(authHeader);
            boolean revoked = false;
            if (token != null) {
                revoked = authService.revokeToken(token);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", revoked ? "Logged out successfully" : "Token not found");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get all active extension sessions (for admin)
     */
    @GetMapping("/sessions")
    public ResponseEntity<Map<String, Object>> getActiveSessions() {
        Map<String, Object> response = new HashMap<>();
        response.put("activeTokens", authService.getActiveTokenCount());
        return ResponseEntity.ok(response);
    }
}

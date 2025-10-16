package com.autotrack.service;

import com.autotrack.model.User;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing VS Code extension authentication tokens.
 * Centralizes token management for all controllers.
 */
@Service
public class ExtensionAuthService {

    // Store active extension sessions (in production, use Redis or database)
    private final Map<String, String> extensionTokens = new ConcurrentHashMap<>();
    private final Map<String, User> tokenToUser = new ConcurrentHashMap<>();
    private final Map<String, Long> tokenCreationTime = new ConcurrentHashMap<>();

    // Token expiration time (24 hours)
    private static final long TOKEN_EXPIRATION_MS = 24 * 60 * 60 * 1000;

    /**
     * Generate a new extension token for a user
     */
    public String generateToken(User user) {
        String token = "gal_" + UUID.randomUUID().toString().replace("-", "");
        
        extensionTokens.put(token, user.getNickname());
        tokenToUser.put(token, user);
        tokenCreationTime.put(token, System.currentTimeMillis());
        
        return token;
    }

    /**
     * Validate token and get associated user
     */
    public User getUserByToken(String token) {
        if (token == null || !tokenToUser.containsKey(token)) {
            return null;
        }

        // Check if token is expired
        Long creationTime = tokenCreationTime.get(token);
        if (creationTime != null && System.currentTimeMillis() - creationTime > TOKEN_EXPIRATION_MS) {
            // Token expired, remove it
            revokeToken(token);
            return null;
        }

        return tokenToUser.get(token);
    }

    /**
     * Revoke a token (logout)
     */
    public boolean revokeToken(String token) {
        boolean existed = extensionTokens.containsKey(token);
        
        extensionTokens.remove(token);
        tokenToUser.remove(token);
        tokenCreationTime.remove(token);
        
        return existed;
    }

    /**
     * Get all active tokens count
     */
    public int getActiveTokenCount() {
        return extensionTokens.size();
    }

    /**
     * Extract token from Authorization header
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * Authenticate user from Authorization header
     */
    public User authenticateUser(String authHeader) {
        String token = extractTokenFromHeader(authHeader);
        if (token == null) {
            return null;
        }
        return getUserByToken(token);
    }

    /**
     * Clean up expired tokens
     */
    public void cleanupExpiredTokens() {
        long now = System.currentTimeMillis();
        
        tokenCreationTime.entrySet().removeIf(entry -> {
            if (now - entry.getValue() > TOKEN_EXPIRATION_MS) {
                String token = entry.getKey();
                extensionTokens.remove(token);
                tokenToUser.remove(token);
                return true;
            }
            return false;
        });
    }
}

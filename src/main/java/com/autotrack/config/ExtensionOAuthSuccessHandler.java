package com.autotrack.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom OAuth success handler to route extension authentication to the token display page
 */
@Component
public class ExtensionOAuthSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, 
                                      Authentication authentication) throws IOException {
        
        // Check if this is an extension authentication request
        String state = request.getParameter("state");
        if (state != null && state.startsWith("extension_")) {
            // Redirect to our token display page
            response.sendRedirect("/api/auth/token-display");
        } else {
            // Regular web authentication - redirect to dashboard
            response.sendRedirect("/dashboard");
        }
    }
}

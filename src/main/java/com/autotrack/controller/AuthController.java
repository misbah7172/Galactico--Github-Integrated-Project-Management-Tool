package com.autotrack.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

/**
 * Controller for authentication related endpoints.
 */
@Controller
public class AuthController {

    /**
     * Login page endpoint.
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * Logout endpoint - handles logout and redirects to home page.
     */
    @PostMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }
        return "redirect:/?logout=true";
    }

}

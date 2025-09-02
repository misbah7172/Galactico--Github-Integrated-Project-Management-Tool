package com.autotrack.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

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


}

package com.autotrack.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for home page.
 */
@Controller
public class HomeController {

    /**
     * Home page endpoint.
     */
    @GetMapping("/")
    public String home() {
        return "index";
    }
}
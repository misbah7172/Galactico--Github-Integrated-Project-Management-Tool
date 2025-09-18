package com.autotrack.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Custom error controller to handle authentication and other errors gracefully
 */
@Controller
public class CustomErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String errorMessage = "An unexpected error occurred";
        String errorDetails = "";

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            
            switch (statusCode) {
                case 403:
                    errorMessage = "Access Denied";
                    errorDetails = "GitHub authentication failed. This might be due to:\n" +
                                 "• Invalid OAuth configuration\n" +
                                 "• User denied access to the application\n" +
                                 "• GitHub OAuth app settings mismatch";
                    break;
                case 404:
                    errorMessage = "Page Not Found";
                    errorDetails = "The requested page could not be found.";
                    break;
                case 500:
                    errorMessage = "Internal Server Error";
                    errorDetails = "An internal server error occurred. Please try again later.";
                    break;
                default:
                    errorMessage = "Error " + statusCode;
                    errorDetails = "An error occurred while processing your request.";
            }
        }

        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("errorDetails", errorDetails);
        model.addAttribute("homeUrl", "/");
        model.addAttribute("loginUrl", "/login");
        
        return "error";
    }
}
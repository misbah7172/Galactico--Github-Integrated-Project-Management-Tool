package com.autotrack.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

/**
 * Configuration class for Spring MVC with performance optimizations.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("login");
        registry.addViewController("/").setViewName("index");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String baseUrl = System.getenv("APP_BASE_URL") != null ? 
            System.getenv("APP_BASE_URL") : "https://misbah7172.loca.lt";
        
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:*", baseUrl, "vscode-webview://*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Cache static assets for better performance
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365))
                        .cachePublic()
                        .mustRevalidate());

        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365))
                        .cachePublic()
                        .mustRevalidate());

        registry.addResourceHandler("/images/**", "/img/**")
                .addResourceLocations("classpath:/static/images/", "classpath:/static/img/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30))
                        .cachePublic());

        registry.addResourceHandler("/favicon.png", "/favicon.ico")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30))
                        .cachePublic());

        // Default resource handler for other static content
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(1))
                        .cachePublic());
    }
}

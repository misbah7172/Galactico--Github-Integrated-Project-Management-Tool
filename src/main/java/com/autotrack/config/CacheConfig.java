package com.autotrack.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "teams", "userTeams", "projects", "userProjects",
                "tasks", "users", "pendingCommits", "approvedCommits",
                "projectOverview", "dailyActivity", "commitSizeDistribution",
                "topContributors", "fileTypeDistribution", "commitTrends",
                "codeQuality", "productivityTrends", "developerPerformance",
                "teamCollaboration"
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats());
        return cacheManager;
    }
}

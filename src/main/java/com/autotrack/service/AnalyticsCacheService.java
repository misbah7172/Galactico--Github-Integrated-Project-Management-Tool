package com.autotrack.service;

import com.autotrack.model.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for caching analytics data to improve performance.
 * Provides intelligent caching with automatic invalidation and refresh.
 */
@Service
public class AnalyticsCacheService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsCacheService.class);

    @Autowired
    private CommitAnalyticsService commitAnalyticsService;

    // In-memory cache for frequently accessed data
    private final Map<String, CachedMetric> metricsCache = new ConcurrentHashMap<>();
    private final Map<Long, LocalDateTime> projectLastUpdated = new ConcurrentHashMap<>();

    /**
     * Cached metric holder with timestamp.
     */
    private static class CachedMetric {
        private final Object data;
        private final LocalDateTime timestamp;
        private final long ttlMinutes;

        public CachedMetric(Object data, long ttlMinutes) {
            this.data = data;
            this.timestamp = LocalDateTime.now();
            this.ttlMinutes = ttlMinutes;
        }

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(timestamp.plusMinutes(ttlMinutes));
        }

        public Object getData() {
            return data;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }

    /**
     * Get cached project overview or compute and cache it.
     */
    @Cacheable(value = "projectOverview", key = "#project.id")
    public Map<String, Object> getCachedProjectOverview(Project project) {
        logger.debug("Computing and caching project overview for project: {}", project.getId());
        return commitAnalyticsService.calculateProjectOverview(project);
    }

    /**
     * Get cached daily activity or compute and cache it.
     */
    @Cacheable(value = "dailyActivity", key = "#project.id + '_' + #days")
    public List<Map<String, Object>> getCachedDailyActivity(Project project, int days) {
        logger.debug("Computing and caching daily activity for project: {} (days: {})", project.getId(), days);
        return commitAnalyticsService.getDailyActivity(project, days);
    }

    /**
     * Get cached commit size distribution or compute and cache it.
     */
    @Cacheable(value = "commitSizeDistribution", key = "#project.id")
    public Map<String, Object> getCachedCommitSizeDistribution(Project project) {
        logger.debug("Computing and caching commit size distribution for project: {}", project.getId());
        return commitAnalyticsService.getCommitSizeDistribution(project);
    }

    /**
     * Get cached top contributors or compute and cache it.
     */
    @Cacheable(value = "topContributors", key = "#project.id + '_' + #limit")
    public List<Map<String, Object>> getCachedTopContributors(Project project, int limit) {
        logger.debug("Computing and caching top contributors for project: {} (limit: {})", project.getId(), limit);
        return commitAnalyticsService.getTopContributors(project, limit);
    }

    /**
     * Get cached file type distribution or compute and cache it.
     */
    @Cacheable(value = "fileTypeDistribution", key = "#project.id")
    public Map<String, Object> getCachedFileTypeDistribution(Project project) {
        logger.debug("Computing and caching file type distribution for project: {}", project.getId());
        return commitAnalyticsService.getFileTypeDistribution(project);
    }

    /**
     * Get cached commit trends or compute and cache it.
     */
    @Cacheable(value = "commitTrends", key = "#project.id + '_' + #months")
    public Map<String, Object> getCachedCommitTrends(Project project, int months) {
        logger.debug("Computing and caching commit trends for project: {} (months: {})", project.getId(), months);
        return commitAnalyticsService.getCommitTrends(project, months);
    }

    /**
     * Get cached code quality metrics or compute and cache it.
     */
    @Cacheable(value = "codeQuality", key = "#project.id")
    public Map<String, Object> getCachedCodeQualityMetrics(Project project) {
        logger.debug("Computing and caching code quality metrics for project: {}", project.getId());
        return commitAnalyticsService.getCodeQualityMetrics(project);
    }

    /**
     * Get cached productivity trends or compute and cache it.
     */
    @Cacheable(value = "productivityTrends", key = "#project.id + '_' + #weeks")
    public Map<String, Object> getCachedProductivityTrends(Project project, int weeks) {
        logger.debug("Computing and caching productivity trends for project: {} (weeks: {})", project.getId(), weeks);
        return commitAnalyticsService.getProductivityTrends(project, weeks);
    }

    /**
     * Get cached developer performance metrics or compute and cache it.
     */
    @Cacheable(value = "developerPerformance", key = "#project.id")
    public Map<String, Object> getCachedDeveloperPerformance(Project project) {
        logger.debug("Computing and caching developer performance for project: {}", project.getId());
        return commitAnalyticsService.getDeveloperPerformanceMetrics(project);
    }

    /**
     * Get cached team collaboration insights or compute and cache it.
     */
    @Cacheable(value = "teamCollaboration", key = "#project.id")
    public Map<String, Object> getCachedTeamCollaboration(Project project) {
        logger.debug("Computing and caching team collaboration for project: {}", project.getId());
        return commitAnalyticsService.getTeamCollaborationInsights(project);
    }

    /**
     * Invalidate all cached data for a specific project.
     */
    @CacheEvict(value = {"projectOverview", "dailyActivity", "commitSizeDistribution", 
                        "topContributors", "fileTypeDistribution", "commitTrends", 
                        "codeQuality", "productivityTrends", "developerPerformance", 
                        "teamCollaboration"}, key = "#project.id")
    public void invalidateProjectCache(Project project) {
        logger.info("Invalidating cache for project: {}", project.getId());
        projectLastUpdated.put(project.getId(), LocalDateTime.now());
        
        // Also clear manual cache entries
        String projectPrefix = "project_" + project.getId() + "_";
        metricsCache.entrySet().removeIf(entry -> entry.getKey().startsWith(projectPrefix));
    }

    /**
     * Invalidate all cached data for a specific project by ID.
     */
    @CacheEvict(value = {"projectOverview", "dailyActivity", "commitSizeDistribution", 
                        "topContributors", "fileTypeDistribution", "commitTrends", 
                        "codeQuality", "productivityTrends", "developerPerformance", 
                        "teamCollaboration"}, key = "#projectId")
    public void invalidateProjectCache(Long projectId) {
        logger.info("Invalidating cache for project ID: {}", projectId);
        projectLastUpdated.put(projectId, LocalDateTime.now());
        
        // Also clear manual cache entries
        String projectPrefix = "project_" + projectId + "_";
        metricsCache.entrySet().removeIf(entry -> entry.getKey().startsWith(projectPrefix));
    }

    /**
     * Manually cache a metric with custom TTL.
     */
    public void cacheMetric(String key, Object data, long ttlMinutes) {
        metricsCache.put(key, new CachedMetric(data, ttlMinutes));
        logger.debug("Cached metric with key: {} (TTL: {} minutes)", key, ttlMinutes);
    }

    /**
     * Get a manually cached metric.
     */
    public Object getCachedMetric(String key) {
        CachedMetric cached = metricsCache.get(key);
        if (cached == null || cached.isExpired()) {
            metricsCache.remove(key);
            return null;
        }
        
        logger.debug("Retrieved cached metric with key: {}", key);
        return cached.getData();
    }

    /**
     * Check if project data has been updated recently.
     */
    public boolean isProjectDataFresh(Long projectId, int minutesThreshold) {
        LocalDateTime lastUpdated = projectLastUpdated.get(projectId);
        if (lastUpdated == null) {
            return false;
        }
        
        return LocalDateTime.now().isBefore(lastUpdated.plusMinutes(minutesThreshold));
    }

    /**
     * Warm up cache for a specific project by pre-computing common metrics.
     */
    public void warmUpProjectCache(Project project) {
        logger.info("Warming up cache for project: {}", project.getId());
        
        try {
            // Pre-compute common metrics
            getCachedProjectOverview(project);
            getCachedDailyActivity(project, 30);
            getCachedCommitSizeDistribution(project);
            getCachedTopContributors(project, 10);
            getCachedFileTypeDistribution(project);
            getCachedCommitTrends(project, 6);
            getCachedCodeQualityMetrics(project);
            getCachedProductivityTrends(project, 12);
            getCachedDeveloperPerformance(project);
            getCachedTeamCollaboration(project);
            
            logger.info("Cache warm-up completed for project: {}", project.getId());
        } catch (Exception e) {
            logger.error("Error warming up cache for project: {}", project.getId(), e);
        }
    }

    /**
     * Scheduled task to clean up expired cache entries.
     * Runs every hour.
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void cleanupExpiredCache() {
        logger.debug("Starting cache cleanup");
        
        int removedCount = 0;
        for (Map.Entry<String, CachedMetric> entry : metricsCache.entrySet()) {
            if (entry.getValue().isExpired()) {
                metricsCache.remove(entry.getKey());
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            logger.info("Cleaned up {} expired cache entries", removedCount);
        }
    }

    /**
     * Get cache statistics for monitoring.
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        stats.put("totalCachedMetrics", metricsCache.size());
        stats.put("trackedProjects", projectLastUpdated.size());
        
        // Count fresh vs expired entries
        long freshEntries = metricsCache.values().stream()
            .mapToLong(metric -> metric.isExpired() ? 0 : 1)
            .sum();
        
        stats.put("freshEntries", freshEntries);
        stats.put("expiredEntries", metricsCache.size() - freshEntries);
        
        logger.debug("Cache statistics: {}", stats);
        return stats;
    }

    /**
     * Clear all cached data (use with caution).
     */
    @CacheEvict(value = {"projectOverview", "dailyActivity", "commitSizeDistribution", 
                        "topContributors", "fileTypeDistribution", "commitTrends", 
                        "codeQuality", "productivityTrends", "developerPerformance", 
                        "teamCollaboration"}, allEntries = true)
    public void clearAllCache() {
        logger.warn("Clearing all analytics cache");
        metricsCache.clear();
        projectLastUpdated.clear();
    }
}
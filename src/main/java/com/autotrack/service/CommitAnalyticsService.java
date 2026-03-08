package com.autotrack.service;

import com.autotrack.model.Commit;
import com.autotrack.model.ContributorStats;
import com.autotrack.model.FileChangeMetrics;
import com.autotrack.model.Project;
import com.autotrack.repository.CommitRepository;
import com.autotrack.repository.ContributorStatsRepository;
import com.autotrack.repository.FileChangeMetricsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CommitAnalyticsService {

    private final CommitRepository commitRepository;
    private final ContributorStatsRepository contributorStatsRepository;
    private final FileChangeMetricsRepository fileChangeMetricsRepository;

    public CommitAnalyticsService(CommitRepository commitRepository,
                                 ContributorStatsRepository contributorStatsRepository,
                                 FileChangeMetricsRepository fileChangeMetricsRepository) {
        this.commitRepository = commitRepository;
        this.contributorStatsRepository = contributorStatsRepository;
        this.fileChangeMetricsRepository = fileChangeMetricsRepository;
    }

    public Map<String, Object> calculateProjectOverview(Project project) {
        Map<String, Object> overview = new HashMap<>();
        
        Long totalCommits = contributorStatsRepository.getTotalCommitsByProject(project);
        Long totalLinesAdded = contributorStatsRepository.getTotalLinesAddedByProject(project);
        Long totalLinesModified = contributorStatsRepository.getTotalLinesModifiedByProject(project);
        Long totalLinesDeleted = contributorStatsRepository.getTotalLinesDeletedByProject(project);
        Long activeContributors = contributorStatsRepository.getActiveContributorCountByProject(project);
        Long totalFiles = fileChangeMetricsRepository.getTotalUniqueFilesByProject(project);
        
        overview.put("totalCommits", totalCommits != null ? totalCommits : 0);
        overview.put("totalLinesAdded", totalLinesAdded != null ? totalLinesAdded : 0);
        overview.put("totalLinesModified", totalLinesModified != null ? totalLinesModified : 0);
        overview.put("totalLinesDeleted", totalLinesDeleted != null ? totalLinesDeleted : 0);
        overview.put("totalFilesModified", totalFiles != null ? totalFiles : 0);
        overview.put("activeContributors", activeContributors != null ? activeContributors : 0);
        
        double impactScore = calculateProjectImpactScore(project);
        overview.put("impactScore", Math.round(impactScore * 10.0) / 10.0);
        
        return overview;
    }

    public List<Map<String, Object>> getDailyActivity(Project project, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Object[]> dailyData = fileChangeMetricsRepository.getDailyFileChangesByProject(project, startDate);
        
        List<Map<String, Object>> activity = new ArrayList<>();
        for (Object[] row : dailyData) {
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", row[0].toString());
            dayData.put("commits", row[1]);
            activity.add(dayData);
        }
        
        return activity;
    }

    public Map<String, Object> getCommitSizeDistribution(Project project) {
        List<Commit> commits = commitRepository.findByProjectOrderByCommittedAtDesc(project);
        
        int small = 0, medium = 0, large = 0, veryLarge = 0;
        
        for (Commit commit : commits) {
            int totalChanges = (commit.getLinesAdded() != null ? commit.getLinesAdded() : 0) +
                             (commit.getLinesModified() != null ? commit.getLinesModified() : 0) +
                             (commit.getLinesDeleted() != null ? commit.getLinesDeleted() : 0);
            
            if (totalChanges <= 10) {
                small++;
            } else if (totalChanges <= 50) {
                medium++;
            } else if (totalChanges <= 200) {
                large++;
            } else {
                veryLarge++;
            }
        }
        
        Map<String, Object> distribution = new HashMap<>();
        distribution.put("small", small);
        distribution.put("medium", medium);
        distribution.put("large", large);
        distribution.put("veryLarge", veryLarge);
        
        return distribution;
    }

    public List<Map<String, Object>> getTopContributors(Project project, int limit) {
        List<ContributorStats> contributors = contributorStatsRepository.findByProjectOrderByTotalCommitsDesc(project);
        
        return contributors.stream()
                .limit(limit)
                .map(contributor -> {
                    Map<String, Object> contributorData = new HashMap<>();
                    contributorData.put("name", contributor.getContributorName());
                    contributorData.put("email", contributor.getContributorEmail());
                    contributorData.put("commits", contributor.getTotalCommits());
                    contributorData.put("linesAdded", contributor.getTotalLinesAdded());
                    contributorData.put("linesModified", contributor.getTotalLinesModified());
                    contributorData.put("linesDeleted", contributor.getTotalLinesDeleted());
                    contributorData.put("filesChanged", contributor.getTotalFilesChanged());
                    contributorData.put("impactScore", Math.round(contributor.getProductivityScore() * 10.0) / 10.0);
                    contributorData.put("codeQuality", Math.round(contributor.getCodeQualityScore() * 10.0) / 10.0);
                    contributorData.put("approvalRate", Math.round(contributor.getApprovalRate() * 10.0) / 10.0);
                    return contributorData;
                })
                .collect(Collectors.toList());
    }

    public Map<String, Object> getTimeDistribution(Project project) {
        List<Commit> commits = commitRepository.findByProjectOrderByCommittedAtDesc(project);
        
        int morning = 0, afternoon = 0, evening = 0, night = 0;
        
        for (Commit commit : commits) {
            if (commit.getCommittedAt() != null) {
                LocalTime time = commit.getCommittedAt().toLocalTime();
                int hour = time.getHour();
                
                if (hour >= 6 && hour < 12) {
                    morning++;
                } else if (hour >= 12 && hour < 18) {
                    afternoon++;
                } else if (hour >= 18 && hour < 24) {
                    evening++;
                } else {
                    night++;
                }
            }
        }
        
        Map<String, Object> timeDistribution = new HashMap<>();
        timeDistribution.put("morning", morning);
        timeDistribution.put("afternoon", afternoon);
        timeDistribution.put("evening", evening);
        timeDistribution.put("night", night);
        
        return timeDistribution;
    }

    public Map<String, Object> getFileTypeDistribution(Project project) {
        List<Object[]> fileTypeStats = fileChangeMetricsRepository.getFileTypeStatsByProject(project);
        
        Map<String, Object> distribution = new HashMap<>();
        
        for (Object[] row : fileTypeStats) {
            String extension = (String) row[0];
            Long count = (Long) row[1];
            
            if (extension != null && !extension.isEmpty()) {
                distribution.put(extension, count);
            }
        }
        
        return distribution;
    }

    public Map<String, Object> getCommitTrends(Project project, int months) {
        LocalDateTime startDate = LocalDateTime.now().minusMonths(months);
        List<Commit> recentCommits = commitRepository.findByProjectAndCommittedAtAfter(project, startDate);
        
        Map<String, Object> trends = new HashMap<>();
        
        Map<String, Integer> monthlyCommits = recentCommits.stream()
                .collect(Collectors.groupingBy(
                    commit -> commit.getCommittedAt().getYear() + "-" + 
                             String.format("%02d", commit.getCommittedAt().getMonthValue()),
                    Collectors.summingInt(commit -> 1)
                ));
        
        trends.put("monthlyCommits", monthlyCommits);
        trends.put("totalRecentCommits", recentCommits.size());
        trends.put("averageCommitsPerMonth", recentCommits.size() / Math.max(months, 1));
        
        return trends;
    }

    public Map<String, Object> getCodeQualityMetrics(Project project) {
        Double avgProductivity = contributorStatsRepository.getAverageProductivityScoreByProject(project);
        Double avgCodeQuality = contributorStatsRepository.getAverageCodeQualityScoreByProject(project);
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("averageProductivityScore", avgProductivity != null ? Math.round(avgProductivity * 10.0) / 10.0 : 0.0);
        metrics.put("averageCodeQualityScore", avgCodeQuality != null ? Math.round(avgCodeQuality * 10.0) / 10.0 : 0.0);
        
        List<ContributorStats> topPerformers = contributorStatsRepository.findByProjectOrderByProductivityScoreDesc(project)
                .stream().limit(3).collect(Collectors.toList());
        
        metrics.put("topPerformers", topPerformers.stream()
                .map(contributor -> Map.of(
                    "name", contributor.getContributorName(),
                    "score", Math.round(contributor.getProductivityScore() * 10.0) / 10.0
                ))
                .collect(Collectors.toList()));
        
        return metrics;
    }

    private double calculateProjectImpactScore(Project project) {
        Long totalCommits = contributorStatsRepository.getTotalCommitsByProject(project);
        Long activeContributors = contributorStatsRepository.getActiveContributorCountByProject(project);
        Double avgProductivity = contributorStatsRepository.getAverageProductivityScoreByProject(project);
        Double avgCodeQuality = contributorStatsRepository.getAverageCodeQualityScoreByProject(project);
        
        if (totalCommits == null || totalCommits == 0) return 0.0;
        
        double commitScore = Math.min(totalCommits * 0.1, 30.0);
        double teamScore = activeContributors != null ? Math.min(activeContributors * 5.0, 25.0) : 0.0;
        double productivityScore = avgProductivity != null ? avgProductivity * 0.25 : 0.0;
        double qualityScore = avgCodeQuality != null ? avgCodeQuality * 0.20 : 0.0;
        
        return Math.min(commitScore + teamScore + productivityScore + qualityScore, 10.0);
    }

    public void updateContributorStats(Commit commit) {
        Optional<ContributorStats> existingStats = contributorStatsRepository
                .findByProjectAndContributorEmail(commit.getProject(), commit.getAuthorEmail());
        
        ContributorStats stats;
        if (existingStats.isPresent()) {
            stats = existingStats.get();
        } else {
            stats = new ContributorStats(commit.getProject(), commit.getAuthorName(), commit.getAuthorEmail());
        }
        
        stats.setTotalCommits(stats.getTotalCommits() + 1);
        stats.setTotalLinesAdded(stats.getTotalLinesAdded() + (commit.getLinesAdded() != null ? commit.getLinesAdded() : 0));
        stats.setTotalLinesModified(stats.getTotalLinesModified() + (commit.getLinesModified() != null ? commit.getLinesModified() : 0));
        stats.setTotalLinesDeleted(stats.getTotalLinesDeleted() + (commit.getLinesDeleted() != null ? commit.getLinesDeleted() : 0));
        stats.setTotalFilesChanged(stats.getTotalFilesChanged() + (commit.getFilesChanged() != null ? commit.getFilesChanged() : 0));
        
        if (stats.getFirstCommitDate() == null || commit.getCommittedAt().isBefore(stats.getFirstCommitDate())) {
            stats.setFirstCommitDate(commit.getCommittedAt());
        }
        
        if (stats.getLastCommitDate() == null || commit.getCommittedAt().isAfter(stats.getLastCommitDate())) {
            stats.setLastCommitDate(commit.getCommittedAt());
        }
        
        contributorStatsRepository.save(stats);
    }
    
    /**
     * Calculate productivity trends and velocity metrics for a project.
     */
    public Map<String, Object> getProductivityTrends(Project project, int weeks) {
        Map<String, Object> trends = new HashMap<>();
        
        LocalDateTime startDate = LocalDateTime.now().minusWeeks(weeks);
        List<Commit> recentCommits = commitRepository.findByProjectAndCommittedAtAfter(project, startDate);
        
        // Group commits by week
        Map<Integer, List<Commit>> commitsByWeek = recentCommits.stream()
            .collect(Collectors.groupingBy(commit -> 
                commit.getCommittedAt().getDayOfYear() / 7));
        
        List<Map<String, Object>> weeklyData = new ArrayList<>();
        for (int week = 0; week < weeks; week++) {
            List<Commit> weekCommits = commitsByWeek.getOrDefault(week, new ArrayList<>());
            
            Map<String, Object> weekData = new HashMap<>();
            weekData.put("week", week);
            weekData.put("commits", weekCommits.size());
            weekData.put("linesAdded", weekCommits.stream()
                .mapToInt(c -> c.getLinesAdded() != null ? c.getLinesAdded() : 0).sum());
            weekData.put("linesDeleted", weekCommits.stream()
                .mapToInt(c -> c.getLinesDeleted() != null ? c.getLinesDeleted() : 0).sum());
            weekData.put("filesChanged", weekCommits.stream()
                .mapToInt(c -> c.getFilesChanged() != null ? c.getFilesChanged() : 0).sum());
            
            weeklyData.add(weekData);
        }
        
        trends.put("weeklyData", weeklyData);
        
        // Calculate velocity (average commits per week)
        double avgCommitsPerWeek = weeklyData.stream()
            .mapToInt(w -> (Integer) w.get("commits"))
            .average().orElse(0.0);
        trends.put("avgCommitsPerWeek", Math.round(avgCommitsPerWeek * 100.0) / 100.0);
        
        // Calculate trend direction (increasing/decreasing/stable)
        if (weeklyData.size() >= 2) {
            int firstHalfAvg = weeklyData.subList(0, weeklyData.size() / 2).stream()
                .mapToInt(w -> (Integer) w.get("commits")).sum();
            int secondHalfAvg = weeklyData.subList(weeklyData.size() / 2, weeklyData.size()).stream()
                .mapToInt(w -> (Integer) w.get("commits")).sum();
                
            if (secondHalfAvg > firstHalfAvg) {
                trends.put("trend", "increasing");
            } else if (secondHalfAvg < firstHalfAvg) {
                trends.put("trend", "decreasing");
            } else {
                trends.put("trend", "stable");
            }
        } else {
            trends.put("trend", "insufficient_data");
        }
        
        return trends;
    }
    
    /**
     * Calculate developer performance metrics including consistency and impact.
     */
    public Map<String, Object> getDeveloperPerformanceMetrics(Project project) {
        Map<String, Object> metrics = new HashMap<>();
        
        List<ContributorStats> contributors = contributorStatsRepository.findByProjectOrderByTotalCommitsDesc(project);
        
        List<Map<String, Object>> performanceData = new ArrayList<>();
        for (ContributorStats contributor : contributors) {
            Map<String, Object> devMetrics = new HashMap<>();
            devMetrics.put("name", contributor.getContributorName());
            devMetrics.put("email", contributor.getContributorEmail());
            devMetrics.put("commits", contributor.getTotalCommits());
            devMetrics.put("linesAdded", contributor.getTotalLinesAdded());
            devMetrics.put("linesDeleted", contributor.getTotalLinesDeleted());
            devMetrics.put("filesChanged", contributor.getTotalFilesChanged());
            
            // Calculate productivity score (weighted combination of metrics)
            double productivityScore = calculateProductivityScore(contributor);
            devMetrics.put("productivityScore", Math.round(productivityScore * 100.0) / 100.0);
            
            // Calculate consistency (commits spread over time)
            double consistencyScore = calculateConsistencyScore(contributor, project);
            devMetrics.put("consistencyScore", Math.round(consistencyScore * 100.0) / 100.0);
            
            // Calculate impact score (lines per commit ratio)
            double impactScore = calculateImpactScore(contributor);
            devMetrics.put("impactScore", Math.round(impactScore * 100.0) / 100.0);
            
            performanceData.add(devMetrics);
        }
        
        // Sort by productivity score
        performanceData.sort((a, b) -> 
            Double.compare((Double) b.get("productivityScore"), (Double) a.get("productivityScore")));
        
        metrics.put("developers", performanceData);
        metrics.put("totalDevelopers", contributors.size());
        
        // Calculate team averages
        if (!contributors.isEmpty()) {
            double avgProductivity = performanceData.stream()
                .mapToDouble(d -> (Double) d.get("productivityScore"))
                .average().orElse(0.0);
            double avgConsistency = performanceData.stream()
                .mapToDouble(d -> (Double) d.get("consistencyScore"))
                .average().orElse(0.0);
            double avgImpact = performanceData.stream()
                .mapToDouble(d -> (Double) d.get("impactScore"))
                .average().orElse(0.0);
                
            metrics.put("teamAvgProductivity", Math.round(avgProductivity * 100.0) / 100.0);
            metrics.put("teamAvgConsistency", Math.round(avgConsistency * 100.0) / 100.0);
            metrics.put("teamAvgImpact", Math.round(avgImpact * 100.0) / 100.0);
        }
        
        return metrics;
    }
    
    /**
     * Calculate team collaboration insights and communication patterns.
     */
    public Map<String, Object> getTeamCollaborationInsights(Project project) {
        Map<String, Object> insights = new HashMap<>();
        
        List<ContributorStats> contributors = contributorStatsRepository.findByProjectOrderByTotalCommitsDesc(project);
        List<Commit> commits = commitRepository.findByProject(project);
        
        // Calculate collaboration score based on file overlap
        Map<String, Set<String>> filesByDeveloper = new HashMap<>();
        
        // Get file changes for each developer
        for (Commit commit : commits) {
            String author = commit.getAuthorEmail();
            filesByDeveloper.putIfAbsent(author, new HashSet<>());
            
            // Add files from this commit (simplified - you might want to get actual file paths)
            List<FileChangeMetrics> fileChanges = fileChangeMetricsRepository.findByCommitId(commit.getId());
            Set<String> files = fileChanges.stream()
                .map(fcm -> fcm.getFilePath())
                .collect(Collectors.toSet());
            filesByDeveloper.get(author).addAll(files);
        }
        
        // Calculate collaboration metrics
        int totalPairs = 0;
        int collaboratingPairs = 0;
        
        List<String> developers = new ArrayList<>(filesByDeveloper.keySet());
        for (int i = 0; i < developers.size(); i++) {
            for (int j = i + 1; j < developers.size(); j++) {
                totalPairs++;
                
                Set<String> dev1Files = filesByDeveloper.get(developers.get(i));
                Set<String> dev2Files = filesByDeveloper.get(developers.get(j));
                
                // Check if they work on common files
                Set<String> commonFiles = new HashSet<>(dev1Files);
                commonFiles.retainAll(dev2Files);
                
                if (!commonFiles.isEmpty()) {
                    collaboratingPairs++;
                }
            }
        }
        
        double collaborationScore = totalPairs > 0 ? 
            (double) collaboratingPairs / totalPairs * 100.0 : 0.0;
        
        insights.put("collaborationScore", Math.round(collaborationScore * 100.0) / 100.0);
        insights.put("collaboratingPairs", collaboratingPairs);
        insights.put("totalPairs", totalPairs);
        insights.put("activeContributors", contributors.size());
        
        // Calculate knowledge distribution (how much of the codebase each developer knows)
        Set<String> allFiles = filesByDeveloper.values().stream()
            .flatMap(Set::stream)
            .collect(Collectors.toSet());
            
        List<Map<String, Object>> knowledgeDistribution = new ArrayList<>();
        for (ContributorStats contributor : contributors) {
            Set<String> devFiles = filesByDeveloper.getOrDefault(contributor.getContributorEmail(), new HashSet<>());
            double knowledgePercentage = allFiles.isEmpty() ? 0.0 : 
                (double) devFiles.size() / allFiles.size() * 100.0;
                
            Map<String, Object> knowledge = new HashMap<>();
            knowledge.put("developer", contributor.getContributorName());
            knowledge.put("filesKnown", devFiles.size());
            knowledge.put("knowledgePercentage", Math.round(knowledgePercentage * 100.0) / 100.0);
            knowledgeDistribution.add(knowledge);
        }
        
        // Sort by knowledge percentage
        knowledgeDistribution.sort((a, b) -> 
            Double.compare((Double) b.get("knowledgePercentage"), (Double) a.get("knowledgePercentage")));
        
        insights.put("knowledgeDistribution", knowledgeDistribution);
        
        return insights;
    }
    
    // Helper methods for scoring algorithms
    
    private double calculateProductivityScore(ContributorStats contributor) {
        // Weighted score based on commits, lines added, and files changed
        double commitsWeight = 0.4;
        double linesWeight = 0.4;
        double filesWeight = 0.2;
        
        // Normalize values (you might want to adjust these based on project standards)
        double normalizedCommits = Math.min(contributor.getTotalCommits() / 50.0, 1.0);
        double normalizedLines = Math.min(contributor.getTotalLinesAdded() / 5000.0, 1.0);
        double normalizedFiles = Math.min(contributor.getTotalFilesChanged() / 200.0, 1.0);
        
        return (normalizedCommits * commitsWeight + 
                normalizedLines * linesWeight + 
                normalizedFiles * filesWeight) * 100.0;
    }
    
    private double calculateConsistencyScore(ContributorStats contributor, Project project) {
        // Calculate based on commit frequency and distribution over time
        List<Commit> userCommits = commitRepository.findByProjectAndAuthorEmail(project, contributor.getContributorEmail());
        
        if (userCommits.size() < 2) {
            return 0.0;
        }
        
        // Calculate days between first and last commit
        LocalDateTime firstCommit = contributor.getFirstCommitDate();
        LocalDateTime lastCommit = contributor.getLastCommitDate();
        
        if (firstCommit == null || lastCommit == null) {
            return 0.0;
        }
        
        long daysBetween = java.time.Duration.between(firstCommit, lastCommit).toDays();
        if (daysBetween == 0) {
            return 100.0; // All commits in one day = perfect consistency for that day
        }
        
        // Ideal would be evenly distributed commits
        double idealCommitFrequency = (double) contributor.getTotalCommits() / daysBetween;
        
        // Calculate actual distribution variance (simplified)
        double consistencyScore = Math.min(idealCommitFrequency * 20.0, 100.0);
        
        return consistencyScore;
    }
    
    private double calculateImpactScore(ContributorStats contributor) {
        // Calculate based on lines of code per commit and files affected
        if (contributor.getTotalCommits() == 0) {
            return 0.0;
        }
        
        double avgLinesPerCommit = (double) contributor.getTotalLinesAdded() / contributor.getTotalCommits();
        double avgFilesPerCommit = (double) contributor.getTotalFilesChanged() / contributor.getTotalCommits();
        
        // Balance between code volume and file spread
        double linesScore = Math.min(avgLinesPerCommit / 100.0, 1.0) * 70.0; // Max 70 points for lines
        double filesScore = Math.min(avgFilesPerCommit / 10.0, 1.0) * 30.0; // Max 30 points for files
        
        return linesScore + filesScore;
    }
}
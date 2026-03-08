package com.autotrack.service;

import com.autotrack.model.Commit;
import com.autotrack.model.Project;
import com.autotrack.model.User;
import com.autotrack.repository.CommitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for commit statistics and analytics
 */
@Service
public class CommitStatisticsService {
    
    private final CommitRepository commitRepository;
    
    @Autowired
    public CommitStatisticsService(CommitRepository commitRepository) {
        this.commitRepository = commitRepository;
    }
    
    /**
     * Data class for commit statistics summary
     */
    public static class CommitStatsSummary {
        private final int totalCommits;
        private final int totalLinesAdded;
        private final int totalLinesModified;
        private final int totalLinesDeleted;
        private final int totalFilesChanged;
        private final double averageLinesPerCommit;
        private final String mostActiveDay;
        private final int impactScore;
        
        public CommitStatsSummary(int totalCommits, int totalLinesAdded, int totalLinesModified, 
                                int totalLinesDeleted, int totalFilesChanged, double averageLinesPerCommit, 
                                String mostActiveDay, int impactScore) {
            this.totalCommits = totalCommits;
            this.totalLinesAdded = totalLinesAdded;
            this.totalLinesModified = totalLinesModified;
            this.totalLinesDeleted = totalLinesDeleted;
            this.totalFilesChanged = totalFilesChanged;
            this.averageLinesPerCommit = averageLinesPerCommit;
            this.mostActiveDay = mostActiveDay;
            this.impactScore = impactScore;
        }
        
        // Getters
        public int getTotalCommits() { return totalCommits; }
        public int getTotalLinesAdded() { return totalLinesAdded; }
        public int getTotalLinesModified() { return totalLinesModified; }
        public int getTotalLinesDeleted() { return totalLinesDeleted; }
        public int getTotalFilesChanged() { return totalFilesChanged; }
        public double getAverageLinesPerCommit() { return averageLinesPerCommit; }
        public String getMostActiveDay() { return mostActiveDay; }
        public int getImpactScore() { return impactScore; }
        public int getTotalLinesChanged() { return totalLinesAdded + totalLinesModified + totalLinesDeleted; }
    }
    
    /**
     * Data class for daily commit activity
     */
    public static class DailyCommitActivity {
        private final LocalDateTime date;
        private final int commits;
        private final int linesAdded;
        private final int linesModified;
        private final int linesDeleted;
        
        public DailyCommitActivity(LocalDateTime date, int commits, int linesAdded, 
                                 int linesModified, int linesDeleted) {
            this.date = date;
            this.commits = commits;
            this.linesAdded = linesAdded;
            this.linesModified = linesModified;
            this.linesDeleted = linesDeleted;
        }
        
        // Getters
        public LocalDateTime getDate() { return date; }
        public int getCommits() { return commits; }
        public int getLinesAdded() { return linesAdded; }
        public int getLinesModified() { return linesModified; }
        public int getLinesDeleted() { return linesDeleted; }
        public int getTotalLines() { return linesAdded + linesModified + linesDeleted; }
    }
    
    /**
     * Get commit statistics for a specific project
     */
    public CommitStatsSummary getProjectCommitStats(Project project) {
        List<Commit> commits = commitRepository.findByProject(project);
        return calculateStatsSummary(commits);
    }
    
    /**
     * Get commit statistics for a specific user in a project
     */
    public CommitStatsSummary getUserCommitStats(Project project, String authorName) {
        List<Commit> commits = commitRepository.findByProjectAndAuthorName(project, authorName);
        return calculateStatsSummary(commits);
    }
    
    /**
     * Get commit statistics for a specific user across all projects
     */
    public CommitStatsSummary getUserCommitStatsGlobal(String authorName) {
        List<Commit> commits = commitRepository.findByAuthorName(authorName);
        return calculateStatsSummary(commits);
    }
    
    /**
     * Get daily commit activity for a project over the last 30 days
     */
    public List<DailyCommitActivity> getDailyCommitActivity(Project project, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Commit> commits = commitRepository.findByProjectAndCommittedAtAfter(project, startDate);
        
        Map<LocalDateTime, List<Commit>> commitsByDay = commits.stream()
                .collect(Collectors.groupingBy(commit -> 
                    commit.getCommittedAt().toLocalDate().atStartOfDay()));
        
        return commitsByDay.entrySet().stream()
                .map(entry -> {
                    List<Commit> dayCommits = entry.getValue();
                    int totalAdded = dayCommits.stream().mapToInt(Commit::getLinesAdded).sum();
                    int totalModified = dayCommits.stream().mapToInt(Commit::getLinesModified).sum();
                    int totalDeleted = dayCommits.stream().mapToInt(Commit::getLinesDeleted).sum();
                    
                    return new DailyCommitActivity(entry.getKey(), dayCommits.size(), 
                                                 totalAdded, totalModified, totalDeleted);
                })
                .sorted(Comparator.comparing(DailyCommitActivity::getDate))
                .collect(Collectors.toList());
    }
    
    /**
     * Get top contributors for a project based on lines of code
     */
    public List<Map<String, Object>> getTopContributors(Project project, int limit) {
        List<Commit> commits = commitRepository.findByProject(project);
        
        Map<String, CommitStatsSummary> contributorStats = commits.stream()
                .collect(Collectors.groupingBy(Commit::getAuthorName))
                .entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> calculateStatsSummary(entry.getValue())
                ));
        
        return contributorStats.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().getImpactScore(), e1.getValue().getImpactScore()))
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> contributor = new HashMap<>();
                    contributor.put("authorName", entry.getKey());
                    contributor.put("stats", entry.getValue());
                    return contributor;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Get commit size distribution for a project
     */
    public Map<String, Integer> getCommitSizeDistribution(Project project) {
        List<Commit> commits = commitRepository.findByProject(project);
        
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("Small (1-10 lines)", 0);
        distribution.put("Medium (11-50 lines)", 0);
        distribution.put("Large (51-200 lines)", 0);
        distribution.put("Huge (200+ lines)", 0);
        
        for (Commit commit : commits) {
            int totalLines = commit.getLinesAdded() + commit.getLinesModified() + commit.getLinesDeleted();
            
            if (totalLines <= 10) {
                distribution.put("Small (1-10 lines)", distribution.get("Small (1-10 lines)") + 1);
            } else if (totalLines <= 50) {
                distribution.put("Medium (11-50 lines)", distribution.get("Medium (11-50 lines)") + 1);
            } else if (totalLines <= 200) {
                distribution.put("Large (51-200 lines)", distribution.get("Large (51-200 lines)") + 1);
            } else {
                distribution.put("Huge (200+ lines)", distribution.get("Huge (200+ lines)") + 1);
            }
        }
        
        return distribution;
    }
    
    /**
     * Calculate summary statistics for a list of commits
     */
    private CommitStatsSummary calculateStatsSummary(List<Commit> commits) {
        if (commits.isEmpty()) {
            return new CommitStatsSummary(0, 0, 0, 0, 0, 0.0, "No activity", 0);
        }
        
        int totalCommits = commits.size();
        int totalLinesAdded = commits.stream().mapToInt(Commit::getLinesAdded).sum();
        int totalLinesModified = commits.stream().mapToInt(Commit::getLinesModified).sum();
        int totalLinesDeleted = commits.stream().mapToInt(Commit::getLinesDeleted).sum();
        int totalFilesChanged = commits.stream().mapToInt(Commit::getFilesChanged).sum();
        
        double averageLinesPerCommit = totalCommits > 0 ? 
            (double)(totalLinesAdded + totalLinesModified + totalLinesDeleted) / totalCommits : 0.0;
        
        // Calculate impact score: additions (1x) + modifications (2x) + deletions (0.5x)
        int impactScore = (int)(totalLinesAdded * 1.0 + totalLinesModified * 2.0 + totalLinesDeleted * 0.5);
        
        // Find most active day of week
        Map<String, Integer> dayActivity = commits.stream()
                .collect(Collectors.groupingBy(
                    commit -> commit.getCommittedAt().getDayOfWeek().toString(),
                    Collectors.summingInt(c -> 1)
                ));
        
        String mostActiveDay = dayActivity.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");
        
        return new CommitStatsSummary(totalCommits, totalLinesAdded, totalLinesModified, 
                                    totalLinesDeleted, totalFilesChanged, averageLinesPerCommit, 
                                    mostActiveDay, impactScore);
    }
}

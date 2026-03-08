package com.autotrack.service;

import com.autotrack.model.Commit;
import com.autotrack.model.ContributorStats;
import com.autotrack.model.FileChangeMetrics;
import com.autotrack.model.Project;
import com.autotrack.repository.CommitRepository;
import com.autotrack.repository.ContributorStatsRepository;
import com.autotrack.repository.FileChangeMetricsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI-powered code contribution analysis service.
 * Analyzes commits to break down contributions into code elements
 * (functions, variables, comments, imports, etc.) and provides
 * detailed commit size rankings per contributor.
 */
@Service
@Transactional(readOnly = true)
public class CodeContributionAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(CodeContributionAnalysisService.class);

    private final CommitRepository commitRepository;
    private final ContributorStatsRepository contributorStatsRepository;
    private final FileChangeMetricsRepository fileChangeMetricsRepository;

    // Weights for contribution score calculation
    private static final double WEIGHT_LINES_ADDED = 1.0;
    private static final double WEIGHT_LINES_MODIFIED = 1.5;
    private static final double WEIGHT_LINES_DELETED = 0.3;
    private static final double WEIGHT_FILES_CHANGED = 2.0;
    private static final double WEIGHT_COMMITS = 3.0;

    // File extension → language mapping for code element estimation
    private static final Map<String, String> LANGUAGE_MAP = Map.ofEntries(
        Map.entry("java", "Java"), Map.entry("kt", "Kotlin"),
        Map.entry("py", "Python"), Map.entry("js", "JavaScript"),
        Map.entry("ts", "TypeScript"), Map.entry("jsx", "JavaScript"),
        Map.entry("tsx", "TypeScript"), Map.entry("go", "Go"),
        Map.entry("rs", "Rust"), Map.entry("cpp", "C++"),
        Map.entry("c", "C"), Map.entry("cs", "C#"),
        Map.entry("rb", "Ruby"), Map.entry("php", "PHP"),
        Map.entry("swift", "Swift"), Map.entry("scala", "Scala"),
        Map.entry("html", "HTML"), Map.entry("css", "CSS"),
        Map.entry("scss", "SCSS"), Map.entry("xml", "XML"),
        Map.entry("json", "JSON"), Map.entry("yml", "YAML"),
        Map.entry("yaml", "YAML"), Map.entry("sql", "SQL"),
        Map.entry("sh", "Shell"), Map.entry("bat", "Batch"),
        Map.entry("md", "Markdown"), Map.entry("properties", "Properties")
    );

    // Average code element ratios per language (estimated from industry data)
    // Format: {functions%, variables%, comments%, imports%, classes%, other%}
    private static final Map<String, double[]> CODE_ELEMENT_RATIOS = Map.of(
        "Java",       new double[]{0.25, 0.20, 0.15, 0.10, 0.10, 0.20},
        "Python",     new double[]{0.30, 0.22, 0.12, 0.10, 0.08, 0.18},
        "JavaScript", new double[]{0.28, 0.24, 0.10, 0.08, 0.06, 0.24},
        "TypeScript", new double[]{0.26, 0.22, 0.12, 0.10, 0.08, 0.22},
        "HTML",       new double[]{0.0,  0.05, 0.05, 0.0,  0.0,  0.90},
        "CSS",        new double[]{0.0,  0.30, 0.05, 0.05, 0.0,  0.60},
        "SQL",        new double[]{0.15, 0.10, 0.10, 0.0,  0.0,  0.65},
        "default",    new double[]{0.20, 0.20, 0.12, 0.08, 0.05, 0.35}
    );

    public CodeContributionAnalysisService(CommitRepository commitRepository,
                                           ContributorStatsRepository contributorStatsRepository,
                                           FileChangeMetricsRepository fileChangeMetricsRepository) {
        this.commitRepository = commitRepository;
        this.contributorStatsRepository = contributorStatsRepository;
        this.fileChangeMetricsRepository = fileChangeMetricsRepository;
    }

    /**
     * Get full contribution analysis for all contributors in a project.
     * Returns percentage breakdown (clickable to see details).
     */
    public Map<String, Object> getContributionAnalysis(Project project) {
        Map<String, Object> result = new LinkedHashMap<>();

        List<ContributorStats> allStats = contributorStatsRepository.findByProjectOrderByTotalCommitsDesc(project);
        if (allStats.isEmpty()) {
            result.put("contributors", Collections.emptyList());
            result.put("totalCommits", 0);
            return result;
        }

        // Calculate total contribution score
        double totalScore = allStats.stream()
                .mapToDouble(this::calculateContributionScore)
                .sum();

        List<Map<String, Object>> contributors = new ArrayList<>();
        for (ContributorStats stats : allStats) {
            Map<String, Object> contributor = new LinkedHashMap<>();
            double score = calculateContributionScore(stats);
            double percentage = totalScore > 0 ? (score / totalScore) * 100.0 : 0.0;

            contributor.put("name", stats.getContributorName());
            contributor.put("email", stats.getContributorEmail());
            contributor.put("contributionPercentage", Math.round(percentage * 10.0) / 10.0);
            contributor.put("totalCommits", stats.getTotalCommits());
            contributor.put("linesAdded", stats.getTotalLinesAdded());
            contributor.put("linesModified", stats.getTotalLinesModified());
            contributor.put("linesDeleted", stats.getTotalLinesDeleted());
            contributor.put("filesChanged", stats.getTotalFilesChanged());
            contributor.put("productivityScore", Math.round(stats.getProductivityScore() * 10.0) / 10.0);
            contributor.put("codeQualityScore", Math.round(stats.getCodeQualityScore() * 10.0) / 10.0);
            contributor.put("avgCommitSize", Math.round(stats.getAvgCommitSize() * 10.0) / 10.0);
            contributors.add(contributor);
        }

        long totalCommits = allStats.stream().mapToLong(ContributorStats::getTotalCommits).sum();
        result.put("contributors", contributors);
        result.put("totalCommits", totalCommits);
        result.put("totalContributors", allStats.size());
        return result;
    }

    /**
     * Get detailed code element breakdown for a specific contributor.
     * Analyzes their commits to estimate functions, variables, comments, etc.
     */
    public Map<String, Object> getContributorDetail(Project project, String contributorEmail) {
        Map<String, Object> detail = new LinkedHashMap<>();

        Optional<ContributorStats> statsOpt = contributorStatsRepository
                .findByProjectAndContributorEmail(project, contributorEmail);
        if (statsOpt.isEmpty()) {
            detail.put("error", "Contributor not found");
            return detail;
        }

        ContributorStats stats = statsOpt.get();
        detail.put("name", stats.getContributorName());
        detail.put("email", stats.getContributorEmail());
        detail.put("totalCommits", stats.getTotalCommits());
        detail.put("linesAdded", stats.getTotalLinesAdded());
        detail.put("linesModified", stats.getTotalLinesModified());
        detail.put("linesDeleted", stats.getTotalLinesDeleted());
        detail.put("filesChanged", stats.getTotalFilesChanged());
        detail.put("avgCommitSize", Math.round(stats.getAvgCommitSize() * 10.0) / 10.0);
        detail.put("productivityScore", Math.round(stats.getProductivityScore() * 10.0) / 10.0);
        detail.put("codeQualityScore", Math.round(stats.getCodeQualityScore() * 10.0) / 10.0);
        detail.put("firstCommitDate", stats.getFirstCommitDate() != null ? stats.getFirstCommitDate().toString() : null);
        detail.put("lastCommitDate", stats.getLastCommitDate() != null ? stats.getLastCommitDate().toString() : null);

        // Analyze code elements from file change metrics
        List<Commit> userCommits = commitRepository.findByProjectAndAuthorEmail(project, contributorEmail);
        Map<String, Object> codeElements = analyzeCodeElements(project, userCommits);
        detail.put("codeElements", codeElements);

        // Language breakdown
        Map<String, Object> languageBreakdown = analyzeLanguageBreakdown(project, userCommits);
        detail.put("languageBreakdown", languageBreakdown);

        // Commit size list (all commits sorted by size)
        List<Map<String, Object>> commitSizes = getCommitSizeList(userCommits);
        detail.put("commits", commitSizes);

        // Commit time patterns
        Map<String, Integer> timePatterns = analyzeCommitTimePatterns(userCommits);
        detail.put("timePatterns", timePatterns);

        return detail;
    }

    /**
     * Get commit size rankings for all contributors in a project.
     * Shows biggest and smallest commits per user.
     */
    public Map<String, Object> getCommitSizeRankings(Project project) {
        Map<String, Object> rankings = new LinkedHashMap<>();

        List<ContributorStats> allStats = contributorStatsRepository.findByProjectOrderByTotalCommitsDesc(project);
        List<Map<String, Object>> userRankings = new ArrayList<>();

        for (ContributorStats stats : allStats) {
            List<Commit> userCommits = commitRepository.findByProjectAndAuthorEmail(project, stats.getContributorEmail());
            if (userCommits.isEmpty()) continue;

            Map<String, Object> userRanking = new LinkedHashMap<>();
            userRanking.put("name", stats.getContributorName());
            userRanking.put("email", stats.getContributorEmail());
            userRanking.put("totalCommits", stats.getTotalCommits());
            userRanking.put("avgCommitSize", Math.round(stats.getAvgCommitSize() * 10.0) / 10.0);

            // Sort commits by total changes
            userCommits.sort((a, b) -> Integer.compare(getCommitSize(b), getCommitSize(a)));

            // Biggest commit
            Commit biggest = userCommits.get(0);
            userRanking.put("biggestCommit", buildCommitSummary(biggest));

            // Smallest commit
            Commit smallest = userCommits.get(userCommits.size() - 1);
            userRanking.put("smallestCommit", buildCommitSummary(smallest));

            // Commit size distribution for this user
            Map<String, Integer> sizeDistribution = new LinkedHashMap<>();
            int small = 0, medium = 0, large = 0, huge = 0;
            for (Commit c : userCommits) {
                int size = getCommitSize(c);
                if (size <= 10) small++;
                else if (size <= 50) medium++;
                else if (size <= 200) large++;
                else huge++;
            }
            sizeDistribution.put("small", small);
            sizeDistribution.put("medium", medium);
            sizeDistribution.put("large", large);
            sizeDistribution.put("huge", huge);
            userRanking.put("sizeDistribution", sizeDistribution);

            userRankings.add(userRanking);
        }

        // Sort by average commit size descending
        userRankings.sort((a, b) -> Double.compare(
                (Double) b.get("avgCommitSize"), (Double) a.get("avgCommitSize")));

        rankings.put("rankings", userRankings);

        // Project-wide biggest and smallest
        List<Commit> allCommits = commitRepository.findByProjectOrderByCommittedAtDesc(project);
        if (!allCommits.isEmpty()) {
            allCommits.sort((a, b) -> Integer.compare(getCommitSize(b), getCommitSize(a)));
            rankings.put("projectBiggestCommit", buildCommitSummary(allCommits.get(0)));
            rankings.put("projectSmallestCommit", buildCommitSummary(allCommits.get(allCommits.size() - 1)));
        }

        return rankings;
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private double calculateContributionScore(ContributorStats stats) {
        return (safeInt(stats.getTotalLinesAdded()) * WEIGHT_LINES_ADDED)
             + (safeInt(stats.getTotalLinesModified()) * WEIGHT_LINES_MODIFIED)
             + (safeInt(stats.getTotalLinesDeleted()) * WEIGHT_LINES_DELETED)
             + (safeInt(stats.getTotalFilesChanged()) * WEIGHT_FILES_CHANGED)
             + (safeInt(stats.getTotalCommits()) * WEIGHT_COMMITS);
    }

    /**
     * Analyze code elements (functions, variables, comments, imports, classes)
     * by looking at file types modified by this contributor.
     */
    private Map<String, Object> analyzeCodeElements(Project project, List<Commit> commits) {
        Map<String, Object> elements = new LinkedHashMap<>();
        int totalLinesChanged = 0;
        Map<String, Integer> linesByLanguage = new HashMap<>();

        for (Commit commit : commits) {
            List<FileChangeMetrics> fileChanges = fileChangeMetricsRepository.findByCommitId(commit.getId());
            for (FileChangeMetrics fcm : fileChanges) {
                int lines = safeInt(fcm.getLinesAdded()) + safeInt(fcm.getLinesModified());
                String lang = LANGUAGE_MAP.getOrDefault(
                        fcm.getFileExtension() != null ? fcm.getFileExtension().toLowerCase() : "", "Other");
                linesByLanguage.merge(lang, lines, Integer::sum);
                totalLinesChanged += lines;
            }
        }

        // Estimate code elements based on language ratios
        int estFunctions = 0, estVariables = 0, estComments = 0;
        int estImports = 0, estClasses = 0, estOther = 0;

        for (Map.Entry<String, Integer> entry : linesByLanguage.entrySet()) {
            double[] ratios = CODE_ELEMENT_RATIOS.getOrDefault(entry.getKey(),
                    CODE_ELEMENT_RATIOS.get("default"));
            int lines = entry.getValue();
            estFunctions  += (int)(lines * ratios[0]);
            estVariables  += (int)(lines * ratios[1]);
            estComments   += (int)(lines * ratios[2]);
            estImports    += (int)(lines * ratios[3]);
            estClasses    += (int)(lines * ratios[4]);
            estOther      += (int)(lines * ratios[5]);
        }

        elements.put("functions", estFunctions);
        elements.put("variables", estVariables);
        elements.put("comments", estComments);
        elements.put("imports", estImports);
        elements.put("classes", estClasses);
        elements.put("other", estOther);
        elements.put("totalLinesAnalyzed", totalLinesChanged);

        // Also compute percentages
        if (totalLinesChanged > 0) {
            Map<String, Double> percentages = new LinkedHashMap<>();
            percentages.put("functions", round1(estFunctions * 100.0 / totalLinesChanged));
            percentages.put("variables", round1(estVariables * 100.0 / totalLinesChanged));
            percentages.put("comments", round1(estComments * 100.0 / totalLinesChanged));
            percentages.put("imports", round1(estImports * 100.0 / totalLinesChanged));
            percentages.put("classes", round1(estClasses * 100.0 / totalLinesChanged));
            percentages.put("other", round1(estOther * 100.0 / totalLinesChanged));
            elements.put("percentages", percentages);
        }

        return elements;
    }

    private Map<String, Object> analyzeLanguageBreakdown(Project project, List<Commit> commits) {
        Map<String, Integer> linesByLanguage = new HashMap<>();
        int totalLines = 0;

        for (Commit commit : commits) {
            List<FileChangeMetrics> fileChanges = fileChangeMetricsRepository.findByCommitId(commit.getId());
            for (FileChangeMetrics fcm : fileChanges) {
                int lines = safeInt(fcm.getLinesAdded()) + safeInt(fcm.getLinesModified()) + safeInt(fcm.getLinesDeleted());
                String lang = LANGUAGE_MAP.getOrDefault(
                        fcm.getFileExtension() != null ? fcm.getFileExtension().toLowerCase() : "", "Other");
                linesByLanguage.merge(lang, lines, Integer::sum);
                totalLines += lines;
            }
        }

        Map<String, Object> breakdown = new LinkedHashMap<>();
        final int total = totalLines;
        // Sort by lines descending
        linesByLanguage.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> {
                    Map<String, Object> langData = new LinkedHashMap<>();
                    langData.put("lines", e.getValue());
                    langData.put("percentage", total > 0 ? round1(e.getValue() * 100.0 / total) : 0.0);
                    breakdown.put(e.getKey(), langData);
                });

        return breakdown;
    }

    private List<Map<String, Object>> getCommitSizeList(List<Commit> commits) {
        return commits.stream()
                .sorted((a, b) -> Integer.compare(getCommitSize(b), getCommitSize(a)))
                .map(this::buildCommitSummary)
                .collect(Collectors.toList());
    }

    private Map<String, Integer> analyzeCommitTimePatterns(List<Commit> commits) {
        Map<String, Integer> patterns = new LinkedHashMap<>();
        int morning = 0, afternoon = 0, evening = 0, night = 0;
        for (Commit c : commits) {
            if (c.getCommittedAt() != null) {
                int hour = c.getCommittedAt().getHour();
                if (hour >= 6 && hour < 12) morning++;
                else if (hour >= 12 && hour < 18) afternoon++;
                else if (hour >= 18 && hour < 24) evening++;
                else night++;
            }
        }
        patterns.put("morning", morning);
        patterns.put("afternoon", afternoon);
        patterns.put("evening", evening);
        patterns.put("night", night);
        return patterns;
    }

    private Map<String, Object> buildCommitSummary(Commit commit) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("sha", commit.getSha());
        summary.put("message", commit.getMessage());
        summary.put("author", commit.getAuthorName());
        summary.put("date", commit.getCommittedAt() != null ? commit.getCommittedAt().toString() : null);
        summary.put("linesAdded", safeInt(commit.getLinesAdded()));
        summary.put("linesModified", safeInt(commit.getLinesModified()));
        summary.put("linesDeleted", safeInt(commit.getLinesDeleted()));
        summary.put("filesChanged", safeInt(commit.getFilesChanged()));
        summary.put("totalSize", getCommitSize(commit));
        summary.put("sizeCategory", categorizeSize(getCommitSize(commit)));
        summary.put("url", commit.getGitHubUrl());
        return summary;
    }

    private int getCommitSize(Commit c) {
        return safeInt(c.getLinesAdded()) + safeInt(c.getLinesModified()) + safeInt(c.getLinesDeleted());
    }

    private String categorizeSize(int size) {
        if (size <= 10) return "Small";
        if (size <= 50) return "Medium";
        if (size <= 200) return "Large";
        return "Huge";
    }

    private int safeInt(Integer val) {
        return val != null ? val : 0;
    }

    private double round1(double val) {
        return Math.round(val * 10.0) / 10.0;
    }
}

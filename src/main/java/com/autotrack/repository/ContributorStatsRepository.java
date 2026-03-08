package com.autotrack.repository;

import com.autotrack.model.ContributorStats;
import com.autotrack.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContributorStatsRepository extends JpaRepository<ContributorStats, Long> {

    Optional<ContributorStats> findByProjectAndContributorEmail(Project project, String contributorEmail);

    List<ContributorStats> findByProjectOrderByTotalCommitsDesc(Project project);

    List<ContributorStats> findByProjectOrderByProductivityScoreDesc(Project project);

    List<ContributorStats> findByProjectOrderByCodeQualityScoreDesc(Project project);

    @Query("SELECT cs FROM ContributorStats cs WHERE cs.project = :project ORDER BY cs.totalLinesAdded + cs.totalLinesModified + cs.totalLinesDeleted DESC")
    List<ContributorStats> findByProjectOrderByTotalLinesChanged(@Param("project") Project project);

    @Query("SELECT cs FROM ContributorStats cs WHERE cs.project = :project AND cs.totalCommits > 0 ORDER BY (cs.approvedCommits * 1.0 / cs.totalCommits) DESC")
    List<ContributorStats> findByProjectOrderByApprovalRate(@Param("project") Project project);

    @Query("SELECT SUM(cs.totalCommits) FROM ContributorStats cs WHERE cs.project = :project")
    Long getTotalCommitsByProject(@Param("project") Project project);

    @Query("SELECT SUM(cs.totalLinesAdded) FROM ContributorStats cs WHERE cs.project = :project")
    Long getTotalLinesAddedByProject(@Param("project") Project project);

    @Query("SELECT SUM(cs.totalLinesModified) FROM ContributorStats cs WHERE cs.project = :project")
    Long getTotalLinesModifiedByProject(@Param("project") Project project);

    @Query("SELECT SUM(cs.totalLinesDeleted) FROM ContributorStats cs WHERE cs.project = :project")
    Long getTotalLinesDeletedByProject(@Param("project") Project project);

    @Query("SELECT COUNT(DISTINCT cs.contributorEmail) FROM ContributorStats cs WHERE cs.project = :project AND cs.totalCommits > 0")
    Long getActiveContributorCountByProject(@Param("project") Project project);

    @Query("SELECT AVG(cs.productivityScore) FROM ContributorStats cs WHERE cs.project = :project AND cs.totalCommits > 0")
    Double getAverageProductivityScoreByProject(@Param("project") Project project);

    @Query("SELECT AVG(cs.codeQualityScore) FROM ContributorStats cs WHERE cs.project = :project AND cs.totalCommits > 0")
    Double getAverageCodeQualityScoreByProject(@Param("project") Project project);
}
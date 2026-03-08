package com.autotrack.repository;

import com.autotrack.model.Commit;
import com.autotrack.model.Project;
import com.autotrack.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Commit entity.
 */
@Repository
public interface CommitRepository extends JpaRepository<Commit, Long> {
    
    List<Commit> findByTaskOrderByCommittedAtDesc(Task task);
    
    List<Commit> findByProjectOrderByCommittedAtDesc(Project project);
    
    Optional<Commit> findByShaAndProject(String sha, Project project);
    
    // New methods for commit statistics
    List<Commit> findByProject(Project project);
    
    List<Commit> findByProjectAndAuthorName(Project project, String authorName);
    
    List<Commit> findByProjectAndAuthorEmail(Project project, String authorEmail);
    
    List<Commit> findByAuthorName(String authorName);
    
    List<Commit> findByProjectAndCommittedAtAfter(Project project, LocalDateTime startDate);
    
    @Query("SELECT SUM(c.linesAdded) FROM Commit c WHERE c.project = :project")
    Long getTotalLinesAddedByProject(@Param("project") Project project);
    
    @Query("SELECT SUM(c.linesModified) FROM Commit c WHERE c.project = :project")
    Long getTotalLinesModifiedByProject(@Param("project") Project project);
    
    @Query("SELECT SUM(c.linesDeleted) FROM Commit c WHERE c.project = :project")
    Long getTotalLinesDeletedByProject(@Param("project") Project project);
    
    @Query("SELECT SUM(c.filesChanged) FROM Commit c WHERE c.project = :project")
    Long getTotalFilesChangedByProject(@Param("project") Project project);
    
    @Query("SELECT c.authorName, COUNT(c), SUM(c.linesAdded), SUM(c.linesModified), SUM(c.linesDeleted) " +
           "FROM Commit c WHERE c.project = :project " +
           "GROUP BY c.authorName ORDER BY COUNT(c) DESC")
    List<Object[]> getContributorStatsByProject(@Param("project") Project project);
}

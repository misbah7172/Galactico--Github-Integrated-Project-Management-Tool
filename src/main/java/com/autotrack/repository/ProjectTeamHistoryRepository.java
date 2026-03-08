package com.autotrack.repository;

import com.autotrack.model.Project;
import com.autotrack.model.ProjectTeamHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ProjectTeamHistory entity.
 */
@Repository
public interface ProjectTeamHistoryRepository extends JpaRepository<ProjectTeamHistory, Long> {
    
    List<ProjectTeamHistory> findByProjectOrderByChangedAtDesc(Project project);
    
    List<ProjectTeamHistory> findByProjectIdOrderByChangedAtDesc(Long projectId);
}

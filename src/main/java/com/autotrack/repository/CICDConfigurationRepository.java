package com.autotrack.repository;

import com.autotrack.model.CICDConfiguration;
import com.autotrack.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for CI/CD configuration entities.
 */
@Repository
public interface CICDConfigurationRepository extends JpaRepository<CICDConfiguration, Long> {
    
    /**
     * Find active CI/CD configuration by project.
     */
    Optional<CICDConfiguration> findByProjectAndIsActive(Project project, Boolean isActive);
    
    /**
     * Find all CI/CD configurations by project (including inactive ones).
     */
    List<CICDConfiguration> findByProjectOrderByGeneratedAtDesc(Project project);
    
    /**
     * Find all active CI/CD configurations.
     */
    List<CICDConfiguration> findByIsActiveOrderByGeneratedAtDesc(Boolean isActive);
    
    /**
     * Find CI/CD configurations by project type.
     */
    List<CICDConfiguration> findByProjectTypeAndIsActive(String projectType, Boolean isActive);
    
    /**
     * Find CI/CD configurations by deploy strategy.
     */
    List<CICDConfiguration> findByDeployStrategyAndIsActive(String deployStrategy, Boolean isActive);
    
    /**
     * Find configurations that haven't run in a specified time period.
     */
    @Query("SELECT c FROM CICDConfiguration c WHERE c.isActive = true AND " +
           "(c.lastPipelineRun IS NULL OR c.lastPipelineRun < :cutoffDate)")
    List<CICDConfiguration> findStaleConfigurations(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Find configurations with failed last pipeline status.
     */
    List<CICDConfiguration> findByLastPipelineStatusAndIsActive(String status, Boolean isActive);
    
    /**
     * Count active configurations by project type.
     */
    @Query("SELECT c.projectType, COUNT(c) FROM CICDConfiguration c WHERE c.isActive = true GROUP BY c.projectType")
    List<Object[]> countActiveConfigurationsByProjectType();
    
    /**
     * Count active configurations by deploy strategy.
     */
    @Query("SELECT c.deployStrategy, COUNT(c) FROM CICDConfiguration c WHERE c.isActive = true GROUP BY c.deployStrategy")
    List<Object[]> countActiveConfigurationsByDeployStrategy();
}

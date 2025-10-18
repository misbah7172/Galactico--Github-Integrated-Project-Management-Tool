package com.autotrack.service;

import com.autotrack.dto.CICDConfigDTO;
import com.autotrack.model.CICDConfiguration;
import com.autotrack.model.Project;
import com.autotrack.repository.CICDConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CICDGeneratorService.
 */
@ExtendWith(MockitoExtension.class)
class CICDGeneratorServiceTest {

    @Mock
    private GitHubService gitHubService;

    @Mock
    private CICDConfigurationRepository cicdConfigRepository;

    @InjectMocks
    private CICDGeneratorService cicdGeneratorService;

    private Project testProject;
    private CICDConfigDTO testConfigDTO;

    @BeforeEach
    void setUp() {
        // Create test project
        testProject = Project.builder()
                .id(1L)
                .name("Test Project")
                .gitHubRepoUrl("https://github.com/testuser/testrepo")
                .gitHubAccessToken("test_token")
                .build();

        // Create test configuration
        testConfigDTO = new CICDConfigDTO();
        testConfigDTO.setProjectName("Test Project");
        testConfigDTO.setProjectType("node");
        testConfigDTO.setTestCommand("npm test");
        testConfigDTO.setBuildCommand("npm run build");
        testConfigDTO.setDeployStrategy("staging");
    }

    @Test
    void testGenerateNodeJSPipeline() {
        // Mock repository save
        CICDConfiguration savedConfig = CICDConfiguration.builder()
                .id(1L)
                .project(testProject)
                .projectType("node")
                .testCommand("npm test")
                .buildCommand("npm run build")
                .deployStrategy("staging")
                .generatedAt(LocalDateTime.now())
                .isActive(true)
                .build();

        when(cicdConfigRepository.save(any(CICDConfiguration.class))).thenReturn(savedConfig);
        when(gitHubService.createOrUpdateFile(any(), any(), any(), any())).thenReturn("https://github.com/test/workflow");

        // Execute
        CICDConfiguration result = cicdGeneratorService.generateCICDPipeline(testProject, testConfigDTO);

        // Verify
        assertNotNull(result);
        assertEquals("node", result.getProjectType());
        assertEquals("staging", result.getDeployStrategy());
        assertTrue(result.getIsActive());
        verify(cicdConfigRepository, times(1)).save(any(CICDConfiguration.class));
    }

    @Test
    void testGeneratePythonPipeline() {
        testConfigDTO.setProjectType("python");
        testConfigDTO.setTestCommand("pytest");
        testConfigDTO.setBuildCommand("python setup.py build");

        CICDConfiguration savedConfig = CICDConfiguration.builder()
                .id(2L)
                .project(testProject)
                .projectType("python")
                .testCommand("pytest")
                .buildCommand("python setup.py build")
                .deployStrategy("staging")
                .generatedAt(LocalDateTime.now())
                .isActive(true)
                .build();

        when(cicdConfigRepository.save(any(CICDConfiguration.class))).thenReturn(savedConfig);

        CICDConfiguration result = cicdGeneratorService.generateCICDPipeline(testProject, testConfigDTO);

        assertNotNull(result);
        assertEquals("python", result.getProjectType());
        verify(cicdConfigRepository, times(1)).save(any(CICDConfiguration.class));
    }

    @Test
    void testGenerateDockerPipeline() {
        testConfigDTO.setProjectType("docker");
        testConfigDTO.setDeployStrategy("docker");

        CICDConfiguration savedConfig = CICDConfiguration.builder()
                .id(3L)
                .project(testProject)
                .projectType("docker")
                .deployStrategy("docker")
                .generatedAt(LocalDateTime.now())
                .isActive(true)
                .build();

        when(cicdConfigRepository.save(any(CICDConfiguration.class))).thenReturn(savedConfig);

        CICDConfiguration result = cicdGeneratorService.generateCICDPipeline(testProject, testConfigDTO);

        assertNotNull(result);
        assertEquals("docker", result.getProjectType());
        assertEquals("docker", result.getDeployStrategy());
    }

    @Test
    void testUpdateExistingConfiguration() {
        // Mock existing configuration
        CICDConfiguration existingConfig = CICDConfiguration.builder()
                .id(1L)
                .project(testProject)
                .projectType("node")
                .deployStrategy("none")
                .isActive(true)
                .build();

        when(cicdConfigRepository.findByProjectAndIsActive(testProject, true))
                .thenReturn(Optional.of(existingConfig));

        CICDConfiguration updatedConfig = CICDConfiguration.builder()
                .id(2L)
                .project(testProject)
                .projectType("node")
                .deployStrategy("staging")
                .isActive(true)
                .generatedAt(LocalDateTime.now())
                .build();

        when(cicdConfigRepository.save(any(CICDConfiguration.class))).thenReturn(updatedConfig);

        CICDConfiguration result = cicdGeneratorService.updateCICDPipeline(testProject, testConfigDTO);

        assertNotNull(result);
        assertEquals("staging", result.getDeployStrategy());
        verify(cicdConfigRepository, times(2)).save(any(CICDConfiguration.class)); // Once to deactivate, once to create new
    }

    @Test
    void testGetCICDConfigByProject() {
        CICDConfiguration existingConfig = CICDConfiguration.builder()
                .id(1L)
                .project(testProject)
                .projectType("node")
                .deployStrategy("staging")
                .isActive(true)
                .build();

        when(cicdConfigRepository.findByProjectAndIsActive(testProject, true))
                .thenReturn(Optional.of(existingConfig));

        CICDConfiguration result = cicdGeneratorService.getCICDConfigByProject(testProject);

        assertNotNull(result);
        assertEquals(existingConfig.getId(), result.getId());
        assertEquals("node", result.getProjectType());
    }

    @Test
    void testDeleteCICDPipeline() {
        CICDConfiguration existingConfig = CICDConfiguration.builder()
                .id(1L)
                .project(testProject)
                .isActive(true)
                .build();

        when(cicdConfigRepository.findByProjectAndIsActive(testProject, true))
                .thenReturn(Optional.of(existingConfig));

        cicdGeneratorService.deleteCICDPipeline(testProject);

        verify(cicdConfigRepository, times(1)).save(any(CICDConfiguration.class));
        assertFalse(existingConfig.getIsActive()); // Should be deactivated
    }

    @Test
    void testGenerateYamlContentContainsCorrectElements() {
        when(cicdConfigRepository.save(any(CICDConfiguration.class))).thenAnswer(invocation -> {
            CICDConfiguration config = invocation.getArgument(0);
            String yamlContent = config.getPipelineContent();
            
            // Verify YAML content contains expected elements
            assertNotNull(yamlContent);
            assertTrue(yamlContent.contains("name: CI/CD Pipeline"));
            assertTrue(yamlContent.contains("on:"));
            assertTrue(yamlContent.contains("push:"));
            assertTrue(yamlContent.contains("pull_request:"));
            assertTrue(yamlContent.contains("jobs:"));
            assertTrue(yamlContent.contains("test:"));
            assertTrue(yamlContent.contains("build:"));
            assertTrue(yamlContent.contains("deploy:")); // Since deploy strategy is staging
            assertTrue(yamlContent.contains("npm test")); // Test command
            assertTrue(yamlContent.contains("npm run build")); // Build command
            
            return config;
        });

        cicdGeneratorService.generateCICDPipeline(testProject, testConfigDTO);

        verify(cicdConfigRepository, times(1)).save(any(CICDConfiguration.class));
    }

    @Test
    void testNoneDeployStrategyDoesNotIncludeDeployJob() {
        testConfigDTO.setDeployStrategy("none");

        when(cicdConfigRepository.save(any(CICDConfiguration.class))).thenAnswer(invocation -> {
            CICDConfiguration config = invocation.getArgument(0);
            String yamlContent = config.getPipelineContent();
            
            // Verify YAML content does not contain deploy job for "none" strategy
            assertNotNull(yamlContent);
            assertTrue(yamlContent.contains("test:"));
            assertFalse(yamlContent.contains("build:")); // No build job for "none" strategy
            assertFalse(yamlContent.contains("deploy:")); // No deploy job for "none" strategy
            
            return config;
        });

        cicdGeneratorService.generateCICDPipeline(testProject, testConfigDTO);

        verify(cicdConfigRepository, times(1)).save(any(CICDConfiguration.class));
    }
}

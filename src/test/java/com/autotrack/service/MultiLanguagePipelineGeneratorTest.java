package com.autotrack.service;

import com.autotrack.dto.MultiLanguageCICDConfigDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MultiLanguagePipelineGenerator
 * These are pure unit tests that don't require Spring context
 */
public class MultiLanguagePipelineGeneratorTest {

    private MultiLanguagePipelineGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new MultiLanguagePipelineGenerator();
    }

    @Test
    void testGenerateMonolithWorkflow() {
        // Arrange
        MultiLanguageCICDConfigDTO config = new MultiLanguageCICDConfigDTO();
        config.setProjectName("test-project");
        config.setProjectArchitecture("MONOLITH");
        config.setDeployStrategy("staging");

        MultiLanguageCICDConfigDTO.LanguageComponent frontendComponent = new MultiLanguageCICDConfigDTO.LanguageComponent();
        frontendComponent.setLanguage("REACT");
        frontendComponent.setDirectory("./frontend");
        frontendComponent.setBuildCommand("npm run build");
        frontendComponent.setTestCommand("npm test");
        frontendComponent.setVersion("18");
        frontendComponent.setMainComponent(true);

        MultiLanguageCICDConfigDTO.LanguageComponent backendComponent = new MultiLanguageCICDConfigDTO.LanguageComponent();
        backendComponent.setLanguage("JAVA");
        backendComponent.setDirectory("./backend");
        backendComponent.setBuildCommand("mvn package");
        backendComponent.setTestCommand("mvn test");
        backendComponent.setVersion("17");
        backendComponent.setMainComponent(false);

        config.setComponents(Arrays.asList(frontendComponent, backendComponent));

        // Act
        String workflow = generator.generateWorkflow(config);

        // Assert
        assertNotNull(workflow);
        assertTrue(workflow.contains("name: Multi-Language CI/CD Pipeline"));
        assertTrue(workflow.contains("test-project"));
        assertTrue(workflow.contains("MONOLITH"));
        assertTrue(workflow.contains("setup-node"));
        assertTrue(workflow.contains("setup-java"));
        assertTrue(workflow.contains("npm run build"));
        assertTrue(workflow.contains("mvn package"));
    }

    @Test
    void testGenerateMicroservicesWorkflow() {
        // Arrange
        MultiLanguageCICDConfigDTO config = new MultiLanguageCICDConfigDTO();
        config.setProjectName("microservices-project");
        config.setProjectArchitecture("MICROSERVICES");
        config.setDeployStrategy("production");

        MultiLanguageCICDConfigDTO.LanguageComponent userService = new MultiLanguageCICDConfigDTO.LanguageComponent();
        userService.setLanguage("NODE_JS");
        userService.setDirectory("./services/user-service");
        userService.setBuildCommand("npm run build");
        userService.setTestCommand("npm test");
        userService.setVersion("18");

        MultiLanguageCICDConfigDTO.LanguageComponent authService = new MultiLanguageCICDConfigDTO.LanguageComponent();
        authService.setLanguage("PYTHON");
        authService.setDirectory("./services/auth-service");
        authService.setBuildCommand("pip install -r requirements.txt");
        authService.setTestCommand("pytest");
        authService.setVersion("3.9");

        config.setComponents(Arrays.asList(userService, authService));

        // Act
        String workflow = generator.generateWorkflow(config);

        // Assert
        assertNotNull(workflow);
        assertTrue(workflow.contains("MICROSERVICES"));
        assertTrue(workflow.contains("user-service"));
        assertTrue(workflow.contains("auth-service"));
        assertTrue(workflow.contains("setup-node"));
        assertTrue(workflow.contains("setup-python"));
        assertTrue(workflow.contains("strategy:"));
        assertTrue(workflow.contains("matrix:"));
    }

    @Test
    void testGenerateFullStackWorkflow() {
        // Arrange
        MultiLanguageCICDConfigDTO config = new MultiLanguageCICDConfigDTO();
        config.setProjectName("fullstack-app");
        config.setProjectArchitecture("FULLSTACK");
        config.setDeployStrategy("production");

        MultiLanguageCICDConfigDTO.LanguageComponent frontend = new MultiLanguageCICDConfigDTO.LanguageComponent();
        frontend.setLanguage("REACT");
        frontend.setDirectory("./frontend");
        frontend.setBuildCommand("npm run build");
        frontend.setTestCommand("npm test");
        frontend.setMainComponent(true);

        MultiLanguageCICDConfigDTO.LanguageComponent backend = new MultiLanguageCICDConfigDTO.LanguageComponent();
        backend.setLanguage("SPRING_BOOT");
        backend.setDirectory("./backend");
        backend.setBuildCommand("mvn package");
        backend.setTestCommand("mvn test");
        backend.setMainComponent(false);

        config.setComponents(Arrays.asList(frontend, backend));

        // Act
        String workflow = generator.generateWorkflow(config);

        // Assert
        assertNotNull(workflow);
        assertTrue(workflow.contains("FULLSTACK"));
        assertTrue(workflow.contains("frontend-build"));
        assertTrue(workflow.contains("backend-build"));
        assertTrue(workflow.contains("needs: [frontend-build, backend-build]"));
    }

    @Test
    void testGenerateExtensionWorkflow() {
        // Arrange
        MultiLanguageCICDConfigDTO config = new MultiLanguageCICDConfigDTO();
        config.setProjectName("vscode-extension");
        config.setProjectArchitecture("EXTENSION");
        config.setDeployStrategy("none");

        MultiLanguageCICDConfigDTO.LanguageComponent extension = new MultiLanguageCICDConfigDTO.LanguageComponent();
        extension.setLanguage("TYPESCRIPT");
        extension.setDirectory(".");
        extension.setBuildCommand("npm run compile");
        extension.setTestCommand("npm test");
        extension.setVersion("18");
        extension.setMainComponent(true);

        config.setComponents(Arrays.asList(extension));

        // Act
        String workflow = generator.generateWorkflow(config);

        // Assert
        assertNotNull(workflow);
        assertTrue(workflow.contains("EXTENSION"));
        assertTrue(workflow.contains("vsce package"));
        assertTrue(workflow.contains("upload-artifact"));
    }

    @Test
    void testInvalidArchitecture() {
        // Arrange
        MultiLanguageCICDConfigDTO config = new MultiLanguageCICDConfigDTO();
        config.setProjectName("test-project");
        config.setProjectArchitecture("INVALID");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            generator.generateWorkflow(config);
        });
    }
}

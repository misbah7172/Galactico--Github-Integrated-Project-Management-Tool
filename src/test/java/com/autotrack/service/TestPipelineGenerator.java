package com.autotrack.service;

import com.autotrack.dto.MultiLanguageCICDConfigDTO;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Quick manual test to debug the pipeline generator
 */
public class TestPipelineGenerator {
    public static void main(String[] args) {
        MultiLanguagePipelineGenerator generator = new MultiLanguagePipelineGenerator();
        
        // Create test configuration
        MultiLanguageCICDConfigDTO config = new MultiLanguageCICDConfigDTO();
        config.setProjectName("test-project");
        config.setProjectArchitecture("monolith");
        config.setDeployStrategy("staging");
        config.setEnvironmentVariables(new HashMap<>());
        
        // Create language components
        MultiLanguageCICDConfigDTO.LanguageComponent nodeComponent = 
            new MultiLanguageCICDConfigDTO.LanguageComponent();
        nodeComponent.setLanguage("node.js");
        nodeComponent.setDirectory("frontend");
        nodeComponent.setBuildCommand("npm install");
        nodeComponent.setTestCommand("npm test");
        nodeComponent.setVersion("18");
        nodeComponent.setMainComponent(true);
        nodeComponent.setDependencies(new ArrayList<>());
        
        MultiLanguageCICDConfigDTO.LanguageComponent javaComponent = 
            new MultiLanguageCICDConfigDTO.LanguageComponent();
        javaComponent.setLanguage("java");
        javaComponent.setDirectory("backend");
        javaComponent.setBuildCommand("mvn clean compile");
        javaComponent.setTestCommand("mvn test");
        javaComponent.setVersion("17");
        javaComponent.setMainComponent(false);
        javaComponent.setDependencies(new ArrayList<>());
        
        config.setComponents(Arrays.asList(nodeComponent, javaComponent));
        
        try {
            // Generate workflow
            String workflow = generator.generateWorkflow(config);
            System.out.println("=== Generated Workflow ===");
            System.out.println(workflow);
            System.out.println("=== End Workflow ===");
            
            // Test basic assertions
            System.out.println("\n=== Test Results ===");
            System.out.println("Contains 'Multi-Language CI/CD Pipeline': " + workflow.contains("Multi-Language CI/CD Pipeline"));
            System.out.println("Contains 'setup-node': " + workflow.contains("setup-node"));
            System.out.println("Contains 'setup-java': " + workflow.contains("setup-java"));
            System.out.println("Contains 'npm install': " + workflow.contains("npm install"));
            System.out.println("Contains 'mvn clean compile': " + workflow.contains("mvn clean compile"));
            System.out.println("Contains 'Deploy to staging': " + workflow.contains("Deploy to staging"));
            
        } catch (Exception e) {
            System.err.println("Error generating workflow: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

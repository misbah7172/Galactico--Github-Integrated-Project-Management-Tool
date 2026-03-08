package com.autotrack.service;

import com.autotrack.dto.CICDConfigDTO;
import com.autotrack.model.Project;
import com.autotrack.model.CICDConfiguration;
import com.autotrack.repository.CICDConfigurationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for generating and managing CI/CD pipelines for projects.
 */
@Service
public class CICDGeneratorService {
    
    private static final Logger logger = LoggerFactory.getLogger(CICDGeneratorService.class);
    
    @Autowired
    private GitHubService gitHubService;
    
    @Autowired
    private CICDConfigurationRepository cicdConfigRepository;
    
    @Value("${cicd.local.workspace.path:${user.home}/galactico-workspace}")
    private String localWorkspacePath;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Generate and deploy CI/CD pipeline for a project.
     */
    @Transactional
    public CICDConfiguration generateCICDPipeline(Project project, CICDConfigDTO configDTO) {
        try {
            logger.info("Generating CI/CD pipeline for project: {}", project.getName());
            
            // Generate pipeline YAML content
            String pipelineContent = generatePipelineYaml(configDTO);
            
            // Create configuration JSON
            String configJson = createConfigurationJson(configDTO);
            
            // Save to local workspace if enabled
            String localPipelinePath = null;
            String localConfigPath = null;
            if (isLocalWorkspaceEnabled()) {
                localPipelinePath = saveToLocalWorkspace(project, pipelineContent, "ci.yml");
                localConfigPath = saveToLocalWorkspace(project, configJson, "ciConfig.json");
            }
            
            // Push to GitHub repository if token is available
            String githubPipelinePath = null;
            String githubConfigPath = null;
            if (project.getGitHubAccessToken() != null && !project.getGitHubAccessToken().isEmpty()) {
                githubPipelinePath = pushToGitHub(project, pipelineContent, ".github/workflows/ci.yml");
                githubConfigPath = pushToGitHub(project, configJson, "ciConfig.json");
            }
            
            // Save configuration to database
            CICDConfiguration cicdConfig = CICDConfiguration.builder()
                    .project(project)
                    .projectType(configDTO.getProjectType())
                    .testCommand(configDTO.getTestCommand())
                    .buildCommand(configDTO.getBuildCommand())
                    .deployStrategy(configDTO.getDeployStrategy())
                    .pipelineContent(pipelineContent)
                    .configurationJson(configJson)
                    .localPipelinePath(localPipelinePath)
                    .localConfigPath(localConfigPath)
                    .githubPipelinePath(githubPipelinePath)
                    .githubConfigPath(githubConfigPath)
                    .generatedAt(LocalDateTime.now())
                    .isActive(true)
                    .build();
            
            CICDConfiguration savedConfig = cicdConfigRepository.save(cicdConfig);
            
            logger.info("CI/CD pipeline generated successfully for project: {}", project.getName());
            return savedConfig;
            
        } catch (Exception e) {
            logger.error("Failed to generate CI/CD pipeline for project: {}", project.getName(), e);
            throw new RuntimeException("Failed to generate CI/CD pipeline: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate GitHub Actions YAML content based on project type and configuration.
     */
    private String generatePipelineYaml(CICDConfigDTO config) {
        StringBuilder yaml = new StringBuilder();
        
        // Header
        yaml.append("name: CI/CD Pipeline\n");
        yaml.append("on:\n");
        yaml.append("  push:\n");
        yaml.append("    branches: [ main, develop ]\n");
        yaml.append("  pull_request:\n");
        yaml.append("    branches: [ main ]\n\n");
        
        // Environment variables
        yaml.append("env:\n");
        yaml.append("  PROJECT_TYPE: ").append(config.getProjectType()).append("\n");
        yaml.append("  DEPLOY_STRATEGY: ").append(config.getDeployStrategy()).append("\n\n");
        
        yaml.append("jobs:\n");
        
        // Test job
        generateTestJob(yaml, config);
        
        // Build job (if not "none" deploy strategy)
        if (!"none".equals(config.getDeployStrategy())) {
            generateBuildJob(yaml, config);
        }
        
        // Deploy job (if deploy strategy is specified)
        if (!"none".equals(config.getDeployStrategy())) {
            generateDeployJob(yaml, config);
        }
        
        return yaml.toString();
    }
    
    /**
     * Generate test job section.
     */
    private void generateTestJob(StringBuilder yaml, CICDConfigDTO config) {
        yaml.append("  test:\n");
        yaml.append("    runs-on: ubuntu-latest\n");
        yaml.append("    \n");
        yaml.append("    steps:\n");
        yaml.append("    - name: Checkout code\n");
        yaml.append("      uses: actions/checkout@v4\n\n");
        
        switch (config.getProjectType().toLowerCase()) {
            case "node":
                generateNodeTestSteps(yaml, config);
                break;
            case "python":
                generatePythonTestSteps(yaml, config);
                break;
            case "react":
                generateReactTestSteps(yaml, config);
                break;
            case "docker":
                generateDockerTestSteps(yaml, config);
                break;
            case "java":
                generateJavaTestSteps(yaml, config);
                break;
            default:
                generateCustomTestSteps(yaml, config);
        }
    }
    
    /**
     * Generate build job section.
     */
    private void generateBuildJob(StringBuilder yaml, CICDConfigDTO config) {
        yaml.append("  build:\n");
        yaml.append("    needs: test\n");
        yaml.append("    runs-on: ubuntu-latest\n");
        yaml.append("    if: github.ref == 'refs/heads/main'\n\n");
        yaml.append("    steps:\n");
        yaml.append("    - name: Checkout code\n");
        yaml.append("      uses: actions/checkout@v4\n\n");
        
        switch (config.getProjectType().toLowerCase()) {
            case "node":
                generateNodeBuildSteps(yaml, config);
                break;
            case "python":
                generatePythonBuildSteps(yaml, config);
                break;
            case "react":
                generateReactBuildSteps(yaml, config);
                break;
            case "docker":
                generateDockerBuildSteps(yaml, config);
                break;
            case "java":
                generateJavaBuildSteps(yaml, config);
                break;
            default:
                generateCustomBuildSteps(yaml, config);
        }
    }
    
    /**
     * Generate deploy job section.
     */
    private void generateDeployJob(StringBuilder yaml, CICDConfigDTO config) {
        yaml.append("  deploy:\n");
        yaml.append("    needs: build\n");
        yaml.append("    runs-on: ubuntu-latest\n");
        yaml.append("    if: github.ref == 'refs/heads/main'\n\n");
        yaml.append("    steps:\n");
        yaml.append("    - name: Checkout code\n");
        yaml.append("      uses: actions/checkout@v4\n\n");
        
        switch (config.getDeployStrategy().toLowerCase()) {
            case "staging":
                generateStagingDeploySteps(yaml, config);
                break;
            case "production":
                generateProductionDeploySteps(yaml, config);
                break;
            case "docker":
                generateDockerDeploySteps(yaml, config);
                break;
            case "aws-lambda":
                generateLambdaDeploySteps(yaml, config);
                break;
            default:
                generateCustomDeploySteps(yaml, config);
        }
    }
    
    // Node.js specific steps
    private void generateNodeTestSteps(StringBuilder yaml, CICDConfigDTO config) {
        yaml.append("    - name: Setup Node.js\n");
        yaml.append("      uses: actions/setup-node@v4\n");
        yaml.append("      with:\n");
        yaml.append("        node-version: '18'\n");
        yaml.append("        cache: 'npm'\n\n");
        
        yaml.append("    - name: Install dependencies\n");
        yaml.append("      run: npm ci\n\n");
        
        yaml.append("    - name: Run linting\n");
        yaml.append("      run: npm run lint\n\n");
        
        yaml.append("    - name: Run tests\n");
        if (config.getTestCommand() != null && !config.getTestCommand().isEmpty()) {
            yaml.append("      run: ").append(config.getTestCommand()).append("\n\n");
        } else {
            yaml.append("      run: npm test\n\n");
        }
    }
    
    private void generateNodeBuildSteps(StringBuilder yaml, CICDConfigDTO config) {
        yaml.append("    - name: Setup Node.js\n");
        yaml.append("      uses: actions/setup-node@v4\n");
        yaml.append("      with:\n");
        yaml.append("        node-version: '18'\n");
        yaml.append("        cache: 'npm'\n\n");
        
        yaml.append("    - name: Install dependencies\n");
        yaml.append("      run: npm ci\n\n");
        
        yaml.append("    - name: Build application\n");
        if (config.getBuildCommand() != null && !config.getBuildCommand().isEmpty()) {
            yaml.append("      run: ").append(config.getBuildCommand()).append("\n\n");
        } else {
            yaml.append("      run: npm run build\n\n");
        }
        
        yaml.append("    - name: Upload build artifacts\n");
        yaml.append("      uses: actions/upload-artifact@v4\n");
        yaml.append("      with:\n");
        yaml.append("        name: build-artifacts\n");
        yaml.append("        path: dist/\n\n");
    }
    
    // Python specific steps
    private void generatePythonTestSteps(StringBuilder yaml, CICDConfigDTO config) {
        yaml.append("    - name: Setup Python\n");
        yaml.append("      uses: actions/setup-python@v4\n");
        yaml.append("      with:\n");
        yaml.append("        python-version: '3.9'\n\n");
        
        yaml.append("    - name: Install dependencies\n");
        yaml.append("      run: |\n");
        yaml.append("        python -m pip install --upgrade pip\n");
        yaml.append("        pip install -r requirements.txt\n\n");
        
        yaml.append("    - name: Run tests\n");
        if (config.getTestCommand() != null && !config.getTestCommand().isEmpty()) {
            yaml.append("      run: ").append(config.getTestCommand()).append("\n\n");
        } else {
            yaml.append("      run: pytest\n\n");
        }
    }
    
    private void generatePythonBuildSteps(StringBuilder yaml, CICDConfigDTO config) {
        yaml.append("    - name: Setup Python\n");
        yaml.append("      uses: actions/setup-python@v4\n");
        yaml.append("      with:\n");
        yaml.append("        python-version: '3.9'\n\n");
        
        yaml.append("    - name: Install dependencies\n");
        yaml.append("      run: |\n");
        yaml.append("        python -m pip install --upgrade pip\n");
        yaml.append("        pip install -r requirements.txt\n\n");
        
        if (config.getBuildCommand() != null && !config.getBuildCommand().isEmpty()) {
            yaml.append("    - name: Build application\n");
            yaml.append("      run: ").append(config.getBuildCommand()).append("\n\n");
        }
    }
    
    // React specific steps
    private void generateReactTestSteps(StringBuilder yaml, CICDConfigDTO config) {
        generateNodeTestSteps(yaml, config); // React uses Node.js
    }
    
    private void generateReactBuildSteps(StringBuilder yaml, CICDConfigDTO config) {
        generateNodeBuildSteps(yaml, config); // React uses Node.js
        
        yaml.append("    - name: Upload build to S3 (if configured)\n");
        yaml.append("      if: env.AWS_S3_BUCKET != ''\n");
        yaml.append("      env:\n");
        yaml.append("        AWS_ACCESS_KEY_ID: ${{ secrets.AWS_ACCESS_KEY_ID }}\n");
        yaml.append("        AWS_SECRET_ACCESS_KEY: ${{ secrets.AWS_SECRET_ACCESS_KEY }}\n");
        yaml.append("        AWS_S3_BUCKET: ${{ secrets.AWS_S3_BUCKET }}\n");
        yaml.append("      run: |\n");
        yaml.append("        aws s3 sync dist/ s3://$AWS_S3_BUCKET/ --delete\n\n");
    }
    
    // Docker specific steps
    private void generateDockerTestSteps(StringBuilder yaml, CICDConfigDTO config) {
        yaml.append("    - name: Build Docker image\n");
        yaml.append("      run: docker build -t test-image .\n\n");
        
        yaml.append("    - name: Run container tests\n");
        if (config.getTestCommand() != null && !config.getTestCommand().isEmpty()) {
            yaml.append("      run: ").append(config.getTestCommand()).append("\n\n");
        } else {
            yaml.append("      run: docker run --rm test-image npm test\n\n");
        }
    }
    
    private void generateDockerBuildSteps(StringBuilder yaml, CICDConfigDTO config) {
        yaml.append("    - name: Set up Docker Buildx\n");
        yaml.append("      uses: docker/setup-buildx-action@v3\n\n");
        
        yaml.append("    - name: Login to DockerHub\n");
        yaml.append("      if: env.DOCKERHUB_USERNAME != ''\n");
        yaml.append("      uses: docker/login-action@v3\n");
        yaml.append("      with:\n");
        yaml.append("        username: ${{ secrets.DOCKERHUB_USERNAME }}\n");
        yaml.append("        password: ${{ secrets.DOCKERHUB_TOKEN }}\n\n");
        
        yaml.append("    - name: Build and push Docker image\n");
        yaml.append("      uses: docker/build-push-action@v5\n");
        yaml.append("      with:\n");
        yaml.append("        context: .\n");
        yaml.append("        push: true\n");
        yaml.append("        tags: ${{ secrets.DOCKERHUB_USERNAME }}/${{ github.event.repository.name }}:latest\n\n");
    }
    
    // Java specific steps
    private void generateJavaTestSteps(StringBuilder yaml, CICDConfigDTO config) {
        yaml.append("    - name: Set up JDK 17\n");
        yaml.append("      uses: actions/setup-java@v4\n");
        yaml.append("      with:\n");
        yaml.append("        java-version: '17'\n");
        yaml.append("        distribution: 'temurin'\n\n");
        
        yaml.append("    - name: Cache Maven dependencies\n");
        yaml.append("      uses: actions/cache@v4\n");
        yaml.append("      with:\n");
        yaml.append("        path: ~/.m2\n");
        yaml.append("        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}\n");
        yaml.append("        restore-keys: ${{ runner.os }}-m2\n\n");
        
        yaml.append("    - name: Run tests\n");
        if (config.getTestCommand() != null && !config.getTestCommand().isEmpty()) {
            yaml.append("      run: ").append(config.getTestCommand()).append("\n\n");
        } else {
            yaml.append("      run: mvn clean test\n\n");
        }
    }
    
    private void generateJavaBuildSteps(StringBuilder yaml, CICDConfigDTO config) {
        yaml.append("    - name: Set up JDK 17\n");
        yaml.append("      uses: actions/setup-java@v4\n");
        yaml.append("      with:\n");
        yaml.append("        java-version: '17'\n");
        yaml.append("        distribution: 'temurin'\n\n");
        
        yaml.append("    - name: Cache Maven dependencies\n");
        yaml.append("      uses: actions/cache@v4\n");
        yaml.append("      with:\n");
        yaml.append("        path: ~/.m2\n");
        yaml.append("        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}\n");
        yaml.append("        restore-keys: ${{ runner.os }}-m2\n\n");
        
        yaml.append("    - name: Build application\n");
        if (config.getBuildCommand() != null && !config.getBuildCommand().isEmpty()) {
            yaml.append("      run: ").append(config.getBuildCommand()).append("\n\n");
        } else {
            yaml.append("      run: mvn clean package -DskipTests\n\n");
        }
        
        yaml.append("    - name: Upload JAR artifact\n");
        yaml.append("      uses: actions/upload-artifact@v4\n");
        yaml.append("      with:\n");
        yaml.append("        name: jar-artifact\n");
        yaml.append("        path: target/*.jar\n\n");
    }
    
    // Custom project type steps
    private void generateCustomTestSteps(StringBuilder yaml, CICDConfigDTO config) {
        if (config.getTestCommand() != null && !config.getTestCommand().isEmpty()) {
            yaml.append("    - name: Run custom tests\n");
            yaml.append("      run: ").append(config.getTestCommand()).append("\n\n");
        } else {
            yaml.append("    - name: Skip tests (no test command specified)\n");
            yaml.append("      run: echo \"No test command specified for custom project type\"\n\n");
        }
    }
    
    private void generateCustomBuildSteps(StringBuilder yaml, CICDConfigDTO config) {
        if (config.getBuildCommand() != null && !config.getBuildCommand().isEmpty()) {
            yaml.append("    - name: Run custom build\n");
            yaml.append("      run: ").append(config.getBuildCommand()).append("\n\n");
        } else {
            yaml.append("    - name: Skip build (no build command specified)\n");
            yaml.append("      run: echo \"No build command specified for custom project type\"\n\n");
        }
    }
    
    // Deploy strategy steps
    private void generateStagingDeploySteps(StringBuilder yaml, CICDConfigDTO config) {
        yaml.append("    - name: Deploy to staging\n");
        yaml.append("      env:\n");
        yaml.append("        STAGING_URL: ${{ secrets.STAGING_URL }}\n");
        yaml.append("        STAGING_TOKEN: ${{ secrets.STAGING_TOKEN }}\n");
        yaml.append("      run: |\n");
        yaml.append("        echo \"Deploying to staging environment\"\n");
        yaml.append("        # Add your staging deployment commands here\n\n");
    }
    
    private void generateProductionDeploySteps(StringBuilder yaml, CICDConfigDTO config) {
        yaml.append("    - name: Deploy to production\n");
        yaml.append("      env:\n");
        yaml.append("        PRODUCTION_URL: ${{ secrets.PRODUCTION_URL }}\n");
        yaml.append("        PRODUCTION_TOKEN: ${{ secrets.PRODUCTION_TOKEN }}\n");
        yaml.append("      run: |\n");
        yaml.append("        echo \"Deploying to production environment\"\n");
        yaml.append("        # Add your production deployment commands here\n\n");
    }
    
    private void generateDockerDeploySteps(StringBuilder yaml, CICDConfigDTO config) {
        yaml.append("    - name: Deploy Docker container\n");
        yaml.append("      env:\n");
        yaml.append("        DOCKER_HOST: ${{ secrets.DOCKER_HOST }}\n");
        yaml.append("        DOCKER_CERT_PATH: ${{ secrets.DOCKER_CERT_PATH }}\n");
        yaml.append("      run: |\n");
        yaml.append("        echo \"Deploying Docker container\"\n");
        yaml.append("        docker pull ${{ secrets.DOCKERHUB_USERNAME }}/${{ github.event.repository.name }}:latest\n");
        yaml.append("        docker stop ${{ github.event.repository.name }} || true\n");
        yaml.append("        docker rm ${{ github.event.repository.name }} || true\n");
        yaml.append("        docker run -d --name ${{ github.event.repository.name }} -p 80:8080 ${{ secrets.DOCKERHUB_USERNAME }}/${{ github.event.repository.name }}:latest\n\n");
    }
    
    private void generateLambdaDeploySteps(StringBuilder yaml, CICDConfigDTO config) {
        yaml.append("    - name: Deploy to AWS Lambda\n");
        yaml.append("      env:\n");
        yaml.append("        AWS_ACCESS_KEY_ID: ${{ secrets.AWS_ACCESS_KEY_ID }}\n");
        yaml.append("        AWS_SECRET_ACCESS_KEY: ${{ secrets.AWS_SECRET_ACCESS_KEY }}\n");
        yaml.append("        AWS_REGION: ${{ secrets.AWS_REGION }}\n");
        yaml.append("        LAMBDA_FUNCTION_NAME: ${{ secrets.LAMBDA_FUNCTION_NAME }}\n");
        yaml.append("      run: |\n");
        yaml.append("        echo \"Deploying to AWS Lambda\"\n");
        yaml.append("        aws lambda update-function-code --function-name $LAMBDA_FUNCTION_NAME --zip-file fileb://function.zip\n\n");
    }
    
    private void generateCustomDeploySteps(StringBuilder yaml, CICDConfigDTO config) {
        yaml.append("    - name: Custom deployment\n");
        yaml.append("      run: |\n");
        yaml.append("        echo \"Custom deployment strategy - add your deployment commands here\"\n");
        yaml.append("        # Add your custom deployment logic\n\n");
    }
    
    /**
     * Create configuration JSON file content.
     */
    private String createConfigurationJson(CICDConfigDTO config) throws IOException {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("projectName", config.getProjectName());
        configMap.put("projectType", config.getProjectType());
        configMap.put("testCommand", config.getTestCommand());
        configMap.put("buildCommand", config.getBuildCommand());
        configMap.put("deployStrategy", config.getDeployStrategy());
        configMap.put("generatedOn", LocalDateTime.now().toString());
        configMap.put("version", "1.0.0");
        
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(configMap);
    }
    
    /**
     * Save files to local workspace.
     */
    private String saveToLocalWorkspace(Project project, String content, String filename) throws IOException {
        Path projectDir = Paths.get(localWorkspacePath, project.getName());
        Files.createDirectories(projectDir);
        
        Path filePath = projectDir.resolve(filename);
        Files.write(filePath, content.getBytes());
        
        logger.info("Saved {} to local workspace: {}", filename, filePath.toString());
        return filePath.toString();
    }
    
    /**
     * Push files to GitHub repository.
     */
    private String pushToGitHub(Project project, String content, String filePath) {
        try {
            String result = gitHubService.createOrUpdateFile(project, filePath, content, 
                    "Add/Update " + filePath + " via Galactico CI/CD Generator");
            
            if (result != null) {
                logger.info("Successfully pushed {} to GitHub: {}", filePath, result);
                return result;
            } else {
                logger.warn("Failed to push {} to GitHub for project: {}", filePath, project.getName());
                return null;
            }
        } catch (Exception e) {
            logger.error("Error pushing {} to GitHub for project: {}", filePath, project.getName(), e);
            return null;
        }
    }
    
    /**
     * Check if local workspace is enabled.
     */
    private boolean isLocalWorkspaceEnabled() {
        return localWorkspacePath != null && !localWorkspacePath.trim().isEmpty();
    }
    
    /**
     * Get CI/CD configuration by project.
     */
    public CICDConfiguration getCICDConfigByProject(Project project) {
        return cicdConfigRepository.findByProjectAndIsActive(project, true).orElse(null);
    }
    
    /**
     * Update existing CI/CD configuration.
     */
    @Transactional
    public CICDConfiguration updateCICDPipeline(Project project, CICDConfigDTO configDTO) {
        // Deactivate existing configuration
        cicdConfigRepository.findByProjectAndIsActive(project, true)
                .ifPresent(config -> {
                    config.setIsActive(false);
                    cicdConfigRepository.save(config);
                });
        
        // Create new configuration
        return generateCICDPipeline(project, configDTO);
    }
    
    /**
     * Delete CI/CD configuration.
     */
    @Transactional
    public void deleteCICDPipeline(Project project) {
        cicdConfigRepository.findByProjectAndIsActive(project, true)
                .ifPresent(config -> {
                    config.setIsActive(false);
                    cicdConfigRepository.save(config);
                    logger.info("Deactivated CI/CD configuration for project: {}", project.getName());
                });
    }
}

import * as vscode from 'vscode';
import { ExtensionConfig } from './config';
import { GalacticoAuthService } from './galacticoAuthService';

export interface CICDConfig {
    projectName: string;
    projectType: string;
    testCommand?: string;
    buildCommand?: string;
    deployStrategy: string;
}

export interface CICDStatus {
    configId: number;
    projectType: string;
    deployStrategy: string;
    isActive: boolean;
    lastPipelineStatus?: string;
    lastPipelineRun?: string;
    pipelineRunCount: number;
    githubPipelinePath?: string;
    localPipelinePath?: string;
}

/**
 * Service for managing CI/CD pipeline generation from VS Code extension.
 */
export class CICDService {
    private readonly GALACTICO_BASE_URL = ExtensionConfig.GALACTICO_BASE_URL;

    constructor(
        private context: vscode.ExtensionContext,
        private galacticoAuth: GalacticoAuthService
    ) {}

    /**
     * Show CI/CD configuration quick pick for a project.
     */
    public async configureCICD(): Promise<void> {
        try {
            // First, let user select a project
            const projects = await this.getProjects();
            if (!projects || projects.length === 0) {
                vscode.window.showWarningMessage('No projects found. Please create a project first.');
                return;
            }

            const selectedProject = await vscode.window.showQuickPick(
                projects.map(project => ({
                    label: project.name,
                    description: project.description || 'No description',
                    detail: `Team: ${project.team?.name || 'No team'}`,
                    project: project
                })),
                {
                    placeHolder: 'Select a project to configure CI/CD',
                    matchOnDescription: true,
                    matchOnDetail: true
                }
            );

            if (!selectedProject) {
                return;
            }

            // Check if CI/CD is already configured
            const existingConfig = await this.getCICDStatus(selectedProject.project.id);
            
            if (existingConfig) {
                const action = await vscode.window.showQuickPick([
                    {
                        label: 'Update Configuration',
                        description: 'Modify existing CI/CD pipeline',
                        action: 'update'
                    },
                    {
                        label: 'View Status',
                        description: 'Check current pipeline status',
                        action: 'status'
                    },
                    {
                        label: 'Delete Configuration',
                        description: 'Remove CI/CD pipeline',
                        action: 'delete'
                    }
                ], {
                    placeHolder: 'CI/CD pipeline already exists. What would you like to do?'
                });

                if (!action) {
                    return;
                }

                switch (action.action) {
                    case 'update':
                        await this.showCICDConfigurationForm(selectedProject.project, existingConfig);
                        break;
                    case 'status':
                        await this.showCICDStatus(existingConfig);
                        break;
                    case 'delete':
                        await this.deleteCICDConfiguration(selectedProject.project.id);
                        break;
                }
            } else {
                // Show configuration form for new CI/CD setup
                await this.showCICDConfigurationForm(selectedProject.project);
            }

        } catch (error) {
            console.error('Error configuring CI/CD:', error);
            vscode.window.showErrorMessage(`Failed to configure CI/CD: ${error}`);
        }
    }

    /**
     * Show CI/CD configuration form.
     */
    private async showCICDConfigurationForm(project: any, existingConfig?: CICDStatus): Promise<void> {
        const config: CICDConfig = {
            projectName: project.name,
            projectType: existingConfig?.projectType || '',
            testCommand: '',
            buildCommand: '',
            deployStrategy: existingConfig?.deployStrategy || ''
        };

        // Project Type selection
        const projectTypeOptions = [
            { label: 'Node.js', value: 'node', description: 'JavaScript/TypeScript with npm' },
            { label: 'Python', value: 'python', description: 'Python with pip/conda' },
            { label: 'React', value: 'react', description: 'React application with npm' },
            { label: 'Docker', value: 'docker', description: 'Containerized application' },
            { label: 'Java', value: 'java', description: 'Java application with Maven' },
            { label: 'Custom', value: 'custom', description: 'Custom project type' }
        ];

        const selectedProjectType = await vscode.window.showQuickPick(projectTypeOptions, {
            placeHolder: 'Select project type',
            matchOnDescription: true
        });

        if (!selectedProjectType) {
            return;
        }
        config.projectType = selectedProjectType.value;

        // Deploy Strategy selection
        const deployOptions = [
            { label: 'None (Test & Build only)', value: 'none', description: 'Only run tests and build' },
            { label: 'Staging Environment', value: 'staging', description: 'Deploy to staging server' },
            { label: 'Production', value: 'production', description: 'Deploy to production server' },
            { label: 'Docker Registry', value: 'docker', description: 'Push to Docker Hub/registry' },
            { label: 'AWS Lambda', value: 'aws-lambda', description: 'Deploy to AWS Lambda' }
        ];

        const selectedDeployStrategy = await vscode.window.showQuickPick(deployOptions, {
            placeHolder: 'Select deployment strategy',
            matchOnDescription: true
        });

        if (!selectedDeployStrategy) {
            return;
        }
        config.deployStrategy = selectedDeployStrategy.value;

        // Optional: Test Command
        const testCommand = await vscode.window.showInputBox({
            prompt: 'Enter test command (optional)',
            placeHolder: this.getDefaultTestCommand(config.projectType),
            value: ''
        });
        config.testCommand = testCommand || '';

        // Optional: Build Command (if not "none" deploy strategy)
        if (config.deployStrategy !== 'none') {
            const buildCommand = await vscode.window.showInputBox({
                prompt: 'Enter build command (optional)',
                placeHolder: this.getDefaultBuildCommand(config.projectType),
                value: ''
            });
            config.buildCommand = buildCommand || '';
        }

        // Confirm configuration
        const confirmMessage = `
Project: ${config.projectName}
Type: ${config.projectType}
Deploy Strategy: ${config.deployStrategy}
Test Command: ${config.testCommand || 'Default'}
Build Command: ${config.buildCommand || 'Default'}

Generate CI/CD pipeline?`;

        const confirmation = await vscode.window.showInformationMessage(
            confirmMessage,
            { modal: true },
            'Generate Pipeline',
            'Cancel'
        );

        if (confirmation === 'Generate Pipeline') {
            await this.createCICDPipeline(project.id, config);
        }
    }

    /**
     * Create CI/CD pipeline via API.
     */
    private async createCICDPipeline(projectId: number, config: CICDConfig): Promise<void> {
        try {
            vscode.window.withProgress({
                location: vscode.ProgressLocation.Notification,
                title: "Generating CI/CD Pipeline...",
                cancellable: false
            }, async (progress) => {
                progress.report({ increment: 0, message: "Sending configuration..." });

                const response = await this.galacticoAuth.makeAuthenticatedRequest(
                    `/projects/${projectId}/cicd/api`,
                    {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify(config)
                    }
                );

                progress.report({ increment: 50, message: "Processing..." });

                if (!response.ok) {
                    const errorData = await response.json() as any;
                    throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
                }

                const result = await response.json() as any;
                progress.report({ increment: 100, message: "Complete!" });

                if (result.success) {
                    const message = `CI/CD pipeline generated successfully!\n\n` +
                                  `Project Type: ${result.projectType}\n` +
                                  `Deploy Strategy: ${result.deployStrategy}\n` +
                                  `GitHub Workflow: ${result.githubPipelinePath || 'Not pushed to GitHub'}\n` +
                                  `Local Files: ${result.localPipelinePath || 'Not saved locally'}`;

                    const action = await vscode.window.showInformationMessage(
                        message,
                        'View in Browser',
                        'OK'
                    );

                    if (action === 'View in Browser') {
                        const dashboardUrl = `${this.GALACTICO_BASE_URL}/projects/${projectId}/cicd`;
                        await vscode.env.openExternal(vscode.Uri.parse(dashboardUrl));
                    }
                } else {
                    throw new Error(result.message || 'Unknown error occurred');
                }
            });

        } catch (error) {
            console.error('Error creating CI/CD pipeline:', error);
            vscode.window.showErrorMessage(`Failed to create CI/CD pipeline: ${error}`);
        }
    }

    /**
     * Get CI/CD status for a project.
     */
    private async getCICDStatus(projectId: number): Promise<CICDStatus | null> {
        try {
            const response = await this.galacticoAuth.makeAuthenticatedRequest(`/projects/${projectId}/cicd/api`);
            
            if (response.status === 404) {
                return null; // No CI/CD configuration found
            }
            
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const result = await response.json() as any;
            return result.success ? result as CICDStatus : null;

        } catch (error) {
            console.error('Error getting CI/CD status:', error);
            return null;
        }
    }

    /**
     * Show CI/CD status information.
     */
    private async showCICDStatus(config: CICDStatus): Promise<void> {
        const statusEmoji = this.getStatusEmoji(config.lastPipelineStatus);
        const lastRun = config.lastPipelineRun ? new Date(config.lastPipelineRun).toLocaleString() : 'Never';
        
        const statusMessage = `
CI/CD Pipeline Status

${statusEmoji} Status: ${config.lastPipelineStatus || 'Never Run'}
🏗️ Project Type: ${config.projectType}
🚀 Deploy Strategy: ${config.deployStrategy}
📊 Pipeline Runs: ${config.pipelineRunCount}
⏰ Last Run: ${lastRun}
📁 GitHub Workflow: ${config.githubPipelinePath ? 'Available' : 'Not available'}`;

        const actions = ['View in Browser', 'Refresh Status'];
        if (config.githubPipelinePath) {
            actions.unshift('Open GitHub Workflow');
        }

        const action = await vscode.window.showInformationMessage(
            statusMessage,
            ...actions
        );

        switch (action) {
            case 'Open GitHub Workflow':
                if (config.githubPipelinePath) {
                    await vscode.env.openExternal(vscode.Uri.parse(config.githubPipelinePath));
                }
                break;
            case 'View in Browser':
                const dashboardUrl = `${this.GALACTICO_BASE_URL}/projects/${config.configId}/cicd`;
                await vscode.env.openExternal(vscode.Uri.parse(dashboardUrl));
                break;
            case 'Refresh Status':
                // Refresh and show again
                const refreshedConfig = await this.getCICDStatus(config.configId);
                if (refreshedConfig) {
                    await this.showCICDStatus(refreshedConfig);
                }
                break;
        }
    }

    /**
     * Delete CI/CD configuration.
     */
    private async deleteCICDConfiguration(projectId: number): Promise<void> {
        const confirmation = await vscode.window.showWarningMessage(
            'Are you sure you want to delete the CI/CD pipeline configuration?\n\nNote: This will not delete the actual GitHub workflow files.',
            { modal: true },
            'Delete',
            'Cancel'
        );

        if (confirmation !== 'Delete') {
            return;
        }

        try {
            const response = await this.galacticoAuth.makeAuthenticatedRequest(
                `/projects/${projectId}/cicd/delete`,
                {
                    method: 'POST'
                }
            );

            if (response.ok) {
                vscode.window.showInformationMessage('CI/CD pipeline configuration deleted successfully.');
            } else {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

        } catch (error) {
            console.error('Error deleting CI/CD configuration:', error);
            vscode.window.showErrorMessage(`Failed to delete CI/CD configuration: ${error}`);
        }
    }

    /**
     * Get projects from API.
     */
    private async getProjects(): Promise<any[]> {
        try {
            const response = await this.galacticoAuth.makeAuthenticatedRequest('/api/user/projects');
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return await response.json() as any[];
        } catch (error) {
            console.error('Error fetching projects:', error);
            return [];
        }
    }

    /**
     * Get default test command for project type.
     */
    private getDefaultTestCommand(projectType: string): string {
        const commands: { [key: string]: string } = {
            'node': 'npm test',
            'python': 'pytest',
            'react': 'npm test',
            'docker': 'docker run --rm test-image npm test',
            'java': 'mvn clean test',
            'custom': 'No default'
        };
        return commands[projectType] || 'No default';
    }

    /**
     * Get default build command for project type.
     */
    private getDefaultBuildCommand(projectType: string): string {
        const commands: { [key: string]: string } = {
            'node': 'npm run build',
            'python': 'python setup.py build',
            'react': 'npm run build',
            'docker': 'docker build -t app .',
            'java': 'mvn clean package -DskipTests',
            'custom': 'No default'
        };
        return commands[projectType] || 'No default';
    }

    /**
     * Get status emoji for pipeline status.
     */
    private getStatusEmoji(status?: string): string {
        switch (status) {
            case 'success': return '✅';
            case 'failure': return '❌';
            case 'running': return '🔄';
            default: return '⚪';
        }
    }
}

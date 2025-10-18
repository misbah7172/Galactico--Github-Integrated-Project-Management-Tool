import * as vscode from 'vscode';
import { GitHubAuthService } from './githubAuth';
import { ExtensionConfig } from './config';

/**
 * Interface for task information
 */
export interface TaskInfo {
    id: string;
    title: string;
    description: string;
    projectId: string;
    projectName: string;
    status: string;
}

/**
 * Interface for commit data to send to Galactico backend
 */
export interface CommitData {
    username: string;
    commitMessage: string;
    branch: string;
    taskId: string;
    commitTime: string;
    commitUrl: string;
    commitSha: string;
    projectId: string;
}

/**
 * Service for Galactico-specific Git automation features
 */
export class GalacticoService {
    private readonly GALACTICO_BASE_URL = ExtensionConfig.getBaseUrl();
    private selectedTask: TaskInfo | null = null;

    constructor(private githubAuth: GitHubAuthService) {}

    /**
     * Set the selected task for commits
     */
    public setSelectedTask(task: TaskInfo): void {
        this.selectedTask = task;
        vscode.window.showInformationMessage(`Selected task: ${task.title}`);
    }

    /**
     * Get the currently selected task
     */
    public getSelectedTask(): TaskInfo | null {
        return this.selectedTask;
    }

    /**
     * Show task selection quick pick
     */
    public async selectTask(): Promise<TaskInfo | null> {
        try {
            // Fetch tasks from Galactico API
            const response = await fetch(`${this.GALACTICO_BASE_URL}/api/tasks/my-tasks`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`Failed to fetch tasks: ${response.status}`);
            }

            const tasks = await response.json() as TaskInfo[];

            if (!tasks || tasks.length === 0) {
                vscode.window.showInformationMessage('No tasks found. Please create tasks in your Galactico dashboard first.');
                return null;
            }

            const selectedItem = await vscode.window.showQuickPick(
                tasks.map(task => ({
                    label: task.id,
                    description: task.title,
                    detail: `Project: ${task.projectName} | Status: ${task.status}`,
                    task: task
                })),
                {
                    placeHolder: 'Select a task for your commit',
                    matchOnDescription: true,
                    matchOnDetail: true
                }
            );

            if (selectedItem) {
                this.setSelectedTask(selectedItem.task);
                return selectedItem.task;
            }

            return null;

        } catch (error) {
            vscode.window.showErrorMessage(`Failed to load tasks: ${error}. Please check your Galactico connection.`);
            return null;
        }
    }

    /**
     * Create a formatted commit message based on task
     */
    public formatCommitMessage(userMessage: string, task: TaskInfo): string {
        return `[${task.id}] ${userMessage}

Task: ${task.title}
Project: ${task.projectName}
Status: ${task.status}

Description: ${task.description}`;
    }

    /**
     * Perform Galactico commit with task tracking
     */
    public async performGalacticoCommit(): Promise<boolean> {
        try {
            // Ensure user is authenticated
            const userInfo = await this.githubAuth.getUserInfoIfAuthenticated();
            if (!userInfo) {
                vscode.window.showErrorMessage('Please authenticate with GitHub first.');
                return false;
            }

            // Check if task is selected
            if (!this.selectedTask) {
                const task = await this.selectTask();
                if (!task) {
                    vscode.window.showWarningMessage('No task selected. Commit cancelled.');
                    return false;
                }
            }

            // Get commit message from user
            const userMessage = await vscode.window.showInputBox({
                prompt: `Enter commit message for task ${this.selectedTask!.id}`,
                placeHolder: 'Brief description of changes made',
                validateInput: (value) => {
                    if (!value || value.trim().length === 0) {
                        return 'Commit message cannot be empty';
                    }
                    if (value.length > 100) {
                        return 'Commit message should be under 100 characters';
                    }
                    return null;
                }
            });

            if (!userMessage) {
                vscode.window.showWarningMessage('Commit cancelled - no message provided.');
                return false;
            }

            // Format the commit message
            const formattedMessage = this.formatCommitMessage(userMessage, this.selectedTask!);

            // Perform git operations
            const success = await this.executeGalacticoCommitWorkflow(
                formattedMessage,
                userInfo.login,
                this.selectedTask!
            );

            if (success) {
                vscode.window.showInformationMessage(
                    `Successfully committed and pushed changes for task ${this.selectedTask!.id}!`
                );
                // Clear selected task after successful commit
                this.selectedTask = null;
            }

            return success;

        } catch (error) {
            const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
            vscode.window.showErrorMessage(`Galactico commit failed: ${errorMessage}`);
            console.error('Galactico commit error:', error);
            return false;
        }
    }

    /**
     * Execute the complete Git workflow for Galactico
     */
    private async executeGalacticoCommitWorkflow(
        commitMessage: string, 
        username: string, 
        task: TaskInfo
    ): Promise<boolean> {
        try {
            const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
            if (!workspaceFolder) {
                throw new Error('No workspace folder found');
            }

            // Import the exec functionality
            const { exec } = await import('child_process');
            const { promisify } = await import('util');
            const execAsync = promisify(exec);

            const execOptions = { 
                cwd: workspaceFolder.uri.fsPath,
                timeout: 30000
            };

            // Step 1: Add all changes
            await execAsync('git add .', execOptions);
            console.log('Git add completed');

            // Step 2: Commit with formatted message
            await execAsync(`git commit -m "${commitMessage.replace(/"/g, '\\"')}"`, execOptions);
            console.log('Git commit completed');

            // Step 3: Get current branch
            const { stdout: branchOutput } = await execAsync('git branch --show-current', execOptions);
            const currentBranch = branchOutput.trim();

            // Step 4: Get commit SHA
            const { stdout: shaOutput } = await execAsync('git rev-parse HEAD', execOptions);
            const commitSha = shaOutput.trim();

            // Step 5: Get remote URL for commit URL
            const { stdout: remoteOutput } = await execAsync('git remote get-url origin', execOptions);
            const remoteUrl = remoteOutput.trim();
            const commitUrl = this.buildCommitUrl(remoteUrl, commitSha);

            // Step 6: Push to remote
            await execAsync(`git push origin ${currentBranch}`, execOptions);
            console.log('Git push completed');

            // Step 7: Send commit data to Galactico backend
            await this.sendCommitToGalactico({
                username: username,
                commitMessage: commitMessage,
                branch: currentBranch,
                taskId: task.id,
                commitTime: new Date().toISOString(),
                commitUrl: commitUrl,
                commitSha: commitSha,
                projectId: task.projectId
            });

            return true;

        } catch (error) {
            console.error('Git workflow error:', error);
            throw error;
        }
    }

    /**
     * Build GitHub commit URL from remote URL and commit SHA
     */
    private buildCommitUrl(remoteUrl: string, commitSha: string): string {
        try {
            // Convert SSH URL to HTTPS if needed
            let httpsUrl = remoteUrl;
            if (remoteUrl.startsWith('git@github.com:')) {
                httpsUrl = remoteUrl.replace('git@github.com:', 'https://github.com/');
            }
            
            // Remove .git suffix if present
            if (httpsUrl.endsWith('.git')) {
                httpsUrl = httpsUrl.slice(0, -4);
            }

            return `${httpsUrl}/commit/${commitSha}`;
        } catch (error) {
            console.error('Error building commit URL:', error);
            return `https://github.com/commit/${commitSha}`;
        }
    }

    /**
     * Send commit data to Galactico backend webhook
     */
    private async sendCommitToGalactico(commitData: CommitData): Promise<void> {
        try {
            const response = await fetch(`${this.GALACTICO_BASE_URL}/webhook/commit`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(commitData)
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const result = await response.json();
            console.log('Successfully sent commit to Galactico:', result);

        } catch (error) {
            console.error('Failed to send commit to Galactico:', error);
            // Don't throw here - commit was successful even if webhook failed
            vscode.window.showWarningMessage(
                'Commit successful, but failed to notify Galactico. Please check your connection.'
            );
        }
    }

    /**
     * View commit status from Galactico
     */
    public async viewCommitStatus(): Promise<void> {
        try {
            const userInfo = await this.githubAuth.getUserInfoIfAuthenticated();
            if (!userInfo) {
                vscode.window.showErrorMessage('Please authenticate with GitHub first.');
                return;
            }

            // Open Galactico dashboard in VS Code's simple browser
            const dashboardUrl = `${this.GALACTICO_BASE_URL}/dashboard`;
            await vscode.env.openExternal(vscode.Uri.parse(dashboardUrl));

        } catch (error) {
            console.error('Failed to open commit status:', error);
            vscode.window.showErrorMessage('Failed to open Galactico dashboard.');
        }
    }
}

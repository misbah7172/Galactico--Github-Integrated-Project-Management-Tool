import * as vscode from 'vscode';
import { GitHubAuthService } from './githubAuth';
import { ExtensionConfig } from './config';

/**
 * Interface for sprint information
 */
export interface SprintInfo {
    id: string;
    name: string;
    status: 'UPCOMING' | 'ACTIVE' | 'COMPLETED';
    startDate: string;
    endDate: string;
    projectId: string;
}

/**
 * Interface for backlog item information
 */
export interface BacklogItemInfo {
    id: string;
    title: string;
    description: string;
    priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
    storyPoints: number;
    projectId: string;
}

/**
 * Interface for commit template information
 */
export interface CommitTemplate {
    featureCode: string;
    taskTitle: string;
    assignee?: string;
    sprintId?: string;
    backlogPriority?: string;
    storyPoints?: number;
    timeEstimate?: string;
    taskType?: string;
    status?: string;
    tags?: string[];
}

/**
 * Service for automated sprint and backlog management in VS Code Extension
 */
export class AutoTrackCommitService {
    private readonly BASE_URL = ExtensionConfig.getBaseUrl();
    private currentProject: any = null;

    constructor(private githubAuth: GitHubAuthService) {}

    /**
     * Show interactive commit creator with sprint/backlog options
     */
    public async createAutoTrackCommit(): Promise<void> {
        try {
            // Step 1: Get feature code
            const featureCode = await vscode.window.showInputBox({
                prompt: 'Enter Feature Code (e.g., Feature123 or F123)',
                placeHolder: 'Feature123',
                validateInput: (value) => {
                    if (!value || !value.match(/^(Feature|F)\d+$/i)) {
                        return 'Feature code must be in format: Feature123 or F123';
                    }
                    return null;
                }
            });

            if (!featureCode) return;

            // Step 2: Get task title
            const taskTitle = await vscode.window.showInputBox({
                prompt: 'Enter task title',
                placeHolder: 'Implement user authentication system'
            });

            if (!taskTitle) return;

            // Step 3: Choose creation type
            const creationType = await vscode.window.showQuickPick([
                { label: '🎯 Create Task Only', value: 'task' },
                { label: '🚀 Add to Sprint', value: 'sprint' },
                { label: '📋 Add to Backlog', value: 'backlog' },
                { label: '⚙️ Advanced Options', value: 'advanced' }
            ], {
                placeHolder: 'How would you like to create this task?'
            });

            if (!creationType) return;

            // Build commit template based on selection
            const template: CommitTemplate = {
                featureCode,
                taskTitle
            };

            // Handle different creation types
            switch (creationType.value) {
                case 'task':
                    await this.handleSimpleTask(template);
                    break;
                case 'sprint':
                    await this.handleSprintAssignment(template);
                    break;
                case 'backlog':
                    await this.handleBacklogAssignment(template);
                    break;
                case 'advanced':
                    await this.handleAdvancedOptions(template);
                    break;
            }

        } catch (error) {
            vscode.window.showErrorMessage(`Error creating AutoTrack commit: ${error}`);
        }
    }

    /**
     * Handle simple task creation
     */
    private async handleSimpleTask(template: CommitTemplate): Promise<void> {
        // Get assignee
        const assignee = await this.getAssignee();
        if (assignee) template.assignee = assignee;

        // Get status
        const status = await this.getTaskStatus();
        if (status) template.status = status;

        await this.generateAndCommit(template);
    }

    /**
     * Handle sprint assignment
     */
    private async handleSprintAssignment(template: CommitTemplate): Promise<void> {
        // Get available sprints
        const sprintOption = await vscode.window.showQuickPick([
            { label: '🆕 Create New Sprint', value: 'new' },
            { label: '⏳ Current Sprint', value: 'current' },
            { label: '➡️ Next Sprint', value: 'next' },
            { label: '🔢 Specific Sprint Number', value: 'specific' }
        ], {
            placeHolder: 'Choose sprint assignment option'
        });

        if (!sprintOption) return;

        switch (sprintOption.value) {
            case 'new':
                const sprintNumber = await vscode.window.showInputBox({
                    prompt: 'Enter new sprint number',
                    placeHolder: '1'
                });
                if (sprintNumber) template.sprintId = sprintNumber;
                break;
            case 'current':
                template.sprintId = 'current';
                break;
            case 'next':
                template.sprintId = 'next';
                break;
            case 'specific':
                const specificNumber = await vscode.window.showInputBox({
                    prompt: 'Enter sprint number',
                    placeHolder: '2'
                });
                if (specificNumber) template.sprintId = specificNumber;
                break;
        }

        // Get additional details
        await this.addOptionalDetails(template);
        await this.generateAndCommit(template);
    }

    /**
     * Handle backlog assignment
     */
    private async handleBacklogAssignment(template: CommitTemplate): Promise<void> {
        // Get backlog priority
        const priority = await vscode.window.showQuickPick([
            { label: '🔴 Critical', value: 'critical' },
            { label: '🟠 High', value: 'high' },
            { label: '🟡 Medium', value: 'medium' },
            { label: '🟢 Low', value: 'low' }
        ], {
            placeHolder: 'Select backlog priority'
        });

        if (priority) {
            template.backlogPriority = priority.value;
        }

        // Get additional details
        await this.addOptionalDetails(template);
        await this.generateAndCommit(template);
    }

    /**
     * Handle advanced options
     */
    private async handleAdvancedOptions(template: CommitTemplate): Promise<void> {
        // Get assignee
        const assignee = await this.getAssignee();
        if (assignee) template.assignee = assignee;

        // Get sprint assignment
        const sprintId = await vscode.window.showInputBox({
            prompt: 'Sprint assignment (optional)',
            placeHolder: 'sprint1, sprintcurrent, sprintnext, or leave empty'
        });
        if (sprintId) template.sprintId = sprintId;

        // Get backlog priority (if not assigned to sprint)
        if (!sprintId) {
            const backlogPriority = await vscode.window.showQuickPick([
                { label: 'None', value: '' },
                { label: 'Critical', value: 'critical' },
                { label: 'High', value: 'high' },
                { label: 'Medium', value: 'medium' },
                { label: 'Low', value: 'low' }
            ], {
                placeHolder: 'Backlog priority (optional)'
            });
            if (backlogPriority && backlogPriority.value) {
                template.backlogPriority = backlogPriority.value;
            }
        }

        // Get story points
        const storyPoints = await vscode.window.showInputBox({
            prompt: 'Story points (optional)',
            placeHolder: '1, 2, 3, 5, 8, 13, 21',
            validateInput: (value) => {
                if (value && !value.match(/^[0-9]+$/)) {
                    return 'Story points must be a number';
                }
                return null;
            }
        });
        if (storyPoints) template.storyPoints = parseInt(storyPoints);

        // Get time estimate
        const timeEstimate = await vscode.window.showInputBox({
            prompt: 'Time estimate (optional)',
            placeHolder: '4h, 2d, 1w'
        });
        if (timeEstimate) template.timeEstimate = timeEstimate;

        // Get task type
        const taskType = await vscode.window.showQuickPick([
            { label: 'Story', value: 'story' },
            { label: 'Bug', value: 'bug' },
            { label: 'Epic', value: 'epic' },
            { label: 'Task', value: 'task' },
            { label: 'Subtask', value: 'subtask' }
        ], {
            placeHolder: 'Task type (optional)'
        });
        if (taskType) template.taskType = taskType.value;

        // Get status
        const status = await this.getTaskStatus();
        if (status) template.status = status;

        // Get tags
        const tags = await vscode.window.showInputBox({
            prompt: 'Tags (optional, comma-separated)',
            placeHolder: 'frontend, authentication, security'
        });
        if (tags) {
            template.tags = tags.split(',').map(tag => tag.trim()).filter(tag => tag.length > 0);
        }

        await this.generateAndCommit(template);
    }

    /**
     * Get assignee from user input
     */
    private async getAssignee(): Promise<string | undefined> {
        return await vscode.window.showInputBox({
            prompt: 'Assignee username (optional)',
            placeHolder: 'misbah7172'
        });
    }

    /**
     * Get task status
     */
    private async getTaskStatus(): Promise<string | undefined> {
        const status = await vscode.window.showQuickPick([
            { label: '📋 TODO', value: 'todo' },
            { label: '🔄 In Progress', value: 'in-progress' },
            { label: '👀 Review', value: 'review' },
            { label: '✅ Done', value: 'done' }
        ], {
            placeHolder: 'Task status (optional)'
        });
        return status?.value;
    }

    /**
     * Add optional details to template
     */
    private async addOptionalDetails(template: CommitTemplate): Promise<void> {
        // Get assignee
        const assignee = await this.getAssignee();
        if (assignee) template.assignee = assignee;

        // Get story points
        const storyPoints = await vscode.window.showInputBox({
            prompt: 'Story points (optional)',
            placeHolder: '1, 2, 3, 5, 8, 13',
            validateInput: (value) => {
                if (value && !value.match(/^[0-9]+$/)) {
                    return 'Story points must be a number';
                }
                return null;
            }
        });
        if (storyPoints) template.storyPoints = parseInt(storyPoints);

        // Get task type
        const taskType = await vscode.window.showQuickPick([
            { label: 'Story', value: 'story' },
            { label: 'Bug', value: 'bug' },
            { label: 'Task', value: 'task' }
        ], {
            placeHolder: 'Task type (optional)'
        });
        if (taskType) template.taskType = taskType.value;

        // Get status
        const status = await this.getTaskStatus();
        if (status) template.status = status;
    }

    /**
     * Generate commit message from template and perform commit
     */
    private async generateAndCommit(template: CommitTemplate): Promise<void> {
        const commitMessage = this.buildCommitMessage(template);

        // Show preview
        const proceed = await vscode.window.showInformationMessage(
            `Generated commit message:\n\n${commitMessage}\n\nProceed with commit?`,
            'Commit', 'Edit', 'Cancel'
        );

        if (proceed === 'Edit') {
            const editedMessage = await vscode.window.showInputBox({
                prompt: 'Edit commit message',
                value: commitMessage
            });
            if (editedMessage) {
                await this.performCommit(editedMessage);
            }
        } else if (proceed === 'Commit') {
            await this.performCommit(commitMessage);
        }
    }

    /**
     * Build commit message from template
     */
    private buildCommitMessage(template: CommitTemplate): string {
        let message = `${template.featureCode}: ${template.taskTitle}`;

        // Add assignee
        if (template.assignee) {
            message += ` -> ${template.assignee}`;
        }

        // Add sprint assignment
        if (template.sprintId) {
            if (template.sprintId.match(/^\d+$/)) {
                message += ` -> sprint${template.sprintId}`;
            } else {
                message += ` -> sprint${template.sprintId}`;
            }
        }

        // Add backlog priority
        if (template.backlogPriority) {
            message += ` -> backlog-${template.backlogPriority}`;
        }

        // Add story points
        if (template.storyPoints) {
            message += ` -> sp:${template.storyPoints}`;
        }

        // Add time estimate
        if (template.timeEstimate) {
            message += ` -> estimate:${template.timeEstimate}`;
        }

        // Add task type
        if (template.taskType) {
            message += ` -> ${template.taskType}`;
        }

        // Add status (must be at the end)
        if (template.status) {
            message += ` -> ${template.status}`;
        }

        // Add tags
        if (template.tags && template.tags.length > 0) {
            message += ' ' + template.tags.map(tag => `#${tag}`).join(' ');
        }

        return message;
    }

    /**
     * Perform the actual Git commit
     */
    private async performCommit(commitMessage: string): Promise<void> {
        try {
            const { exec } = require('child_process');
            const { promisify } = require('util');
            const execAsync = promisify(exec);

            const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
            if (!workspaceFolder) {
                vscode.window.showErrorMessage('No workspace folder found');
                return;
            }

            // Add all changes
            await execAsync('git add .', { cwd: workspaceFolder.uri.fsPath });

            // Commit with message
            await execAsync(`git commit -m "${commitMessage}"`, { 
                cwd: workspaceFolder.uri.fsPath 
            });

            // Ask if user wants to push
            const pushChoice = await vscode.window.showInformationMessage(
                'Commit created successfully! Push to remote?',
                'Push Now', 'Push Later'
            );

            if (pushChoice === 'Push Now') {
                await execAsync('git push', { cwd: workspaceFolder.uri.fsPath });
                vscode.window.showInformationMessage('Changes pushed successfully!');
            }

            vscode.window.showInformationMessage('AutoTrack commit created successfully!');

        } catch (error) {
            vscode.window.showErrorMessage(`Commit failed: ${error}`);
        }
    }

    /**
     * Quick sprint assignment helper
     */
    public async quickSprintAssignment(): Promise<void> {
        const sprintNumber = await vscode.window.showInputBox({
            prompt: 'Enter sprint number to assign current changes',
            placeHolder: '1'
        });

        if (!sprintNumber) return;

        const featureCode = await vscode.window.showInputBox({
            prompt: 'Enter Feature Code',
            placeHolder: 'Feature123'
        });

        if (!featureCode) return;

        const taskTitle = await vscode.window.showInputBox({
            prompt: 'Enter task title',
            placeHolder: 'Complete feature implementation'
        });

        if (!taskTitle) return;

        const template: CommitTemplate = {
            featureCode,
            taskTitle,
            sprintId: sprintNumber,
            status: 'done'
        };

        await this.generateAndCommit(template);
    }

    /**
     * Quick backlog addition helper
     */
    public async quickBacklogAddition(): Promise<void> {
        const priority = await vscode.window.showQuickPick([
            { label: '🔴 Critical', value: 'critical' },
            { label: '🟠 High', value: 'high' },
            { label: '🟡 Medium', value: 'medium' },
            { label: '🟢 Low', value: 'low' }
        ], {
            placeHolder: 'Select priority for backlog item'
        });

        if (!priority) return;

        const featureCode = await vscode.window.showInputBox({
            prompt: 'Enter Feature Code',
            placeHolder: 'Feature123'
        });

        if (!featureCode) return;

        const taskTitle = await vscode.window.showInputBox({
            prompt: 'Enter task title',
            placeHolder: 'New feature request'
        });

        if (!taskTitle) return;

        const template: CommitTemplate = {
            featureCode,
            taskTitle,
            backlogPriority: priority.value
        };

        await this.generateAndCommit(template);
    }
}
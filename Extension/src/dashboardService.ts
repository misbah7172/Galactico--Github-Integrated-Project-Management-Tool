import * as vscode from 'vscode';
import { GalacticoAuthService } from './galacticoAuthService';
import { ExtensionConfig } from './config';

/**
 * Interface for task data from backend
 */
export interface Task {
    id: string;
    title: string;
    description: string;
    projectName: string;
    assignedDate: string;
    deadline: string;
    status: 'ASSIGNED' | 'IN_PROGRESS' | 'COMPLETED' | 'OVERDUE';
    taskUrl: string;
    commits: TaskCommit[];
    branchName?: string;
}

/**
 * Interface for commit data related to tasks
 */
export interface TaskCommit {
    id: string;
    sha: string;
    message: string;
    author: string;
    date: string;
    status: 'APPROVED' | 'PENDING' | 'REJECTED';
    commitUrl: string;
}

/**
 * Interface for user contributions data
 */
export interface UserContributions {
    totalTasks: number;
    completedTasks: number;
    pendingTasks: number;
    overdueTasks: number;
    totalCommits: number;
    approvedCommits: number;
    pendingCommits: number;
    rejectedCommits: number;
}

/**
 * Dashboard service for managing developer task dashboard
 */
export class DashboardService {
    private readonly GALACTICO_BASE_URL = ExtensionConfig.GALACTICO_BASE_URL;
    private panel: vscode.WebviewPanel | undefined;
    private tasks: Task[] = [];
    private contributions: UserContributions | null = null;
    private autoRefreshTimer: NodeJS.Timeout | undefined;

    constructor(private context: vscode.ExtensionContext, private galacticoAuth: GalacticoAuthService) {}

    /**
     * Open the task dashboard webview
     */
    public async openTaskDashboard(): Promise<void> {
        try {
            // Check if user is authenticated
            const userInfo = await this.galacticoAuth.getUserInfoIfAuthenticated();
            if (!userInfo) {
                const authResult = await vscode.window.showInformationMessage(
                    'Please authenticate with Galactico to access the dashboard.',
                    'Authenticate',
                    'Cancel'
                );
                
                if (authResult === 'Authenticate') {
                    const authenticatedUser = await this.galacticoAuth.authenticate();
                    if (!authenticatedUser) {
                        return; // Authentication failed or was cancelled
                    }
                } else {
                    return; // User cancelled
                }
            }

            // Create or show existing panel
            if (this.panel) {
                this.panel.reveal(vscode.ViewColumn.One);
                return;
            }

            this.panel = vscode.window.createWebviewPanel(
                'galacticoTaskDashboard',
                'Galactico Task Dashboard',
                vscode.ViewColumn.One,
                {
                    enableScripts: true,
                    retainContextWhenHidden: true,
                    localResourceRoots: [
                        vscode.Uri.joinPath(this.context.extensionUri, 'media'),
                        vscode.Uri.joinPath(this.context.extensionUri, 'out')
                    ]
                }
            );

            // Set up message handling
            this.panel.webview.onDidReceiveMessage(
                message => this.handleWebviewMessage(message),
                undefined,
                this.context.subscriptions
            );

            // Handle panel disposal
            this.panel.onDidDispose(
                () => {
                    this.panel = undefined;
                    this.stopAutoRefresh();
                },
                null,
                this.context.subscriptions
            );

            // Load and display tasks
            await this.syncTasks();
            this.updateWebviewContent();
            this.startAutoRefresh();

        } catch (error) {
            vscode.window.showErrorMessage(`Failed to open dashboard: ${error}`);
        }
    }

    /**
     * Sync tasks from the backend API
     */
    public async syncTasks(): Promise<void> {
        try {
            const userInfo = await this.galacticoAuth.getUserInfoIfAuthenticated();
            if (!userInfo) {
                throw new Error('User not authenticated with Galactico');
            }

            vscode.window.withProgress({
                location: vscode.ProgressLocation.Notification,
                title: "Syncing tasks from Galactico...",
                cancellable: false
            }, async (progress) => {
                progress.report({ increment: 0, message: "Fetching tasks..." });

                // Fetch tasks from backend using authenticated request
                const tasksResponse = await this.galacticoAuth.makeAuthenticatedRequest('/api/user/tasks');
                if (!tasksResponse.ok) {
                    throw new Error(`HTTP error! status: ${tasksResponse.status}`);
                }

                progress.report({ increment: 50, message: "Processing task data..." });

                const tasksData = await tasksResponse.json() as any[];
                this.tasks = this.processTaskData(tasksData);

                // Fetch contributions data
                const contributionsResponse = await this.galacticoAuth.makeAuthenticatedRequest('/api/user/contributions');
                if (contributionsResponse.ok) {
                    this.contributions = await contributionsResponse.json() as UserContributions;
                }

                progress.report({ increment: 100, message: "Complete!" });
            });

            // Update webview if open
            if (this.panel) {
                this.updateWebviewContent();
            }

            vscode.window.showInformationMessage(`Synced ${this.tasks.length} tasks successfully!`);

        } catch (error) {
            vscode.window.showErrorMessage(`Failed to sync tasks: ${error}`);
            console.error('Sync error:', error);
        }
    }

    /**
     * Process raw task data from backend
     */
    private processTaskData(rawData: any[]): Task[] {
        return rawData.map(task => ({
            id: task.id,
            title: task.title,
            description: task.description || '',
            projectName: task.project?.name || 'Unknown Project',
            assignedDate: task.assignedDate || task.createdAt,
            deadline: task.deadline || this.calculateDefaultDeadline(task.createdAt),
            status: this.calculateTaskStatus(task),
            taskUrl: `${this.GALACTICO_BASE_URL}/tasks/${task.id}`,
            commits: task.commits || [],
            branchName: task.featureCode ? `feature/${task.featureCode}` : undefined
        }));
    }

    /**
     * Calculate task status based on data
     */
    private calculateTaskStatus(task: any): Task['status'] {
        const now = new Date();
        const deadline = new Date(task.deadline);
        
        if (task.status === 'DONE') {
            return 'COMPLETED';
        }
        
        if (deadline < now && task.status !== 'DONE') {
            return 'OVERDUE';
        }
        
        if (task.status === 'IN_PROGRESS') {
            return 'IN_PROGRESS';
        }
        
        return 'ASSIGNED';
    }

    /**
     * Calculate default deadline (7 days from creation)
     */
    private calculateDefaultDeadline(createdAt: string): string {
        const created = new Date(createdAt);
        created.setDate(created.getDate() + 7);
        return created.toISOString();
    }

    /**
     * Handle messages from webview
     */
    private async handleWebviewMessage(message: any): Promise<void> {
        switch (message.command) {
            case 'sync':
                await this.syncTasks();
                break;
            case 'openTask':
                vscode.env.openExternal(vscode.Uri.parse(message.url));
                break;
            case 'openCommit':
                vscode.env.openExternal(vscode.Uri.parse(message.url));
                break;
            case 'filterTasks':
                this.updateWebviewContent(message.filter);
                break;
        }
    }

    /**
     * Update webview content
     */
    private updateWebviewContent(filter?: string): void {
        if (!this.panel) return;

        const filteredTasks = this.filterTasks(filter);
        const html = this.generateDashboardHTML(filteredTasks);
        this.panel.webview.html = html;
    }

    /**
     * Filter tasks based on status
     */
    private filterTasks(filter?: string): Task[] {
        if (!filter || filter === 'all') {
            return this.tasks;
        }

        return this.tasks.filter(task => {
            switch (filter) {
                case 'in-progress':
                    return task.status === 'IN_PROGRESS';
                case 'completed':
                    return task.status === 'COMPLETED';
                case 'overdue':
                    return task.status === 'OVERDUE';
                default:
                    return true;
            }
        });
    }

    /**
     * Calculate remaining time for a task
     */
    private calculateRemainingTime(deadline: string): string {
        const now = new Date();
        const deadlineDate = new Date(deadline);
        const diffMs = deadlineDate.getTime() - now.getTime();
        
        if (diffMs <= 0) {
            return 'Overdue';
        }
        
        const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));
        
        if (diffDays === 1) {
            return '1 day left';
        } else if (diffDays < 7) {
            return `${diffDays} days left`;
        } else {
            const weeks = Math.floor(diffDays / 7);
            const remainingDays = diffDays % 7;
            return remainingDays === 0 ? `${weeks} week${weeks > 1 ? 's' : ''} left` : 
                   `${weeks} week${weeks > 1 ? 's' : ''} ${remainingDays} day${remainingDays > 1 ? 's' : ''} left`;
        }
    }

    /**
     * Generate HTML for dashboard
     */
    private generateDashboardHTML(tasks: Task[]): string {
        const contributionsHtml = this.contributions ? `
            <div class="stats-overview">
                <div class="stat-card">
                    <div class="stat-number">${this.contributions.totalTasks}</div>
                    <div class="stat-label">Total Tasks</div>
                </div>
                <div class="stat-card completed">
                    <div class="stat-number">${this.contributions.completedTasks}</div>
                    <div class="stat-label">Completed</div>
                </div>
                <div class="stat-card pending">
                    <div class="stat-number">${this.contributions.pendingTasks}</div>
                    <div class="stat-label">In Progress</div>
                </div>
                <div class="stat-card overdue">
                    <div class="stat-number">${this.contributions.overdueTasks}</div>
                    <div class="stat-label">Overdue</div>
                </div>
            </div>
        ` : '';

        const tasksHtml = tasks.map(task => `
            <div class="task-card ${task.status.toLowerCase().replace('_', '-')}" onclick="openTask('${task.taskUrl}')">
                <div class="task-header">
                    <h3 class="task-title">${task.title}</h3>
                    <span class="task-status status-${task.status.toLowerCase().replace('_', '-')}">${task.status.replace('_', ' ')}</span>
                </div>
                <div class="task-details">
                    <div class="task-project">${task.projectName}</div>
                    <div class="task-deadline">
                        <span class="deadline-label">Deadline:</span>
                        <span class="deadline-date">${new Date(task.deadline).toLocaleDateString()}</span>
                        <span class="remaining-time">${this.calculateRemainingTime(task.deadline)}</span>
                    </div>
                    ${task.branchName ? `<div class="task-branch">🌿 ${task.branchName}</div>` : ''}
                </div>
                <div class="task-commits">
                    <div class="commits-header">
                        <span>📝 ${task.commits.length} commit${task.commits.length !== 1 ? 's' : ''}</span>
                    </div>
                    ${task.commits.slice(0, 3).map(commit => `
                        <div class="commit-item" onclick="event.stopPropagation(); openCommit('${commit.commitUrl}')">
                            <span class="commit-message">${commit.message.substring(0, 50)}${commit.message.length > 50 ? '...' : ''}</span>
                            <span class="commit-status status-${commit.status.toLowerCase()}">${commit.status}</span>
                        </div>
                    `).join('')}
                    ${task.commits.length > 3 ? `<div class="more-commits">+${task.commits.length - 3} more</div>` : ''}
                </div>
            </div>
        `).join('');

        return `
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Galactico Dashboard</title>
            <style>
                ${this.getDashboardCSS()}
            </style>
        </head>
        <body>
            <div class="dashboard-container">
                <header class="dashboard-header">
                    <h1>🚀 Galactico Developer Dashboard</h1>
                    <button class="sync-btn" onclick="syncTasks()">🔄 Sync</button>
                </header>

                ${contributionsHtml}

                <div class="filter-tabs">
                    <button class="filter-tab active" onclick="filterTasks('all')">All Tasks</button>
                    <button class="filter-tab" onclick="filterTasks('in-progress')">In Progress</button>
                    <button class="filter-tab" onclick="filterTasks('completed')">Completed</button>
                    <button class="filter-tab" onclick="filterTasks('overdue')">Overdue</button>
                </div>

                <div class="tasks-container">
                    ${tasks.length > 0 ? tasksHtml : '<div class="no-tasks">No tasks found. Click sync to refresh!</div>'}
                </div>
            </div>

            <script>
                const vscode = acquireVsCodeApi();

                function syncTasks() {
                    vscode.postMessage({ command: 'sync' });
                }

                function openTask(url) {
                    vscode.postMessage({ command: 'openTask', url: url });
                }

                function openCommit(url) {
                    vscode.postMessage({ command: 'openCommit', url: url });
                }

                function filterTasks(filter) {
                    // Update active tab
                    document.querySelectorAll('.filter-tab').forEach(tab => tab.classList.remove('active'));
                    event.target.classList.add('active');
                    
                    vscode.postMessage({ command: 'filterTasks', filter: filter });
                }
            </script>
        </body>
        </html>`;
    }

    /**
     * Get CSS styles for dashboard
     */
    private getDashboardCSS(): string {
        return `
            body {
                font-family: var(--vscode-font-family);
                background-color: var(--vscode-editor-background);
                color: var(--vscode-editor-foreground);
                margin: 0;
                padding: 16px;
                line-height: 1.4;
            }

            .dashboard-container {
                max-width: 1200px;
                margin: 0 auto;
            }

            .dashboard-header {
                display: flex;
                justify-content: space-between;
                align-items: center;
                margin-bottom: 24px;
                padding-bottom: 16px;
                border-bottom: 1px solid var(--vscode-panel-border);
            }

            .dashboard-header h1 {
                margin: 0;
                font-size: 24px;
                font-weight: 600;
            }

            .sync-btn {
                background-color: var(--vscode-button-background);
                color: var(--vscode-button-foreground);
                border: none;
                padding: 8px 16px;
                border-radius: 4px;
                cursor: pointer;
                font-size: 14px;
            }

            .sync-btn:hover {
                background-color: var(--vscode-button-hoverBackground);
            }

            .stats-overview {
                display: grid;
                grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                gap: 16px;
                margin-bottom: 24px;
            }

            .stat-card {
                background-color: var(--vscode-panel-background);
                border: 1px solid var(--vscode-panel-border);
                border-radius: 8px;
                padding: 16px;
                text-align: center;
            }

            .stat-number {
                font-size: 32px;
                font-weight: 700;
                margin-bottom: 4px;
            }

            .stat-label {
                font-size: 14px;
                opacity: 0.8;
            }

            .stat-card.completed .stat-number { color: #4caf50; }
            .stat-card.pending .stat-number { color: #ff9800; }
            .stat-card.overdue .stat-number { color: #f44336; }

            .filter-tabs {
                display: flex;
                gap: 8px;
                margin-bottom: 24px;
                border-bottom: 1px solid var(--vscode-panel-border);
            }

            .filter-tab {
                background: none;
                border: none;
                padding: 12px 16px;
                cursor: pointer;
                color: var(--vscode-editor-foreground);
                border-bottom: 2px solid transparent;
                font-size: 14px;
            }

            .filter-tab:hover {
                background-color: var(--vscode-list-hoverBackground);
            }

            .filter-tab.active {
                border-bottom-color: var(--vscode-focusBorder);
                color: var(--vscode-focusBorder);
            }

            .tasks-container {
                display: grid;
                gap: 16px;
            }

            .task-card {
                background-color: var(--vscode-panel-background);
                border: 1px solid var(--vscode-panel-border);
                border-radius: 8px;
                padding: 16px;
                cursor: pointer;
                transition: all 0.2s ease;
                border-left: 4px solid var(--vscode-panel-border);
            }

            .task-card:hover {
                background-color: var(--vscode-list-hoverBackground);
                transform: translateY(-2px);
                box-shadow: 0 4px 12px rgba(0,0,0,0.1);
            }

            .task-card.assigned { border-left-color: #2196f3; }
            .task-card.in-progress { border-left-color: #ff9800; }
            .task-card.completed { border-left-color: #4caf50; }
            .task-card.overdue { border-left-color: #f44336; }

            .task-header {
                display: flex;
                justify-content: space-between;
                align-items: flex-start;
                margin-bottom: 12px;
            }

            .task-title {
                margin: 0;
                font-size: 18px;
                font-weight: 600;
                flex: 1;
                margin-right: 16px;
            }

            .task-status {
                padding: 4px 8px;
                border-radius: 4px;
                font-size: 12px;
                font-weight: 500;
                text-transform: uppercase;
                white-space: nowrap;
            }

            .status-assigned { background-color: #e3f2fd; color: #1976d2; }
            .status-in-progress { background-color: #fff3e0; color: #f57c00; }
            .status-completed { background-color: #e8f5e8; color: #388e3c; }
            .status-overdue { background-color: #ffebee; color: #d32f2f; }

            .task-details {
                margin-bottom: 16px;
                font-size: 14px;
            }

            .task-project {
                font-weight: 500;
                margin-bottom: 8px;
                color: var(--vscode-textLink-foreground);
            }

            .task-deadline {
                display: flex;
                align-items: center;
                gap: 8px;
                margin-bottom: 4px;
            }

            .deadline-label {
                font-weight: 500;
            }

            .remaining-time {
                background-color: var(--vscode-badge-background);
                color: var(--vscode-badge-foreground);
                padding: 2px 6px;
                border-radius: 3px;
                font-size: 12px;
            }

            .task-branch {
                font-family: monospace;
                font-size: 12px;
                color: var(--vscode-textPreformat-foreground);
            }

            .task-commits {
                border-top: 1px solid var(--vscode-panel-border);
                padding-top: 12px;
            }

            .commits-header {
                font-weight: 500;
                margin-bottom: 8px;
                font-size: 14px;
            }

            .commit-item {
                display: flex;
                justify-content: space-between;
                align-items: center;
                padding: 4px 0;
                border-bottom: 1px solid rgba(128,128,128,0.1);
            }

            .commit-item:hover {
                background-color: var(--vscode-list-hoverBackground);
                margin: 0 -8px;
                padding: 4px 8px;
                border-radius: 4px;
            }

            .commit-message {
                font-family: monospace;
                font-size: 12px;
                flex: 1;
                margin-right: 8px;
            }

            .commit-status {
                font-size: 10px;
                padding: 2px 6px;
                border-radius: 3px;
            }

            .status-approved { background-color: #e8f5e8; color: #388e3c; }
            .status-pending { background-color: #fff3e0; color: #f57c00; }
            .status-rejected { background-color: #ffebee; color: #d32f2f; }

            .more-commits {
                font-size: 12px;
                opacity: 0.7;
                text-align: center;
                padding: 4px;
                font-style: italic;
            }

            .no-tasks {
                text-align: center;
                padding: 64px;
                font-size: 16px;
                opacity: 0.7;
            }

            @media (max-width: 768px) {
                .stats-overview {
                    grid-template-columns: repeat(2, 1fr);
                }
                
                .filter-tabs {
                    flex-wrap: wrap;
                }
                
                .task-header {
                    flex-direction: column;
                    align-items: flex-start;
                }
                
                .task-title {
                    margin-right: 0;
                    margin-bottom: 8px;
                }
            }
        `;
    }

    /**
     * Start auto-refresh timer
     */
    private startAutoRefresh(): void {
        this.stopAutoRefresh();
        this.autoRefreshTimer = setInterval(() => {
            this.syncTasks();
        }, 10 * 60 * 1000); // 10 minutes
    }

    /**
     * Stop auto-refresh timer
     */
    private stopAutoRefresh(): void {
        if (this.autoRefreshTimer) {
            clearInterval(this.autoRefreshTimer);
            this.autoRefreshTimer = undefined;
        }
    }

    /**
     * Open contributions view
     */
    public async openContributions(): Promise<void> {
        if (!this.panel) {
            await this.openTaskDashboard();
            return;
        }

        // Focus on contributions section or open in new panel
        this.panel.reveal(vscode.ViewColumn.One);
        // Could implement a separate contributions webview here
    }

    /**
     * Dispose of resources
     */
    public dispose(): void {
        this.stopAutoRefresh();
        if (this.panel) {
            this.panel.dispose();
        }
    }
}

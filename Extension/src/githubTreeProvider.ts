import * as vscode from 'vscode';

export class GitHubTreeItem extends vscode.TreeItem {
    constructor(
        public readonly label: string,
        public readonly collapsibleState: vscode.TreeItemCollapsibleState,
        public readonly description?: string,
        iconPath?: string | vscode.Uri | { light: vscode.Uri; dark: vscode.Uri } | vscode.ThemeIcon
    ) {
        super(label, collapsibleState);
        this.tooltip = `${this.label}`;
        this.description = description;
        if (iconPath) {
            this.iconPath = iconPath;
        }
    }
}

export class GitHubTreeDataProvider implements vscode.TreeDataProvider<GitHubTreeItem> {
    private _onDidChangeTreeData: vscode.EventEmitter<GitHubTreeItem | undefined | null | void> = new vscode.EventEmitter<GitHubTreeItem | undefined | null | void>();
    readonly onDidChangeTreeData: vscode.Event<GitHubTreeItem | undefined | null | void> = this._onDidChangeTreeData.event;

    private userInfo: any = null;
    private isAuthenticated: boolean = false;
    private repoStatus: { isRepo: boolean; hasRemote: boolean; hasChanges: boolean } = { isRepo: false, hasRemote: false, hasChanges: false };

    constructor() {}

    refresh(): void {
        this._onDidChangeTreeData.fire();
    }

    updateUserInfo(userInfo: any): void {
        this.userInfo = userInfo;
        this.isAuthenticated = true;
        this.refresh();
    }

    clearUserInfo(): void {
        this.userInfo = null;
        this.isAuthenticated = false;
        this.refresh();
    }

    updateRepoStatus(status: { isRepo: boolean; hasRemote: boolean; hasChanges: boolean }): void {
        this.repoStatus = status;
        this.refresh();
    }

    getTreeItem(element: GitHubTreeItem): vscode.TreeItem {
        return element;
    }

    getChildren(element?: GitHubTreeItem): Thenable<GitHubTreeItem[]> {
        if (!element) {
            // Root level - show all main sections
            return Promise.resolve([
                new GitHubTreeItem(
                    'AutoTrack Galactico',
                    vscode.TreeItemCollapsibleState.Expanded,
                    undefined,
                    new vscode.ThemeIcon('project')
                ),
                new GitHubTreeItem(
                    'Sprint Management',
                    vscode.TreeItemCollapsibleState.Expanded,
                    undefined,
                    new vscode.ThemeIcon('rocket')
                ),
                new GitHubTreeItem(
                    'Task Management',
                    vscode.TreeItemCollapsibleState.Expanded,
                    undefined,
                    new vscode.ThemeIcon('list-unordered')
                ),
                new GitHubTreeItem(
                    'Git Automation',
                    vscode.TreeItemCollapsibleState.Expanded,
                    undefined,
                    new vscode.ThemeIcon('git-branch')
                )
            ]);
        } else {
            // Child items based on parent
            switch (element.label) {
                case 'AutoTrack Galactico':
                    return this.getAutoTrackChildren();
                case 'Sprint Management':
                    return this.getSprintManagementChildren();
                case 'Task Management':
                    return this.getTaskManagementChildren();
                case 'Git Automation':
                    return this.getGitAutomationChildren();

                default:
                    return Promise.resolve([]);
            }
        }
    }

    private getAutoTrackChildren(): Promise<GitHubTreeItem[]> {
        return Promise.resolve([
            (() => {
                const item = new GitHubTreeItem(
                    'Quick Authenticate',
                    vscode.TreeItemCollapsibleState.None,
                    'Fast login - opens dashboard for token',
                    new vscode.ThemeIcon('zap')
                );
                item.command = {
                    command: 'githubAuthExtension.quickAuth',
                    title: 'Quick Authenticate'
                };
                return item;
            })(),
            (() => {
                const item = new GitHubTreeItem(
                    'Authenticate with Galactico',
                    vscode.TreeItemCollapsibleState.None,
                    'Login to your Galactico account',
                    new vscode.ThemeIcon('sign-in')
                );
                item.command = {
                    command: 'githubAuthExtension.galacticoAuth',
                    title: 'Authenticate with Galactico'
                };
                return item;
            })(),
            (() => {
                const item = new GitHubTreeItem(
                    'Open Galactico Dashboard',
                    vscode.TreeItemCollapsibleState.None,
                    'Open web dashboard',
                    new vscode.ThemeIcon('browser')
                );
                item.command = {
                    command: 'githubAuthExtension.openGalacticoDashboard',
                    title: 'Open Galactico Dashboard'
                };
                return item;
            })(),
            (() => {
                const item = new GitHubTreeItem(
                    'Open Task Dashboard',
                    vscode.TreeItemCollapsibleState.None,
                    'View task management dashboard',
                    new vscode.ThemeIcon('dashboard')
                );
                item.command = {
                    command: 'githubAuthExtension.openTaskDashboard',
                    title: 'Open Task Dashboard'
                };
                return item;
            })(),
            (() => {
                const item = new GitHubTreeItem(
                    'Sync Tasks',
                    vscode.TreeItemCollapsibleState.None,
                    'Synchronize tasks with Galactico',
                    new vscode.ThemeIcon('sync')
                );
                item.command = {
                    command: 'githubAuthExtension.syncTasks',
                    title: 'Sync Tasks'
                };
                return item;
            })(),
            (() => {
                const item = new GitHubTreeItem(
                    'Check Login Status',
                    vscode.TreeItemCollapsibleState.None,
                    'Check authentication status',
                    new vscode.ThemeIcon('account')
                );
                item.command = {
                    command: 'githubAuthExtension.loginStatus',
                    title: 'Check Login Status'
                };
                return item;
            })()
        ]);
    }

    private getSprintManagementChildren(): Promise<GitHubTreeItem[]> {
        return Promise.resolve([
            (() => {
                const item = new GitHubTreeItem(
                    'Quick Sprint Assignment',
                    vscode.TreeItemCollapsibleState.None,
                    'Assign task to current sprint',
                    new vscode.ThemeIcon('rocket')
                );
                item.command = {
                    command: 'githubAuthExtension.autoTrackQuickSprint',
                    title: 'Quick Sprint Assignment'
                };
                return item;
            })(),
            (() => {
                const item = new GitHubTreeItem(
                    'Select Task for Sprint',
                    vscode.TreeItemCollapsibleState.None,
                    'Select and assign task',
                    new vscode.ThemeIcon('list-selection')
                );
                item.command = {
                    command: 'githubAuthExtension.selectTask',
                    title: 'Select Task'
                };
                return item;
            })(),
            (() => {
                const item = new GitHubTreeItem(
                    'View Commit Status',
                    vscode.TreeItemCollapsibleState.None,
                    'Check commit status for sprint',
                    new vscode.ThemeIcon('eye')
                );
                item.command = {
                    command: 'githubAuthExtension.viewCommitStatus',
                    title: 'View Commit Status'
                };
                return item;
            })(),
            (() => {
                const item = new GitHubTreeItem(
                    'Galactico Smart Commit',
                    vscode.TreeItemCollapsibleState.None,
                    'Create commit with Galactico integration',
                    new vscode.ThemeIcon('git-commit')
                );
                item.command = {
                    command: 'githubAuthExtension.galacticoCommit',
                    title: 'Galactico Commit'
                };
                return item;
            })()
        ]);
    }

    private getTaskManagementChildren(): Promise<GitHubTreeItem[]> {
        return Promise.resolve([
            (() => {
                const item = new GitHubTreeItem(
                    'Create AutoTrack Commit',
                    vscode.TreeItemCollapsibleState.None,
                    'Create formatted commit with task tracking',
                    new vscode.ThemeIcon('git-commit')
                );
                item.command = {
                    command: 'githubAuthExtension.autoTrackCreateCommit',
                    title: 'Create AutoTrack Commit'
                };
                return item;
            })(),
            (() => {
                const item = new GitHubTreeItem(
                    'Quick Backlog Addition',
                    vscode.TreeItemCollapsibleState.None,
                    'Add task to backlog quickly',
                    new vscode.ThemeIcon('list-unordered')
                );
                item.command = {
                    command: 'githubAuthExtension.autoTrackQuickBacklog',
                    title: 'Quick Backlog Addition'
                };
                return item;
            })(),
            (() => {
                const item = new GitHubTreeItem(
                    'Open Contributions',
                    vscode.TreeItemCollapsibleState.None,
                    'View contribution analytics',
                    new vscode.ThemeIcon('group')
                );
                item.command = {
                    command: 'githubAuthExtension.openContributions',
                    title: 'Open Contributions'
                };
                return item;
            })(),
            (() => {
                const item = new GitHubTreeItem(
                    'Configure CI/CD',
                    vscode.TreeItemCollapsibleState.None,
                    'Setup CI/CD pipeline',
                    new vscode.ThemeIcon('zap')
                );
                item.command = {
                    command: 'githubAuthExtension.configureCICD',
                    title: 'Configure CI/CD'
                };
                return item;
            })()
        ]);
    }

    private getGitAutomationChildren(): Promise<GitHubTreeItem[]> {
        return Promise.resolve([
            (() => {
                const item = new GitHubTreeItem(
                    'Git Add .',
                    vscode.TreeItemCollapsibleState.None,
                    'Add all changes',
                    new vscode.ThemeIcon('add')
                );
                item.command = {
                    command: 'githubAuthExtension.gitAdd',
                    title: 'Git Add'
                };
                return item;
            })(),
            (() => {
                const item = new GitHubTreeItem(
                    'Git Commit',
                    vscode.TreeItemCollapsibleState.None,
                    'Commit with message',
                    new vscode.ThemeIcon('git-commit')
                );
                item.command = {
                    command: 'githubAuthExtension.gitCommit',
                    title: 'Git Commit'
                };
                return item;
            })(),
            (() => {
                const item = new GitHubTreeItem(
                    'Commit to Local Branch',
                    vscode.TreeItemCollapsibleState.None,
                    'Add, commit to local branch (no remote push)',
                    new vscode.ThemeIcon('git-branch')
                );
                item.command = {
                    command: 'githubAuthExtension.gitCommitToLocalBranch',
                    title: 'Commit to Local Branch'
                };
                return item;
            })(),
            (() => {
                const item = new GitHubTreeItem(
                    'Git Push to Repository',
                    vscode.TreeItemCollapsibleState.None,
                    'Push to team repository with branch selection',
                    new vscode.ThemeIcon('cloud-upload')
                );
                item.command = {
                    command: 'githubAuthExtension.gitPushToRepo',
                    title: 'Git Push to Repository'
                };
                return item;
            })(),
            (() => {
                const item = new GitHubTreeItem(
                    'Auto: Add + Commit + Push to Repo',
                    vscode.TreeItemCollapsibleState.None,
                    'Complete workflow with branch selection',
                    new vscode.ThemeIcon('rocket')
                );
                item.command = {
                    command: 'githubAuthExtension.gitAutoToRepo',
                    title: 'Git Auto to Repository'
                };
                return item;
            })()
        ]);
    }
}
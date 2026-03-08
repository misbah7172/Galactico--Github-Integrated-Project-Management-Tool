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
            return Promise.resolve([
                new GitHubTreeItem('Galactico', vscode.TreeItemCollapsibleState.Expanded, undefined, new vscode.ThemeIcon('project')),
                new GitHubTreeItem('Git', vscode.TreeItemCollapsibleState.Expanded, undefined, new vscode.ThemeIcon('git-branch')),
                new GitHubTreeItem('Tools', vscode.TreeItemCollapsibleState.Collapsed, undefined, new vscode.ThemeIcon('tools'))
            ]);
        }

        switch (element.label) {
            case 'Galactico': return this.getGalacticoChildren();
            case 'Git': return this.getGitChildren();
            case 'Tools': return this.getToolsChildren();
            default: return Promise.resolve([]);
        }
    }

    private getGalacticoChildren(): Promise<GitHubTreeItem[]> {
        return Promise.resolve([
            this.makeItem('Quick Authenticate', 'githubAuthExtension.quickAuth', 'Fast login', 'zap'),
            this.makeItem('Authenticate', 'githubAuthExtension.galacticoAuth', 'Login to Galactico', 'sign-in'),
            this.makeItem('Task Dashboard', 'githubAuthExtension.openTaskDashboard', 'View tasks in VS Code', 'dashboard'),
            this.makeItem('Web Dashboard', 'githubAuthExtension.openGalacticoDashboard', 'Open in browser', 'browser'),
            this.makeItem('Sync Tasks', 'githubAuthExtension.syncTasks', 'Pull latest tasks', 'sync'),
            this.makeItem('Check Status', 'githubAuthExtension.loginStatus', 'Auth status', 'account')
        ]);
    }

    private getGitChildren(): Promise<GitHubTreeItem[]> {
        return Promise.resolve([
            this.makeItem('Add + Commit + Push', 'githubAuthExtension.gitAuto', 'One-click workflow', 'rocket'),
            this.makeItem('Git Add', 'githubAuthExtension.gitAdd', 'Stage all changes', 'add'),
            this.makeItem('Git Commit', 'githubAuthExtension.gitCommit', 'Commit with message', 'git-commit'),
            this.makeItem('Git Push', 'githubAuthExtension.gitPush', 'Push to remote', 'cloud-upload'),
            this.makeItem('Commit to Branch', 'githubAuthExtension.gitCommitToLocalBranch', 'Local branch commit', 'git-branch'),
            this.makeItem('Push to Repo', 'githubAuthExtension.gitPushToRepo', 'Push with branch selection', 'repo-push')
        ]);
    }

    private getToolsChildren(): Promise<GitHubTreeItem[]> {
        return Promise.resolve([
            this.makeItem('Select Task', 'githubAuthExtension.selectTask', 'Pick task for commit', 'list-selection'),
            this.makeItem('Galactico Commit', 'githubAuthExtension.galacticoCommit', 'Commit linked to task', 'git-commit'),
            this.makeItem('View Contributions', 'githubAuthExtension.openContributions', 'Contribution analytics', 'group'),
            this.makeItem('Configure CI/CD', 'githubAuthExtension.configureCICD', 'Team Lead / Owner only', 'zap'),
            this.makeItem('Create GitHub Repo', 'githubAuthExtension.createRepo', 'New repository', 'repo'),
            this.makeItem('GitHub Auth', 'githubAuthExtension.authenticateGitHub', 'GitHub API auth', 'github')
        ]);
    }

    private makeItem(label: string, command: string, desc: string, icon: string): GitHubTreeItem {
        const item = new GitHubTreeItem(label, vscode.TreeItemCollapsibleState.None, desc, new vscode.ThemeIcon(icon));
        item.command = { command, title: label };
        return item;
    }
}

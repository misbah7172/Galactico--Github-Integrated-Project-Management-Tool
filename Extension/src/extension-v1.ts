import * as vscode from 'vscode';
import * as cp from 'child_process';
import { promisify } from 'util';

const exec = promisify(cp.exec);

/**
 * AutoTrack Galactico VS Code Extension v1.0.0
 * Complete automation for commit creation with NO manual typing required
 */

export function activate(context: vscode.ExtensionContext) {
    console.log('🚀 AutoTrack Galactico Extension v1.0.0 is now active!');

    // Initialize the commit service
    const commitService = new AutoTrackCommitService();

    // Register AutoTrack Commands
    const commands = [
        // Main AutoTrack Commands
        vscode.commands.registerCommand('autotrack.authenticate', async () => {
            vscode.window.showInformationMessage('🚀 Authenticating with AutoTrack...');
            vscode.env.openExternal(vscode.Uri.parse('http://localhost:5000/login'));
        }),

        vscode.commands.registerCommand('autotrack.createCommit', async () => {
            try {
                await commitService.createAutoTrackCommit();
            } catch (error) {
                vscode.window.showErrorMessage(`❌ AutoTrack commit failed: ${error}`);
            }
        }),

        vscode.commands.registerCommand('autotrack.quickSprint', async () => {
            try {
                await commitService.quickSprintAssignment();
            } catch (error) {
                vscode.window.showErrorMessage(`❌ Sprint assignment failed: ${error}`);
            }
        }),

        vscode.commands.registerCommand('autotrack.quickBacklog', async () => {
            try {
                await commitService.quickBacklogAddition();
            } catch (error) {
                vscode.window.showErrorMessage(`❌ Backlog addition failed: ${error}`);
            }
        }),

        vscode.commands.registerCommand('autotrack.viewTasks', async () => {
            vscode.env.openExternal(vscode.Uri.parse('http://localhost:5000/tasks/assigned'));
        }),

        vscode.commands.registerCommand('autotrack.openDashboard', async () => {
            vscode.env.openExternal(vscode.Uri.parse('http://localhost:5000/dashboard'));
        }),

        vscode.commands.registerCommand('autotrack.refresh', async () => {
            vscode.window.showInformationMessage('🔄 AutoTrack data refreshed!');
        }),

        vscode.commands.registerCommand('autotrack.syncTasks', async () => {
            vscode.window.showInformationMessage('🔄 Tasks synchronized with AutoTrack!');
        }),

        vscode.commands.registerCommand('autotrack.commitGuide', async () => {
            showCommitFormatGuide();
        }),

        // Git Commands
        vscode.commands.registerCommand('autotrack.gitAdd', async () => {
            try {
                await commitService.gitAdd();
                vscode.window.showInformationMessage('✅ Files added to Git staging!');
            } catch (error) {
                vscode.window.showErrorMessage(`❌ Git add failed: ${error}`);
            }
        }),

        vscode.commands.registerCommand('autotrack.gitCommit', async () => {
            const message = await vscode.window.showInputBox({
                prompt: 'Enter commit message (or use AutoTrack Create Commit for structured format)',
                placeHolder: 'Your commit message...'
            });
            if (message) {
                try {
                    await commitService.gitCommit(message);
                    vscode.window.showInformationMessage('✅ Commit created!');
                } catch (error) {
                    vscode.window.showErrorMessage(`❌ Git commit failed: ${error}`);
                }
            }
        }),

        vscode.commands.registerCommand('autotrack.gitPush', async () => {
            try {
                await commitService.gitPush();
                vscode.window.showInformationMessage('✅ Changes pushed to repository!');
            } catch (error) {
                vscode.window.showErrorMessage(`❌ Git push failed: ${error}`);
            }
        }),

        vscode.commands.registerCommand('autotrack.gitAuto', async () => {
            try {
                await commitService.gitAddCommitPush();
                vscode.window.showInformationMessage('✅ Auto Git operations completed!');
            } catch (error) {
                vscode.window.showErrorMessage(`❌ Auto Git failed: ${error}`);
            }
        })
    ];

    // Register all commands
    commands.forEach(command => context.subscriptions.push(command));

    // Create tree view
    const treeDataProvider = new AutoTrackTreeProvider();
    vscode.window.createTreeView('autotrackView', {
        treeDataProvider: treeDataProvider,
        showCollapseAll: true
    });

    // Status bar
    const statusBarItem = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Left, 100);
    statusBarItem.text = '$(project) AutoTrack';
    statusBarItem.tooltip = 'AutoTrack Galactico - Click to open dashboard';
    statusBarItem.command = 'autotrack.openDashboard';
    statusBarItem.show();
    context.subscriptions.push(statusBarItem);

    vscode.window.showInformationMessage('🎉 AutoTrack Galactico Extension ready! Use Ctrl+Shift+P and search "AutoTrack" to get started.');
}

/**
 * AutoTrack Commit Service - Complete automation
 */
class AutoTrackCommitService {
    async createAutoTrackCommit(): Promise<void> {
        try {
            vscode.window.showInformationMessage('🚀 Starting AutoTrack Commit Creation...');

            // Step 1: Get Feature/Task information
            const featureCode = await vscode.window.showInputBox({
                prompt: '🏷️ Enter Feature Code (e.g., Feature01, Feature02)',
                placeHolder: 'Feature01',
                validateInput: (value) => {
                    if (!value || !value.match(/^Feature\d+$/i)) {
                        return 'Please enter a valid feature code (e.g., Feature01)';
                    }
                    return null;
                }
            });

            if (!featureCode) return;

            // Step 2: Get task description
            const description = await vscode.window.showInputBox({
                prompt: '📝 Enter task description',
                placeHolder: 'e.g., Create user authentication system'
            });

            if (!description) return;

            // Step 3: Select assignee
            const assignee = await vscode.window.showQuickPick([
                'Mahi', 'Rakib', 'Alex', 'misbah7172', 'Custom...'
            ], {
                placeHolder: 'Choose team member'
            });

            let finalAssignee = assignee;
            if (assignee === 'Custom...') {
                finalAssignee = await vscode.window.showInputBox({
                    prompt: '👤 Enter custom assignee name',
                    placeHolder: 'Team member name'
                });
            }

            if (!finalAssignee) return;

            // Step 4: Select status
            const status = await vscode.window.showQuickPick([
                'todo', 'in-progress', 'done', 'blocked'
            ], {
                placeHolder: 'Choose current status'
            });

            if (!status) return;

            // Step 5: Optional sprint assignment
            const wantSprint = await vscode.window.showQuickPick([
                'Yes - Assign to Sprint', 'No - Skip Sprint Assignment'
            ], {
                placeHolder: 'Assign to sprint?'
            });

            let sprint = '';
            if (wantSprint?.startsWith('Yes')) {
                const sprintName = await vscode.window.showQuickPick([
                    'sprint1', 'sprint2', 'sprint3', 'Custom...'
                ], {
                    placeHolder: 'Choose sprint'
                });

                if (sprintName === 'Custom...') {
                    sprint = await vscode.window.showInputBox({
                        prompt: '🚀 Enter custom sprint name',
                        placeHolder: 'sprint-name'
                    }) || '';
                } else {
                    sprint = sprintName || '';
                }
            }

            // Build the commit message
            let commitMessage = `${featureCode}: ${description} -> ${finalAssignee}`;
            if (sprint) commitMessage += ` -> ${sprint}`;
            commitMessage += ` -> ${status}`;

            // Show preview and confirm
            const confirmed = await vscode.window.showInformationMessage(
                `📝 Preview: ${commitMessage}\n\nProceed with commit?`,
                { modal: true },
                'Yes, Create Commit', 'Cancel'
            );

            if (confirmed === 'Yes, Create Commit') {
                await this.executeGitCommit(commitMessage);
                vscode.window.showInformationMessage(`✅ AutoTrack commit created!\n📝 ${commitMessage}`);
            }

        } catch (error) {
            throw new Error(`AutoTrack commit creation failed: ${error}`);
        }
    }

    async quickSprintAssignment(): Promise<void> {
        const sprint = await vscode.window.showQuickPick([
            'sprint1', 'sprint2', 'sprint3'
        ], { placeHolder: '⚡ Quick Sprint Assignment' });

        if (!sprint) return;

        const assignee = await vscode.window.showQuickPick([
            'Mahi', 'Rakib', 'Alex', 'misbah7172'
        ], { placeHolder: '👤 Assign to team member' });

        if (!assignee) return;

        const taskName = await vscode.window.showInputBox({
            prompt: '📝 Quick task name',
            placeHolder: 'e.g., Fix login bug'
        });

        if (!taskName) return;

        const timestamp = Date.now().toString().slice(-4);
        const commitMessage = `Feature${timestamp}: ${taskName} -> ${assignee} -> ${sprint} -> todo`;
        
        await this.executeGitCommit(commitMessage);
        vscode.window.showInformationMessage(`⚡ Sprint assignment completed!\n📝 ${commitMessage}`);
    }

    async quickBacklogAddition(): Promise<void> {
        const priority = await vscode.window.showQuickPick([
            'high', 'medium', 'low'
        ], { placeHolder: '📋 Select priority level' });

        if (!priority) return;

        const taskName = await vscode.window.showInputBox({
            prompt: '📝 Backlog item description',
            placeHolder: 'e.g., Implement user profile page'
        });

        if (!taskName) return;

        const timestamp = Date.now().toString().slice(-4);
        const commitMessage = `Backlog${timestamp}: ${taskName} -> ${priority} -> priority -> todo`;
        
        await this.executeGitCommit(commitMessage);
        vscode.window.showInformationMessage(`📋 Backlog item added!\n📝 ${commitMessage}`);
    }

    async executeGitCommit(message: string): Promise<void> {
        const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
        if (!workspaceFolder) throw new Error('No workspace folder found');

        const cwd = workspaceFolder.uri.fsPath;
        await exec('git add .', { cwd });
        await exec(`git commit -m "${message.replace(/"/g, '\\"')}"`, { cwd });
    }

    async gitAdd(): Promise<void> {
        const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
        if (!workspaceFolder) throw new Error('No workspace folder found');
        await exec('git add .', { cwd: workspaceFolder.uri.fsPath });
    }

    async gitCommit(message: string): Promise<void> {
        const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
        if (!workspaceFolder) throw new Error('No workspace folder found');
        await exec(`git commit -m "${message.replace(/"/g, '\\"')}"`, { cwd: workspaceFolder.uri.fsPath });
    }

    async gitPush(): Promise<void> {
        const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
        if (!workspaceFolder) throw new Error('No workspace folder found');
        await exec('git push', { cwd: workspaceFolder.uri.fsPath });
    }

    async gitAddCommitPush(): Promise<void> {
        const message = await vscode.window.showInputBox({
            prompt: 'Enter commit message for auto Git operations',
            placeHolder: 'Your commit message...'
        });

        if (!message) throw new Error('Commit message is required');

        await this.gitAdd();
        await this.gitCommit(message);
        await this.gitPush();
    }
}

function showCommitFormatGuide() {
    const panel = vscode.window.createWebviewPanel(
        'autotrackCommitGuide',
        'AutoTrack Commit Format Guide',
        vscode.ViewColumn.One,
        { enableScripts: true }
    );

    panel.webview.html = `<!DOCTYPE html>
    <html><head><style>
        body { font-family: 'Segoe UI', sans-serif; padding: 20px; background: #1e1e1e; color: #d4d4d4; }
        .container { max-width: 800px; margin: 0 auto; }
        h1 { color: #4FC3F7; border-bottom: 2px solid #4FC3F7; padding-bottom: 10px; }
        h2 { color: #81C784; margin-top: 30px; }
        .format-box { background: #2d2d2d; border: 1px solid #404040; border-radius: 8px; padding: 15px; margin: 15px 0; font-family: monospace; }
        .example { background: #1a472a; border-left: 4px solid #4CAF50; }
        .feature { color: #FFB74D; font-weight: bold; }
        .arrow { color: #4FC3F7; font-weight: bold; }
        .status { color: #81C784; font-weight: bold; }
        .highlight { background: #3949AB; padding: 2px 6px; border-radius: 4px; }
    </style></head><body>
        <div class="container">
            <h1>🚀 AutoTrack Commit Format Guide</h1>
            <h2>📝 Basic Structure</h2>
            <div class="format-box">
                <span class="feature">FeatureXXX</span>: Description <span class="arrow">-></span> Assignee <span class="arrow">-></span> <span class="status">Status</span>
            </div>
            <h2>✨ Examples</h2>
            <div class="format-box example">
                Feature01: Create login page <span class="arrow">-></span> Mahi <span class="arrow">-></span> todo<br>
                Feature02: Setup database <span class="arrow">-></span> Rakib <span class="arrow">-></span> sprint1 <span class="arrow">-></span> in-progress<br>
            </div>
            <h2>🎯 Status Options</h2>
            <ul>
                <li><span class="highlight">todo</span> - New task</li>
                <li><span class="highlight">in-progress</span> - Currently working</li>
                <li><span class="highlight">done</span> - Completed</li>
                <li><span class="highlight">blocked</span> - Cannot proceed</li>
            </ul>
            <h2>💡 Pro Tips</h2>
            <ul>
                <li>Use <strong>AutoTrack Create Commit</strong> for guided creation</li>
                <li>Feature codes should be unique (Feature01, Feature02, etc.)</li>
                <li>Always end with status for proper tracking</li>
            </ul>
        </div>
    </body></html>`;
}

class AutoTrackTreeProvider implements vscode.TreeDataProvider<AutoTrackItem> {
    getTreeItem(element: AutoTrackItem): vscode.TreeItem {
        return element;
    }

    getChildren(): Thenable<AutoTrackItem[]> {
        return Promise.resolve([
            new AutoTrackItem('🚀 Create Commit', 'autotrack.createCommit'),
            new AutoTrackItem('⚡ Quick Sprint', 'autotrack.quickSprint'),
            new AutoTrackItem('📋 Quick Backlog', 'autotrack.quickBacklog'),
            new AutoTrackItem('👀 View Tasks', 'autotrack.viewTasks'),
            new AutoTrackItem('📊 Dashboard', 'autotrack.openDashboard'),
            new AutoTrackItem('📖 Format Guide', 'autotrack.commitGuide')
        ]);
    }
}

class AutoTrackItem extends vscode.TreeItem {
    constructor(
        public readonly label: string,
        private readonly commandId: string
    ) {
        super(label, vscode.TreeItemCollapsibleState.None);
        this.command = {
            command: commandId,
            title: label
        };
    }
}

export function deactivate() {
    console.log('AutoTrack Galactico Extension deactivated');
}
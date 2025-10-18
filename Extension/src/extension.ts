import * as vscode from 'vscode';
import { GitHubAuthService } from './githubAuth';
import { GitHubTreeDataProvider } from './githubTreeProvider';
import { GalacticoService } from './galacticoService';
import { DashboardService } from './dashboardService';
import { GalacticoAuthService } from './galacticoAuthService';
import { CICDService } from './cicdService';
import { AutoTrackCommitService } from './autoTrackCommitService';
import { exec } from 'child_process';
import { promisify } from 'util';

const execAsync = promisify(exec);

/**
 * Helper function to execute Git commands
 */
async function executeGitCommand(command: string, operationName: string, showSuccess: boolean = true): Promise<string> {
    try {
        const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
        if (!workspaceFolder) {
            vscode.window.showErrorMessage('No workspace folder found. Please open a folder first.');
            throw new Error('No workspace folder');
        }

        const { stdout, stderr } = await execAsync(command, { 
            cwd: workspaceFolder.uri.fsPath,
            timeout: 30000 // 30 second timeout
        });

        if (showSuccess) {
            if (stdout.trim()) {
                vscode.window.showInformationMessage(`${operationName} completed: ${stdout.trim()}`);
            } else {
                vscode.window.showInformationMessage(`${operationName} completed successfully!`);
            }
        }

        if (stderr.trim() && !stderr.includes('warning')) {
            console.warn(`${operationName} stderr:`, stderr);
        }

        return stdout.trim();
    } catch (error: any) {
        const errorMessage = error.message || 'Unknown error occurred';
        if (showSuccess) {
            vscode.window.showErrorMessage(`${operationName} failed: ${errorMessage}`);
        }
        console.error(`${operationName} error:`, error);
        throw error;
    }
}

/**
 * Check if current directory is a Git repository
 */
async function isGitRepository(): Promise<boolean> {
    try {
        await executeGitCommand('git rev-parse --git-dir', 'Git Check', false);
        return true;
    } catch {
        return false;
    }
}

/**
 * Check if Git repository has a remote origin
 */
async function hasRemoteOrigin(): Promise<boolean> {
    try {
        const result = await executeGitCommand('git remote get-url origin', 'Remote Check', false);
        return result.length > 0;
    } catch {
        return false;
    }
}

/**
 * Get current Git repository status
 */
async function getGitStatus(): Promise<{ hasChanges: boolean; statusText: string }> {
    try {
        const result = await executeGitCommand('git status --porcelain', 'Status Check', false);
        return {
            hasChanges: result.length > 0,
            statusText: result || 'No changes'
        };
    } catch {
        return { hasChanges: false, statusText: 'Not a git repository' };
    }
}

/**
 * Create GitHub repository using authenticated user's token
 */
async function createGitHubRepository(githubAuth: GitHubAuthService, repoName: string, isPrivate: boolean = false): Promise<boolean> {
    try {
        const octokit = await githubAuth.getOctokitClient();
        if (!octokit) {
            vscode.window.showErrorMessage('Please authenticate with GitHub first.');
            return false;
        }

        // Check if repository already exists
        try {
            const userInfo = await githubAuth.getUserInfoIfAuthenticated();
            if (userInfo) {
                await octokit.repos.get({
                    owner: userInfo.login,
                    repo: repoName
                });
                vscode.window.showErrorMessage(`Repository "${repoName}" already exists. Choose another name.`);
                return false;
            }
        } catch (error: any) {
            // Repository doesn't exist, which is what we want
            if (error.status !== 404) {
                throw error;
            }
        }

        // Create the repository
        await octokit.repos.createForAuthenticatedUser({
            name: repoName,
            private: isPrivate,
            auto_init: false
        });

        const userInfo = await githubAuth.getUserInfoIfAuthenticated();
        const repoUrl = `https://github.com/${userInfo?.login}/${repoName}.git`;

        // Initialize git and add remote
        await executeGitCommand('git init', 'Git Init', false);
        await executeGitCommand(`git remote add origin ${repoUrl}`, 'Add Remote', false);
        
        vscode.window.showInformationMessage(`Repository "${repoName}" created successfully and connected!`);
        return true;
    } catch (error: any) {
        vscode.window.showErrorMessage(`Failed to create repository: ${error.message}`);
        return false;
    }
}

/**
 * This method is called when the extension is activated
 */
export function activate(context: vscode.ExtensionContext) {
    console.log('Galactico Extension is now active!');

    // Create instances of services
    const githubAuthService = new GitHubAuthService();
    const galacticoAuthService = new GalacticoAuthService(context);
    const galacticoService = new GalacticoService(githubAuthService);
    const dashboardService = new DashboardService(context, galacticoAuthService);
    const cicdService = new CICDService(context, galacticoAuthService);
    const autoTrackCommitService = new AutoTrackCommitService(githubAuthService);
    const treeDataProvider = new GitHubTreeDataProvider();

    // Register the tree data provider
    const treeView = vscode.window.createTreeView('githubAuthView', {
        treeDataProvider: treeDataProvider,
        showCollapseAll: true
    });

    // Initialize GitHub authentication status on startup
    async function initializeGitHubAuthentication() {
        try {
            const userInfo = await githubAuthService.getUserInfoIfAuthenticated();
            if (userInfo) {
                treeDataProvider.updateUserInfo(userInfo);
                console.log('GitHub authentication restored for user:', userInfo.login);
            }
        } catch (error) {
            console.log('No existing GitHub authentication found');
        }
    }

    // Call initialization
    initializeGitHubAuthentication();

    // Function to update repository status
    async function updateRepositoryStatus() {
        try {
            const isRepo = await isGitRepository();
            const hasRemote = isRepo ? await hasRemoteOrigin() : false;
            const status = isRepo ? await getGitStatus() : { hasChanges: false, statusText: 'Not a git repository' };
            
            treeDataProvider.updateRepoStatus({
                isRepo,
                hasRemote,
                hasChanges: status.hasChanges
            });
        } catch (error) {
            console.error('Failed to update repository status:', error);
        }
    }

    // Update repository status initially and on refresh
    updateRepositoryStatus();

    // Check for existing Galactico authentication on startup
    async function checkGalacticoAuth() {
        try {
            const userInfo = await galacticoAuthService.getUserInfoIfAuthenticated();
            if (userInfo) {
                treeDataProvider.updateUserInfo({
                    login: userInfo.username,
                    name: userInfo.username,
                    email: userInfo.email,
                    avatar_url: '',
                    id: userInfo.id,
                    public_repos: 0,
                    followers: 0,
                    following: 0
                });
                console.log('Restored Galactico authentication for user:', userInfo.username);
            }
        } catch (error) {
            console.log('No existing Galactico authentication found');
        }
    }

    // Check authentication status on startup
    checkGalacticoAuth();

    // Register the command for starting Galactico authentication
    const startAuthCommand = vscode.commands.registerCommand('githubAuthExtension.start', async () => {
        try {
            // Start the Galactico authentication process and get user info
            const userInfo = await galacticoAuthService.authenticate();
            if (userInfo) {
                // Update the tree view with user information
                treeDataProvider.updateUserInfo({
                    login: userInfo.username,
                    name: userInfo.username,
                    email: userInfo.email,
                    avatar_url: '', // Galactico doesn't provide avatar URL in this response
                    id: userInfo.id,
                    public_repos: 0, // Not available from Galactico
                    followers: 0, // Not available from Galactico
                    following: 0 // Not available from Galactico
                });
                vscode.window.showInformationMessage(`Successfully authenticated with Galactico as ${userInfo.username}!`);
            }
        } catch (error) {
            // Handle any unexpected errors
            const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
            vscode.window.showErrorMessage(`Galactico Authentication failed: ${errorMessage}`);
            console.error('Galactico Authentication Error:', error);
            treeDataProvider.clearUserInfo();
        }
    });

    // Register the refresh command
    const refreshCommand = vscode.commands.registerCommand('githubAuthExtension.refresh', async () => {
        try {
            const userInfo = await galacticoAuthService.getUserInfoIfAuthenticated();
            if (userInfo) {
                treeDataProvider.updateUserInfo({
                    login: userInfo.username,
                    name: userInfo.username,
                    email: userInfo.email,
                    avatar_url: '',
                    id: userInfo.id,
                    public_repos: 0,
                    followers: 0,
                    following: 0
                });
                vscode.window.showInformationMessage('Galactico information refreshed!');
            } else {
                vscode.window.showWarningMessage('Please authenticate with Galactico first.');
                treeDataProvider.clearUserInfo();
            }
            // Also update repository status
            await updateRepositoryStatus();
        } catch (error) {
            const errorMessage = error instanceof Error ? error.message : 'Unknown error occurred';
            vscode.window.showErrorMessage(`Failed to refresh: ${errorMessage}`);
            treeDataProvider.clearUserInfo();
        }
    });

    // Register tree item click handler
    const treeItemClickCommand = vscode.commands.registerCommand('githubAuthView.openItem', async (item: any) => {
        if (item.label === 'Profile URL') {
            const userInfo = await githubAuthService.getUserInfoIfAuthenticated();
            if (userInfo?.html_url) {
                vscode.env.openExternal(vscode.Uri.parse(userInfo.html_url));
            }
        } else if (item.label === 'Not Authenticated') {
            vscode.commands.executeCommand('githubAuthExtension.start');
        } else if (item.label === 'Git Add .') {
            vscode.commands.executeCommand('githubAuthExtension.gitAdd');
        } else if (item.label === 'Git Commit') {
            vscode.commands.executeCommand('githubAuthExtension.gitCommit');
        } else if (item.label === 'Git Push') {
            vscode.commands.executeCommand('githubAuthExtension.gitPush');
        } else if (item.label === 'Auto: Add + Commit + Push') {
            vscode.commands.executeCommand('githubAuthExtension.gitAuto');
        } else if (item.label === 'Create GitHub Repository') {
            vscode.commands.executeCommand('githubAuthExtension.createRepo');
        }
    });

    // Register Git automation commands
    const gitAddCommand = vscode.commands.registerCommand('githubAuthExtension.gitAdd', async () => {
        const isRepo = await isGitRepository();
        const hasRemote = await hasRemoteOrigin();
        
        if (!isRepo || !hasRemote) {
            vscode.window.showWarningMessage('Repository not properly connected. Please setup repository first.');
            return;
        }
        
        await executeGitCommand('git add .', 'Git Add');
        treeDataProvider.refresh(); // Refresh to update status
    });

    const gitCommitCommand = vscode.commands.registerCommand('githubAuthExtension.gitCommit', async () => {
        const isRepo = await isGitRepository();
        const hasRemote = await hasRemoteOrigin();
        
        if (!isRepo || !hasRemote) {
            vscode.window.showWarningMessage('Repository not properly connected. Please setup repository first.');
            return;
        }

        const status = await getGitStatus();
        if (!status.hasChanges) {
            vscode.window.showInformationMessage('No changes to commit.');
            return;
        }

        const commitMessage = await vscode.window.showInputBox({
            prompt: 'Enter commit message',
            placeHolder: 'Your commit message here...',
            validateInput: (value) => {
                return value.trim() === '' ? 'Commit message cannot be empty' : null;
            }
        });
        
        if (commitMessage) {
            await executeGitCommand(`git commit -m "${commitMessage}"`, 'Git Commit');
            treeDataProvider.refresh(); // Refresh to update status
        }
    });

    const gitPushCommand = vscode.commands.registerCommand('githubAuthExtension.gitPush', async () => {
        const isRepo = await isGitRepository();
        const hasRemote = await hasRemoteOrigin();
        
        if (!isRepo || !hasRemote) {
            vscode.window.showWarningMessage('Repository not properly connected. Please setup repository first.');
            return;
        }
        
        await executeGitCommand('git push', 'Git Push');
        treeDataProvider.refresh(); // Refresh to update status
    });

    const gitAutoCommand = vscode.commands.registerCommand('githubAuthExtension.gitAuto', async () => {
        const isRepo = await isGitRepository();
        const hasRemote = await hasRemoteOrigin();
        
        if (!isRepo || !hasRemote) {
            vscode.window.showWarningMessage('Repository not properly connected. Please setup repository first.');
            return;
        }

        const status = await getGitStatus();
        if (!status.hasChanges) {
            vscode.window.showInformationMessage('No changes to commit.');
            return;
        }

        const commitMessage = await vscode.window.showInputBox({
            prompt: 'Enter commit message for automated add + commit + push',
            placeHolder: 'Your commit message here...',
            validateInput: (value) => {
                return value.trim() === '' ? 'Commit message cannot be empty' : null;
            }
        });
        
        if (commitMessage) {
            await vscode.window.withProgress({
                location: vscode.ProgressLocation.Notification,
                title: 'Git Automation',
                cancellable: false
            }, async (progress) => {
                try {
                    progress.report({ increment: 0, message: 'Adding files...' });
                    await executeGitCommand('git add .', 'Git Add', false);
                    
                    progress.report({ increment: 33, message: 'Committing changes...' });
                    await executeGitCommand(`git commit -m "${commitMessage}"`, 'Git Commit', false);
                    
                    progress.report({ increment: 66, message: 'Pushing to remote...' });
                    await executeGitCommand('git push', 'Git Push', false);
                    
                    progress.report({ increment: 100, message: 'Complete!' });
                    vscode.window.showInformationMessage('Git automation completed successfully!');
                    treeDataProvider.refresh(); // Refresh to update status
                } catch (error) {
                    vscode.window.showErrorMessage('Git automation failed. Check individual commands.');
                }
            });
        }
    });

    const createRepoCommand = vscode.commands.registerCommand('githubAuthExtension.createRepo', async () => {
        const userInfo = await githubAuthService.getUserInfoIfAuthenticated();
        if (!userInfo) {
            vscode.window.showErrorMessage('Please authenticate with GitHub first.');
            return;
        }

        const repoName = await vscode.window.showInputBox({
            prompt: 'Enter repository name',
            placeHolder: 'my-awesome-project',
            validateInput: (value) => {
                if (value.trim() === '') {
                    return 'Repository name cannot be empty';
                }
                if (!/^[a-zA-Z0-9._-]+$/.test(value)) {
                    return 'Repository name can only contain letters, numbers, dots, hyphens, and underscores';
                }
                return null;
            }
        });

        if (repoName) {
            const isPrivate = await vscode.window.showQuickPick(
                ['Public', 'Private'],
                { placeHolder: 'Select repository visibility' }
            );

            if (isPrivate) {
                const success = await createGitHubRepository(githubAuthService, repoName, isPrivate === 'Private');
                if (success) {
                    treeDataProvider.refresh(); // Refresh to show new status
                }
            }
        }
    });

    // Register Galactico-specific commands
    const galacticoCommitCommand = vscode.commands.registerCommand('githubAuthExtension.galacticoCommit', async () => {
        const isRepo = await isGitRepository();
        const hasRemote = await hasRemoteOrigin();
        
        if (!isRepo || !hasRemote) {
            vscode.window.showWarningMessage('Repository not properly connected. Please setup repository first.');
            return;
        }

        const status = await getGitStatus();
        if (!status.hasChanges) {
            vscode.window.showInformationMessage('No changes to commit.');
            return;
        }

        const success = await galacticoService.performGalacticoCommit();
        if (success) {
            treeDataProvider.refresh(); // Refresh to update status
        }
    });

    const selectTaskCommand = vscode.commands.registerCommand('githubAuthExtension.selectTask', async () => {
        await galacticoService.selectTask();
    });

    const viewCommitStatusCommand = vscode.commands.registerCommand('githubAuthExtension.viewCommitStatus', async () => {
        await galacticoService.viewCommitStatus();
    });

    // Register dashboard commands
    // Add Galactico authentication command
    const galacticoAuthCommand = vscode.commands.registerCommand('githubAuthExtension.galacticoAuth', async () => {
        await galacticoAuthService.authenticate();
    });

    // Add Quick Galactico authentication command
    const quickAuthCommand = vscode.commands.registerCommand('githubAuthExtension.quickAuth', async () => {
        await galacticoAuthService.quickAuthenticate();
    });

    // Add Galactico logout command
    const galacticoLogoutCommand = vscode.commands.registerCommand('githubAuthExtension.galacticoLogout', async () => {
        await galacticoAuthService.logout();
    });

    // Add command to open token display page
    const openTokenDisplayCommand = vscode.commands.registerCommand('githubAuthExtension.openTokenDisplay', async () => {
        await galacticoAuthService.openTokenDisplayPage();
    });

    // Dashboard commands
    const openTaskDashboardCommand = vscode.commands.registerCommand('githubAuthExtension.openTaskDashboard', async () => {
        await dashboardService.openTaskDashboard();
    });

    const syncTasksCommand = vscode.commands.registerCommand('githubAuthExtension.syncTasks', async () => {
        await dashboardService.syncTasks();
    });

    const openContributionsCommand = vscode.commands.registerCommand('githubAuthExtension.openContributions', async () => {
        await dashboardService.openContributions();
    });

    // Add command to open Galactico web dashboard
    const openGalacticoDashboardCommand = vscode.commands.registerCommand('githubAuthExtension.openGalacticoDashboard', async () => {
        const dashboardUrl = 'https://misbah7172.loca.lt/dashboard';
        await vscode.env.openExternal(vscode.Uri.parse(dashboardUrl));
        vscode.window.showInformationMessage('Galactico Dashboard opened in browser!');
    });

    // Add CI/CD configuration command
    const configureCICDCommand = vscode.commands.registerCommand('githubAuthExtension.configureCICD', async () => {
        await cicdService.configureCICD();
    });

    // Add smart login/logout command
    const loginStatusCommand = vscode.commands.registerCommand('githubAuthExtension.loginStatus', async () => {
        try {
            const userInfo = await galacticoAuthService.getUserInfoIfAuthenticated();
            
            if (userInfo) {
                // User is logged in, show user info and options
                const result = await vscode.window.showInformationMessage(
                    `✅ Logged in as: ${userInfo.username} (${userInfo.email})`,
                    'Logout',
                    'Open Token Display',
                    'Open Dashboard',
                    'Cancel'
                );
                
                if (result === 'Logout') {
                    await galacticoAuthService.logout();
                    vscode.window.showInformationMessage('Successfully logged out from Galactico!');
                } else if (result === 'Open Token Display') {
                    await galacticoAuthService.openTokenDisplayPage();
                } else if (result === 'Open Dashboard') {
                    await dashboardService.openTaskDashboard();
                }
            } else {
                // User is not logged in, show login option
                const result = await vscode.window.showInformationMessage(
                    '❌ Not logged in to Galactico',
                    'Login Now',
                    'Cancel'
                );
                
                if (result === 'Login Now') {
                    const authenticatedUser = await galacticoAuthService.authenticate();
                    if (authenticatedUser) {
                        vscode.window.showInformationMessage(
                            `🎉 Successfully logged in as ${authenticatedUser.username}!`,
                            'Open Dashboard'
                        ).then(selection => {
                            if (selection === 'Open Dashboard') {
                                dashboardService.openTaskDashboard();
                            }
                        });
                    }
                }
            }
        } catch (error) {
            vscode.window.showErrorMessage(`Authentication check failed: ${error}`);
        }
    });

    // GitHub Authentication Command
    const authenticateGitHubCommand = vscode.commands.registerCommand('githubAuthExtension.authenticateGitHub', async () => {
        try {
            // First check if already authenticated with GitHub API
            const existingUserInfo = await githubAuthService.getUserInfoIfAuthenticated();
            
            if (existingUserInfo) {
                // Already authenticated with GitHub API, show status and options
                const result = await vscode.window.showInformationMessage(
                    `✅ GitHub API authenticated as: ${existingUserInfo.login} (${existingUserInfo.name || 'No name'})`,
                    'Re-authenticate',
                    'Configure Git',
                    'View Profile',
                    'Test Git Operations',
                    'Cancel'
                );
                
                if (result === 'Re-authenticate') {
                    await authenticateWithGitHubAPI();
                } else if (result === 'Configure Git') {
                    await configureGitCredentials();
                } else if (result === 'View Profile') {
                    vscode.env.openExternal(vscode.Uri.parse(existingUserInfo.html_url));
                } else if (result === 'Test Git Operations') {
                    await testGitOperations();
                }
            } else {
                // Not authenticated with GitHub API, start authentication
                const result = await vscode.window.showInformationMessage(
                    '🔐 GitHub API authentication required to display profile information',
                    'Authenticate with GitHub',
                    'Configure Git Only',
                    'Cancel'
                );
                
                if (result === 'Authenticate with GitHub') {
                    await authenticateWithGitHubAPI();
                } else if (result === 'Configure Git Only') {
                    await configureGitCredentials();
                }
            }
        } catch (error) {
            vscode.window.showErrorMessage(`GitHub authentication failed: ${error}`);
            console.error('GitHub authentication error:', error);
        }
    });

    // Helper function to authenticate with GitHub API
    async function authenticateWithGitHubAPI() {
        try {
            vscode.window.showInformationMessage('🔐 Starting GitHub API authentication...');
            
            // Use the existing GitHub auth service to authenticate and get user info
            const userInfo = await githubAuthService.authenticateAndGetUserInfo();
            
            if (userInfo) {
                // Update the tree provider with the user information
                treeDataProvider.updateUserInfo(userInfo);
                
                vscode.window.showInformationMessage(
                    `🎉 Successfully authenticated with GitHub API as ${userInfo.login}!`,
                    'View Profile',
                    'Configure Git'
                ).then(selection => {
                    if (selection === 'View Profile') {
                        vscode.env.openExternal(vscode.Uri.parse(userInfo.html_url));
                    } else if (selection === 'Configure Git') {
                        configureGitCredentials();
                    }
                });
            }
        } catch (error: any) {
            vscode.window.showErrorMessage(`GitHub API authentication failed: ${error.message}`);
            console.error('GitHub API authentication error:', error);
        }
    }

    // Helper function to configure Git credentials
    async function configureGitCredentials() {
        const userName = await vscode.window.showInputBox({
            prompt: 'Enter your GitHub username',
            placeHolder: 'e.g., misbah7172'
        });

        if (!userName) {
            vscode.window.showWarningMessage('GitHub username is required for authentication.');
            return;
        }

        const userEmail = await vscode.window.showInputBox({
            prompt: 'Enter your GitHub email',
            placeHolder: 'e.g., your.email@example.com'
        });

        if (!userEmail) {
            vscode.window.showWarningMessage('GitHub email is required for authentication.');
            return;
        }

        try {
            // Configure git credentials
            await executeGitCommand(`git config --global user.name "${userName}"`, 'Set Git Username');
            await executeGitCommand(`git config --global user.email "${userEmail}"`, 'Set Git Email');
            
            // Configure credential helper for Windows
            await executeGitCommand('git config --global credential.helper manager-core', 'Set Credential Helper', false);
            
            vscode.window.showInformationMessage(
                `✅ GitHub authentication configured successfully!\n` +
                `Username: ${userName}\n` +
                `Email: ${userEmail}\n\n` +
                `You can now use Git operations (add, commit, push) with GitHub authentication.`
            );

            // Offer to test the configuration
            const testResult = await vscode.window.showInformationMessage(
                'Would you like to test your Git configuration?',
                'Test Now',
                'Later'
            );

            if (testResult === 'Test Now') {
                await testGitOperations();
            }
        } catch (error: any) {
            vscode.window.showErrorMessage(`Failed to configure GitHub authentication: ${error.message}`);
        }
    }

    // Helper function to test Git operations
    async function testGitOperations() {
        try {
            const status = await executeGitCommand('git status --porcelain', 'Check Git Status', false);
            const branch = await executeGitCommand('git branch --show-current', 'Get Current Branch', false);
            const remote = await executeGitCommand('git remote -v', 'Check Remotes', false).catch(() => 'No remotes configured');

            vscode.window.showInformationMessage(
                `🔍 Git Status Test:\n` +
                `Current Branch: ${branch || 'Unknown'}\n` +
                `Changes: ${status ? `${status.split('\n').length} files` : 'No changes'}\n` +
                `Remotes: ${remote ? 'Configured' : 'Not configured'}`
            );
        } catch (error: any) {
            vscode.window.showErrorMessage(`Git test failed: ${error.message}`);
        }
    }

    // Helper function to show Git status
    async function showGitStatus() {
        try {
            const status = await executeGitCommand('git status', 'Git Status');
            const branch = await executeGitCommand('git branch --show-current', 'Current Branch', false);
            
            // Show in output channel for detailed view
            const outputChannel = vscode.window.createOutputChannel('Git Status');
            outputChannel.clear();
            outputChannel.appendLine(`Current Branch: ${branch}`);
            outputChannel.appendLine('Git Status:');
            outputChannel.appendLine(status);
            outputChannel.show();
        } catch (error: any) {
            vscode.window.showErrorMessage(`Failed to get Git status: ${error.message}`);
        }
    }

    // New Git Commit to Local Branch Command
    const gitCommitToLocalBranchCommand = vscode.commands.registerCommand('githubAuthExtension.gitCommitToLocalBranch', async () => {
        try {
            // Step 1: Ask for commit message
            const commitMessage = await vscode.window.showInputBox({
                prompt: 'Enter commit message for local branch',
                placeHolder: 'feat: add new functionality'
            });

            if (!commitMessage) {
                vscode.window.showWarningMessage('Commit message is required');
                return;
            }

            // Step 2: Get existing local branches and ask user to choose or create new
            vscode.window.showInformationMessage('🌿 Preparing local branch selection...');
            let localBranches: string[] = [];
            try {
                const branchOutput = await executeGitCommand('git branch', 'Get Local Branches', false);
                localBranches = branchOutput.split('\n')
                    .map(branch => branch.replace(/^\*?\s+/, '').trim())
                    .filter(branch => branch && !branch.startsWith('('));
            } catch (error) {
                console.log('No existing local branches or not a git repo');
            }

            let selectedBranch: string;
            if (localBranches.length > 0) {
                const branchChoice = await vscode.window.showQuickPick([
                    '➕ Create new local branch',
                    ...localBranches.map(branch => `🌿 ${branch} (existing local branch)`)
                ], {
                    placeHolder: 'Select a local branch to commit to or create new one'
                });

                if (!branchChoice) {
                    return;
                }

                if (branchChoice.startsWith('➕ Create new local branch')) {
                    const newBranch = await vscode.window.showInputBox({
                        prompt: 'Enter new local branch name',
                        placeHolder: 'feature/new-feature'
                    });
                    if (!newBranch) {
                        return;
                    }
                    selectedBranch = newBranch;
                } else {
                    selectedBranch = branchChoice.replace(/^🌿 /, '').replace(' (existing local branch)', '');
                }
            } else {
                const newBranch = await vscode.window.showInputBox({
                    prompt: 'Enter local branch name (no existing local branches found)',
                    placeHolder: 'main'
                });
                if (!newBranch) {
                    return;
                }
                selectedBranch = newBranch;
            }

            // Step 3: Initialize git repository if needed
            try {
                await executeGitCommand('git status', 'Check Git Status', false);
            } catch (error) {
                // Not a git repository, initialize it
                await executeGitCommand('git init', 'Initialize Git Repository');
            }

            // Step 4: Create/switch to local branch, add and commit (NO PUSH)
            await executeGitCommand('git add .', 'Stage Changes');
            
            // Create or switch to local branch
            try {
                await executeGitCommand(`git checkout -b ${selectedBranch}`, `Create local branch '${selectedBranch}' and switch to it`);
                vscode.window.showInformationMessage(`📝 Created and switched to local branch: ${selectedBranch}`);
            } catch (error) {
                // Branch might already exist, try to switch to it
                await executeGitCommand(`git checkout ${selectedBranch}`, `Switch to existing local branch '${selectedBranch}'`);
                vscode.window.showInformationMessage(`📝 Switched to existing local branch: ${selectedBranch}`);
            }

            await executeGitCommand(`git commit -m "${commitMessage}"`, 'Commit Changes to Local Branch');

            vscode.window.showInformationMessage(
                `🎉 Successfully committed to local branch '${selectedBranch}'! 📍 (No remote push)`,
                'View Status',
                'OK'
            ).then(selection => {
                if (selection === 'View Status') {
                    showGitStatus();
                }
            });

        } catch (error: any) {
            vscode.window.showErrorMessage(`Commit to Local Branch failed: ${error.message}`);
        }
    });

    // New Git Push to Repository Command
    const gitPushToRepoCommand = vscode.commands.registerCommand('githubAuthExtension.gitPushToRepo', async () => {
        try {
            // Step 1: Ask for repository URL
            const repoUrl = await vscode.window.showInputBox({
                prompt: 'Enter the GitHub repository URL',
                placeHolder: 'https://github.com/username/repository-name',
                validateInput: (value) => {
                    if (!value) {
                        return 'Repository URL is required';
                    }
                    if (!value.match(/github\.com[\/:]([^\/]+)\/([^\/\.]+)/)) {
                        return 'Please enter a valid GitHub repository URL';
                    }
                    return null;
                }
            });

            if (!repoUrl) {
                return;
            }

            // Step 2: Check if user is a collaborator
            vscode.window.showInformationMessage('🔍 Checking collaborator status...');
            const collaboratorStatus = await githubAuthService.checkCollaboratorStatus(repoUrl);

            if (!collaboratorStatus.isCollaborator) {
                vscode.window.showErrorMessage(
                    `❌ You are not a collaborator on repository: ${collaboratorStatus.owner}/${collaboratorStatus.repo}`,
                    'Contact Owner',
                    'OK'
                ).then(selection => {
                    if (selection === 'Contact Owner') {
                        vscode.env.openExternal(vscode.Uri.parse(`https://github.com/${collaboratorStatus.owner}`));
                    }
                });
                return;
            }

            vscode.window.showInformationMessage(`✅ Collaborator access confirmed for ${collaboratorStatus.owner}/${collaboratorStatus.repo}`);

            // Step 3: Ask for commit message
            const commitMessage = await vscode.window.showInputBox({
                prompt: 'Enter commit message',
                placeHolder: 'feat: add new functionality'
            });

            if (!commitMessage) {
                vscode.window.showWarningMessage('Commit message is required');
                return;
            }

            // Step 4: Get existing local branches and ask user to choose or create new
            vscode.window.showInformationMessage('🌿 Preparing local branch selection...');
            let localBranches: string[] = [];
            try {
                const branchOutput = await executeGitCommand('git branch', 'Get Local Branches', false);
                localBranches = branchOutput.split('\n')
                    .map(branch => branch.replace(/^\*?\s+/, '').trim())
                    .filter(branch => branch && !branch.startsWith('('));
            } catch (error) {
                console.log('No existing local branches or not a git repo');
            }

            let selectedBranch: string;
            if (localBranches.length > 0) {
                const branchChoice = await vscode.window.showQuickPick([
                    '➕ Create new local branch',
                    ...localBranches.map(branch => `🌿 ${branch} (existing local branch)`)
                ], {
                    placeHolder: 'Select a local branch or create new one'
                });

                if (!branchChoice) {
                    return;
                }

                if (branchChoice.startsWith('➕ Create new local branch')) {
                    const newBranch = await vscode.window.showInputBox({
                        prompt: 'Enter new local branch name',
                        placeHolder: 'feature/new-feature'
                    });
                    if (!newBranch) {
                        return;
                    }
                    selectedBranch = newBranch;
                } else {
                    selectedBranch = branchChoice.replace(/^🌿 /, '').replace(' (existing local branch)', '');
                }
            } else {
                const newBranch = await vscode.window.showInputBox({
                    prompt: 'Enter local branch name (no existing local branches found)',
                    placeHolder: 'main'
                });
                if (!newBranch) {
                    return;
                }
                selectedBranch = newBranch;
            }

            // Step 5: Initialize/configure the repository
            await setupRepositoryConnection(repoUrl, collaboratorStatus.owner, collaboratorStatus.repo);

            // Step 6: Create/switch to local branch, commit and push
            await executeGitCommand('git add .', 'Stage Changes');
            
            // Create or switch to local branch
            try {
                await executeGitCommand(`git checkout -b ${selectedBranch}`, `Create local branch '${selectedBranch}' and switch to it`);
                vscode.window.showInformationMessage(`📝 Created and switched to local branch: ${selectedBranch}`);
            } catch (error) {
                // Branch might already exist, try to switch to it
                await executeGitCommand(`git checkout ${selectedBranch}`, `Switch to existing local branch '${selectedBranch}'`);
                vscode.window.showInformationMessage(`📝 Switched to existing local branch: ${selectedBranch}`);
            }

            await executeGitCommand(`git commit -m "${commitMessage}"`, 'Commit Changes to Local Branch');
            await executeGitCommand(`git push -u origin ${selectedBranch}`, `Push Local Branch '${selectedBranch}' to Repository`);

            vscode.window.showInformationMessage(
                `🎉 Successfully pushed local branch '${selectedBranch}' to ${collaboratorStatus.owner}/${collaboratorStatus.repo}!`,
                'View Repository',
                'OK'
            ).then(selection => {
                if (selection === 'View Repository') {
                    vscode.env.openExternal(vscode.Uri.parse(repoUrl));
                }
            });

        } catch (error: any) {
            vscode.window.showErrorMessage(`Git Push to Repository failed: ${error.message}`);
        }
    });

    // New Git Auto to Repository Command
    const gitAutoToRepoCommand = vscode.commands.registerCommand('githubAuthExtension.gitAutoToRepo', async () => {
        try {
            // This combines all steps from gitPushToRepoCommand
            await vscode.commands.executeCommand('githubAuthExtension.gitPushToRepo');
        } catch (error: any) {
            vscode.window.showErrorMessage(`Git Auto to Repository failed: ${error.message}`);
        }
    });

    // Helper function to setup repository connection
    async function setupRepositoryConnection(repoUrl: string, owner: string, repo: string) {
        try {
            // Check if already a git repository
            try {
                await executeGitCommand('git status', 'Check Git Status', false);
            } catch (error) {
                // Not a git repository, initialize it
                await executeGitCommand('git init', 'Initialize Git Repository');
            }

            // Check if remote origin exists
            try {
                const remoteUrl = await executeGitCommand('git remote get-url origin', 'Get Remote URL', false);
                if (remoteUrl !== repoUrl) {
                    // Update remote URL
                    await executeGitCommand(`git remote set-url origin ${repoUrl}`, 'Update Remote URL');
                }
            } catch (error) {
                // No remote origin, add it
                await executeGitCommand(`git remote add origin ${repoUrl}`, 'Add Remote Origin');
            }

            // Fetch from remote to get latest branches
            await executeGitCommand('git fetch origin', 'Fetch from Remote', false);

        } catch (error: any) {
            throw new Error(`Failed to setup repository connection: ${error.message}`);
        }
    }

    // Register AutoTrack enhanced commit commands
    const autoTrackCreateCommitCommand = vscode.commands.registerCommand('githubAuthExtension.autoTrackCreateCommit', async () => {
        await autoTrackCommitService.createAutoTrackCommit();
    });

    const autoTrackQuickSprintCommand = vscode.commands.registerCommand('githubAuthExtension.autoTrackQuickSprint', async () => {
        await autoTrackCommitService.quickSprintAssignment();
    });

    const autoTrackQuickBacklogCommand = vscode.commands.registerCommand('githubAuthExtension.autoTrackQuickBacklog', async () => {
        await autoTrackCommitService.quickBacklogAddition();
    });

    // Add all commands to context subscriptions for proper cleanup
    context.subscriptions.push(
        startAuthCommand,
        refreshCommand,
        treeItemClickCommand,
        gitAddCommand,
        gitCommitCommand,
        gitPushCommand,
        gitAutoCommand,
        galacticoCommitCommand,
        galacticoAuthCommand,
        quickAuthCommand,
        galacticoLogoutCommand,
        openTokenDisplayCommand,
        selectTaskCommand,
        viewCommitStatusCommand,
        openTaskDashboardCommand,
        syncTasksCommand,
        openContributionsCommand,
        openGalacticoDashboardCommand,
        configureCICDCommand,
        loginStatusCommand,
        authenticateGitHubCommand,
        gitCommitToLocalBranchCommand,
        gitPushToRepoCommand,
        gitAutoToRepoCommand,
        createRepoCommand,
        autoTrackCreateCommitCommand,
        autoTrackQuickSprintCommand,
        autoTrackQuickBacklogCommand,
        treeView
    );
}

/**
 * This method is called when the extension is deactivated
 */
export function deactivate() {
    console.log('Galactico Extension is now deactivated');
}

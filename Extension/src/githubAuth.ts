import * as vscode from 'vscode';
import { Octokit } from '@octokit/rest';

/**
 * Service class for handling GitHub authentication and API interactions
 */
export class GitHubAuthService {
    private octokit: Octokit | null = null;

    /**
     * Authenticates the user with GitHub and returns user information
     */
    public async authenticateAndGetUserInfo(): Promise<any> {
        try {
            // Show progress indicator while authenticating
            await vscode.window.withProgress({
                location: vscode.ProgressLocation.Notification,
                title: "Authenticating with GitHub...",
                cancellable: false
            }, async (progress) => {
                progress.report({ increment: 0, message: "Getting GitHub session..." });

                // Get GitHub authentication session
                const session = await this.getGitHubSession();
                
                if (!session) {
                    throw new Error('Failed to obtain GitHub authentication session');
                }

                progress.report({ increment: 50, message: "Fetching user information..." });

                // Initialize Octokit with the access token
                this.octokit = new Octokit({
                    auth: session.accessToken,
                });

                // Fetch user information from GitHub API
                const userInfo = await this.fetchUserInfo();

                progress.report({ increment: 100, message: "Complete!" });

                // Display user information
                this.displayUserInfo(userInfo);
                
                // Return user information for external use
                return userInfo;
            });

        } catch (error) {
            this.handleAuthenticationError(error);
            throw error;
        }
    }

    /**
     * Gets user information if already authenticated
     */
    public async getUserInfoIfAuthenticated(): Promise<any | null> {
        if (!this.octokit) {
            return null;
        }

        try {
            const response = await this.octokit.rest.users.getAuthenticated();
            return response.data;
        } catch (error) {
            console.error('Failed to get user info:', error);
            return null;
        }
    }

    /**
     * Gets the Octokit client if authenticated
     */
    public async getOctokitClient(): Promise<Octokit | null> {
        if (!this.octokit) {
            // Try to get a fresh session if not authenticated
            try {
                const session = await this.getGitHubSession();
                if (session) {
                    this.octokit = new Octokit({
                        auth: session.accessToken,
                    });
                }
            } catch (error) {
                console.error('Failed to get GitHub session:', error);
                return null;
            }
        }
        return this.octokit;
    }

    /**
     * Authenticates the user with GitHub and displays user information (legacy method)
     */
    public async authenticateAndShowUserInfo(): Promise<void> {
        await this.authenticateAndGetUserInfo();
    }

    /**
     * Gets a GitHub authentication session using VS Code's built-in authentication provider
     */
    private async getGitHubSession(): Promise<vscode.AuthenticationSession | null> {
        try {
            const session = await vscode.authentication.getSession(
                'github',
                ['read:user'],
                { createIfNone: true }
            );

            if (!session) {
                throw new Error('No GitHub session was created');
            }

            return session;
        } catch (error) {
            if (error instanceof Error && error.message.includes('User did not consent')) {
                throw new Error('GitHub authentication was cancelled by user');
            }
            throw new Error(`Failed to authenticate with GitHub: ${error instanceof Error ? error.message : 'Unknown error'}`);
        }
    }

    /**
     * Fetches authenticated user information from GitHub API
     */
    private async fetchUserInfo(): Promise<any> {
        if (!this.octokit) {
            throw new Error('GitHub client not initialized');
        }

        try {
            const response = await this.octokit.rest.users.getAuthenticated();
            return response.data;
        } catch (error) {
            throw new Error(`Failed to fetch user information: ${error instanceof Error ? error.message : 'Unknown error'}`);
        }
    }

    /**
     * Displays user information in VS Code UI
     */
    private displayUserInfo(userInfo: any): void {
        const username = userInfo.login;
        const name = userInfo.name || 'No name provided';
        const email = userInfo.email || 'No public email';
        const publicRepos = userInfo.public_repos || 0;
        const followers = userInfo.followers || 0;
        const following = userInfo.following || 0;

        // Show main success message with username
        vscode.window.showInformationMessage(
            `Successfully authenticated as GitHub user: ${username}`,
            'View Profile',
            'Show Details'
        ).then(selection => {
            if (selection === 'View Profile') {
                // Open GitHub profile in browser
                vscode.env.openExternal(vscode.Uri.parse(userInfo.html_url));
            } else if (selection === 'Show Details') {
                // Show detailed user information
                this.showDetailedUserInfo(userInfo);
            }
        });

        // Log user information to output channel
        this.logUserInfoToOutput(userInfo);
    }

    /**
     * Shows detailed user information in a new document
     */
    private async showDetailedUserInfo(userInfo: any): Promise<void> {
        const details = `
GitHub User Information
======================

Username: ${userInfo.login}
Name: ${userInfo.name || 'Not provided'}
Email: ${userInfo.email || 'Not public'}
Bio: ${userInfo.bio || 'No bio available'}
Location: ${userInfo.location || 'Not specified'}
Company: ${userInfo.company || 'Not specified'}
Blog: ${userInfo.blog || 'None'}
Public Repositories: ${userInfo.public_repos}
Followers: ${userInfo.followers}
Following: ${userInfo.following}
Account Created: ${new Date(userInfo.created_at).toLocaleDateString()}
Last Updated: ${new Date(userInfo.updated_at).toLocaleDateString()}
Profile URL: ${userInfo.html_url}
Avatar URL: ${userInfo.avatar_url}
`;

        // Create a new untitled document with the user details
        const document = await vscode.workspace.openTextDocument({
            content: details,
            language: 'plaintext'
        });

        // Show the document in the editor
        await vscode.window.showTextDocument(document);
    }

    /**
     * Logs user information to VS Code output channel
     */
    private logUserInfoToOutput(userInfo: any): void {
        const outputChannel = vscode.window.createOutputChannel('GitHub Auth Extension');
        outputChannel.appendLine('=== GitHub Authentication Successful ===');
        outputChannel.appendLine(`Timestamp: ${new Date().toISOString()}`);
        outputChannel.appendLine(`Username: ${userInfo.login}`);
        outputChannel.appendLine(`Name: ${userInfo.name || 'Not provided'}`);
        outputChannel.appendLine(`Email: ${userInfo.email || 'Not public'}`);
        outputChannel.appendLine(`Public Repos: ${userInfo.public_repos}`);
        outputChannel.appendLine(`Followers: ${userInfo.followers}`);
        outputChannel.appendLine(`Following: ${userInfo.following}`);
        outputChannel.appendLine('=====================================');
        outputChannel.show(true);
    }

    /**
     * Handles authentication errors with appropriate user messaging
     */
    private handleAuthenticationError(error: unknown): void {
        let errorMessage = 'An unknown error occurred during GitHub authentication';
        
        if (error instanceof Error) {
            errorMessage = error.message;
        }

        // Show error message to user
        vscode.window.showErrorMessage(
            `GitHub Authentication Error: ${errorMessage}`,
            'Retry',
            'Help'
        ).then(selection => {
            if (selection === 'Retry') {
                // Retry authentication
                this.authenticateAndShowUserInfo();
            } else if (selection === 'Help') {
                // Open GitHub authentication help
                vscode.env.openExternal(vscode.Uri.parse('https://docs.github.com/en/authentication'));
            }
        });

        // Log detailed error information
        console.error('GitHub Authentication Error Details:', error);
        
        // Also log to output channel for debugging
        const outputChannel = vscode.window.createOutputChannel('GitHub Auth Extension');
        outputChannel.appendLine('=== GitHub Authentication Error ===');
        outputChannel.appendLine(`Timestamp: ${new Date().toISOString()}`);
        outputChannel.appendLine(`Error: ${errorMessage}`);
        outputChannel.appendLine(`Stack: ${error instanceof Error ? error.stack : 'No stack trace available'}`);
        outputChannel.appendLine('===================================');
        outputChannel.show(true);
    }

    /**
     * Check if user is a collaborator on a repository
     */
    public async checkCollaboratorStatus(repoUrl: string): Promise<{ isCollaborator: boolean; owner: string; repo: string }> {
        try {
            // Parse repository URL to get owner and repo name
            const match = repoUrl.match(/github\.com[\/:]([^\/]+)\/([^\/\.]+)/);
            if (!match) {
                throw new Error('Invalid GitHub repository URL');
            }

            const [, owner, repo] = match;

            if (!this.octokit) {
                const session = await this.getGitHubSession();
                if (!session) {
                    throw new Error('Not authenticated with GitHub');
                }
                this.octokit = new Octokit({ auth: session.accessToken });
            }

            // Check if user is a collaborator
            const userInfo = await this.octokit.rest.users.getAuthenticated();
            const username = userInfo.data.login;

            try {
                await this.octokit.rest.repos.checkCollaborator({
                    owner,
                    repo,
                    username
                });
                return { isCollaborator: true, owner, repo };
            } catch (error: any) {
                if (error.status === 404) {
                    return { isCollaborator: false, owner, repo };
                }
                throw error;
            }
        } catch (error: any) {
            throw new Error(`Failed to check collaborator status: ${error.message}`);
        }
    }

    /**
     * Get list of branches from a repository
     */
    public async getRepositoryBranches(owner: string, repo: string): Promise<string[]> {
        try {
            if (!this.octokit) {
                throw new Error('Not authenticated with GitHub');
            }

            const response = await this.octokit.rest.repos.listBranches({
                owner,
                repo,
                per_page: 100
            });

            return response.data.map(branch => branch.name);
        } catch (error: any) {
            throw new Error(`Failed to get repository branches: ${error.message}`);
        }
    }
}

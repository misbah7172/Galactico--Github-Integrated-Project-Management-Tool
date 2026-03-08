import * as vscode from 'vscode';
import { ExtensionConfig } from './config';

/**
 * Interface for user info from Galactico backend
 */
export interface GalacticoUser {
    id: number;
    username: string;
    email: string;
    githubId: string;
    authenticated: boolean;
}

/**
 * Authentication service for Galactico backend integration.
 * Handles token-based authentication with the Galactico web application.
 */
export class GalacticoAuthService {
    private static readonly GALACTICO_BASE_URL = ExtensionConfig.GALACTICO_BASE_URL;
    private static readonly TOKEN_STORAGE_KEY = 'galactico.auth.token';
    private accessToken: string | undefined;

    constructor(private context: vscode.ExtensionContext) {
        // Load stored token on initialization
        this.loadStoredToken();
    }

    /**
     * Authenticate with Galactico backend using OAuth flow
     */
    public async authenticate(): Promise<GalacticoUser | null> {
        try {
            return await vscode.window.withProgress({
                location: vscode.ProgressLocation.Notification,
                title: "Authenticating with Galactico...",
                cancellable: true
            }, async (progress, token) => {
                
                progress.report({ increment: 0, message: "Initializing authentication..." });

                // Check if user is already authenticated
                const existingUser = await this.getUserInfoIfAuthenticated();
                if (existingUser) {
                    vscode.window.showInformationMessage(
                        `Already authenticated as ${existingUser.username}!`,
                        'Continue', 'Re-authenticate'
                    ).then(choice => {
                        if (choice === 'Re-authenticate') {
                            this.logout().then(() => this.authenticate());
                        }
                    });
                    return existingUser;
                }

                progress.report({ increment: 25, message: "Opening Galactico login page..." });

                // Simple approach: Open the login page directly
                const loginUrl = `${GalacticoAuthService.GALACTICO_BASE_URL}/auth/login`;
                await vscode.env.openExternal(vscode.Uri.parse(loginUrl));

                progress.report({ increment: 50, message: "Complete authentication in browser..." });

                // Give user clear instructions
                const choice = await vscode.window.showInformationMessage(
                    'Authentication Steps:\n\n' +
                    '1. Complete login in the browser that just opened\n' +
                    '2. After successful login, go to your dashboard\n' +
                    '3. Choose how you want to get your access token:',
                    'Get Token from Token Page',
                    'Enter Token Manually', 
                    'Cancel'
                );

                if (choice === 'Cancel' || !choice) {
                    throw new Error('Authentication cancelled by user');
                }

                let authToken: string | undefined;

                if (choice === 'Get Token from Token Page') {
                    // Open token display page
                    const tokenPageUrl = `${GalacticoAuthService.GALACTICO_BASE_URL}/api/auth/token-display`;
                    await vscode.env.openExternal(vscode.Uri.parse(tokenPageUrl));
                    
                    authToken = await vscode.window.showInputBox({
                        prompt: 'Copy your access token from the token page and paste it here',
                        placeHolder: 'gal_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx',
                        ignoreFocusOut: true,
                        validateInput: (value) => {
                            if (!value || !value.startsWith('gal_')) {
                                return 'Please enter a valid Galactico token (starts with "gal_")';
                            }
                            return null;
                        }
                    });
                } else {
                    // Manual token entry
                    authToken = await vscode.window.showInputBox({
                        prompt: 'Enter your Galactico access token (you can get this from your dashboard or profile settings)',
                        placeHolder: 'gal_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx',
                        ignoreFocusOut: true,
                        validateInput: (value) => {
                            if (!value || !value.startsWith('gal_')) {
                                return 'Please enter a valid Galactico token (starts with "gal_")';
                            }
                            return null;
                        }
                    });
                }

                if (!authToken) {
                    throw new Error('No token provided');
                }

                progress.report({ increment: 75, message: "Validating token..." });

                // Validate token and get user info
                const userInfo = await this.validateToken(authToken);
                if (!userInfo) {
                    throw new Error('Invalid token or authentication failed');
                }

                // Store token securely
                await this.storeToken(authToken);
                this.accessToken = authToken;

                progress.report({ increment: 100, message: "Authentication complete!" });

                vscode.window.showInformationMessage(
                    `Successfully authenticated as ${userInfo.username}!`,
                    'OK'
                );

                return userInfo;
            });

        } catch (error) {
            vscode.window.showErrorMessage(`Authentication failed: ${error}`);
            console.error('Authentication error:', error);
            return null;
        }
    }

    /**
     * Validate token with Galactico backend
     */
    public async validateToken(token: string): Promise<GalacticoUser | null> {
        try {
            const response = await fetch(`${GalacticoAuthService.GALACTICO_BASE_URL}/api/auth/me`, {
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                }
            });

            if (!response.ok) {
                return null;
            }

            const userInfo = await response.json() as GalacticoUser;
            return userInfo;

        } catch (error) {
            console.error('Token validation error:', error);
            return null;
        }
    }

    /**
     * Get user info if authenticated
     */
    public async getUserInfoIfAuthenticated(): Promise<GalacticoUser | null> {
        if (!this.accessToken) {
            return null;
        }

        return await this.validateToken(this.accessToken);
    }

    /**
     * Get current access token
     */
    public getAccessToken(): string | undefined {
        return this.accessToken;
    }

    /**
     * Check if user is authenticated
     */
    public isAuthenticated(): boolean {
        return !!this.accessToken;
    }

    /**
     * Logout and clear stored token
     */
    public async logout(): Promise<void> {
        try {
            if (this.accessToken) {
                // Notify backend about logout
                await fetch(`${GalacticoAuthService.GALACTICO_BASE_URL}/api/auth/logout`, {
                    method: 'POST',
                    headers: {
                        'Authorization': `Bearer ${this.accessToken}`,
                        'Content-Type': 'application/json'
                    }
                });
            }
        } catch (error) {
            console.error('Logout error:', error);
        } finally {
            // Clear token locally regardless of backend response
            this.accessToken = undefined;
            await this.context.secrets.delete(GalacticoAuthService.TOKEN_STORAGE_KEY);
            vscode.window.showInformationMessage('Logged out successfully');
        }
    }

    /**
     * Make authenticated API request
     */
    public async makeAuthenticatedRequest(endpoint: string, options: RequestInit = {}): Promise<Response> {
        if (!this.accessToken) {
            throw new Error('Not authenticated');
        }

        const url = endpoint.startsWith('http') ? endpoint : `${GalacticoAuthService.GALACTICO_BASE_URL}${endpoint}`;

        return fetch(url, {
            ...options,
            headers: {
                'Authorization': `Bearer ${this.accessToken}`,
                'Content-Type': 'application/json',
                ...options.headers
            }
        });
    }

    /**
     * Store token securely using VS Code secrets API
     */
    private async storeToken(token: string): Promise<void> {
        await this.context.secrets.store(GalacticoAuthService.TOKEN_STORAGE_KEY, token);
    }

    /**
     * Load stored token from VS Code secrets
     */
    private async loadStoredToken(): Promise<void> {
        try {
            const storedToken = await this.context.secrets.get(GalacticoAuthService.TOKEN_STORAGE_KEY);
            if (storedToken) {
                // Validate stored token
                const userInfo = await this.validateToken(storedToken);
                if (userInfo) {
                    this.accessToken = storedToken;
                    console.log('Restored authentication for user:', userInfo.username);
                } else {
                    // Token is invalid, remove it
                    await this.context.secrets.delete(GalacticoAuthService.TOKEN_STORAGE_KEY);
                }
            }
        } catch (error) {
            console.error('Failed to load stored token:', error);
        }
    }

    /**
     * Quick authentication - just open dashboard and ask for token
     */
    public async quickAuthenticate(): Promise<GalacticoUser | null> {
        try {
            // Check if already authenticated
            const existingUser = await this.getUserInfoIfAuthenticated();
            if (existingUser) {
                const choice = await vscode.window.showInformationMessage(
                    `Already authenticated as ${existingUser.username}!`,
                    'Continue', 'Re-authenticate'
                );
                if (choice === 'Continue') {
                    return existingUser;
                }
                if (choice === 'Re-authenticate') {
                    await this.logout();
                    // Continue with fresh authentication
                }
            }

            // Open dashboard directly
            const dashboardUrl = `${GalacticoAuthService.GALACTICO_BASE_URL}/dashboard`;
            await vscode.env.openExternal(vscode.Uri.parse(dashboardUrl));

            // Simple token input
            const authToken = await vscode.window.showInputBox({
                prompt: 'After logging into Galactico dashboard, get your access token from Profile/Settings and paste it here',
                placeHolder: 'gal_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx',
                ignoreFocusOut: true,
                validateInput: (value) => {
                    if (!value) {
                        return 'Please enter your token';
                    }
                    if (!value.startsWith('gal_')) {
                        return 'Please enter a valid Galactico token (starts with "gal_")';
                    }
                    return null;
                }
            });

            if (!authToken) {
                vscode.window.showWarningMessage('Authentication cancelled');
                return null;
            }

            // Validate token
            const userInfo = await this.validateToken(authToken);
            if (!userInfo) {
                vscode.window.showErrorMessage('Invalid token. Please check your token and try again.');
                return null;
            }

            // Store token
            await this.storeToken(authToken);
            this.accessToken = authToken;

            vscode.window.showInformationMessage(
                `✅ Successfully authenticated as ${userInfo.username}!`
            );

            return userInfo;

        } catch (error) {
            vscode.window.showErrorMessage(`Authentication failed: ${error}`);
            return null;
        }
    }

    /**
     * Open the token display page directly
     */
    public async openTokenDisplayPage(): Promise<void> {
        try {
            const tokenDisplayUrl = `${GalacticoAuthService.GALACTICO_BASE_URL}/api/auth/token-display`;
            await vscode.env.openExternal(vscode.Uri.parse(tokenDisplayUrl));
            
            vscode.window.showInformationMessage(
                'Token display page opened in browser. Copy your token from there if needed.',
                'OK'
            );
        } catch (error) {
            vscode.window.showErrorMessage(`Failed to open token display page: ${error}`);
        }
    }

    /**
     * Get authorization header for API requests
     */
    public getAuthHeader(): { [key: string]: string } | {} {
        if (!this.accessToken) {
            return {};
        }
        return {
            'Authorization': `Bearer ${this.accessToken}`
        };
    }
}

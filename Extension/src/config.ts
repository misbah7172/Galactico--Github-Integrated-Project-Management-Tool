import * as vscode from 'vscode';

/**
 * Configuration file for Galactico Extension.
 * All settings are read from VS Code workspace configuration (galactico.*).
 * No secrets are stored in source code.
 */
export class ExtensionConfig {

    private static getConfig(): vscode.WorkspaceConfiguration {
        return vscode.workspace.getConfiguration('galactico');
    }

    /** Base URL of the Galactico web application */
    public static get GALACTICO_BASE_URL(): string {
        return this.getConfig().get<string>('baseUrl', 'https://galactico-app.azurewebsites.net');
    }

    /** OAuth redirect URI (server-side) */
    public static get OAUTH_REDIRECT_URI(): string {
        return `${this.GALACTICO_BASE_URL}/api/auth/oauth-callback`;
    }

    /** Extension auth URL */
    public static get EXTENSION_AUTH_URL(): string {
        return `${this.GALACTICO_BASE_URL}/api/auth/login-url`;
    }

    /** Token display URL */
    public static get EXTENSION_TOKEN_DISPLAY_URL(): string {
        return `${this.GALACTICO_BASE_URL}/api/auth/token-display`;
    }

    /** Extension callback URL */
    public static get EXTENSION_CALLBACK_URL(): string {
        return `${this.GALACTICO_BASE_URL}/api/auth/callback`;
    }

    /** Whether debug logging is enabled */
    public static get DEBUG_MODE(): boolean {
        return this.getConfig().get<boolean>('debug', false);
    }

    public static getBaseUrl(): string {
        return this.GALACTICO_BASE_URL;
    }

    public static log(message: string, ...args: any[]): void {
        if (this.DEBUG_MODE) {
            console.log(`[Galactico] ${message}`, ...args);
        }
    }
}

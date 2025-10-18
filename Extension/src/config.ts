/**
 * Configuration file for Galactico Extension
 * Centralizes all environment-specific settings
 */
export class ExtensionConfig {
    // Base URLs
    public static readonly GALACTICO_BASE_URL = process.env.GALACTICO_BASE_URL || 'https://misbah7172.loca.lt';
    public static readonly LOCAL_DEV_URL = process.env.LOCAL_DEV_URL || 'https://misbah7172.loca.lt';
    
    // GitHub OAuth Configuration
    public static readonly GITHUB_CLIENT_ID = process.env.GITHUB_EXTENSION_CLIENT_ID || 'Ov23lirwNEdn87DVuezT';
    public static readonly GITHUB_CLIENT_SECRET = process.env.GITHUB_EXTENSION_CLIENT_SECRET || '9cc9ff6fdfb462a9246ed7e6d727d18095a87754';
    
    // OAuth URLs
    public static readonly OAUTH_REDIRECT_URI = process.env.OAUTH_EXTENSION_REDIRECT_URI || 'https://misbah7172.loca.lt/api/auth/oauth-callback';
    public static readonly VSCODE_CALLBACK_URL = process.env.VSCODE_CALLBACK_URL || 'vscode://vscode.github-authentication/did-authenticate';
    
    // Extension specific URLs
    public static readonly EXTENSION_AUTH_URL = process.env.EXTENSION_AUTH_URL || 'https://misbah7172.loca.lt/api/auth/login-url';
    public static readonly EXTENSION_TOKEN_DISPLAY_URL = process.env.EXTENSION_TOKEN_DISPLAY_URL || 'https://misbah7172.loca.lt/api/auth/token-display';
    public static readonly EXTENSION_CALLBACK_URL = process.env.EXTENSION_CALLBACK_URL || 'https://misbah7172.loca.lt/api/auth/callback';
    
    // Environment settings
    public static readonly ENVIRONMENT = process.env.ENVIRONMENT || 'development';
    public static readonly DEBUG_MODE = process.env.DEBUG_MODE === 'true' || false;
    
    // Helper methods
    public static isDevelopment(): boolean {
        return this.ENVIRONMENT === 'development';
    }
    
    public static isProduction(): boolean {
        return this.ENVIRONMENT === 'production';
    }
    
    public static getBaseUrl(): string {
        return this.isDevelopment() ? this.LOCAL_DEV_URL : this.GALACTICO_BASE_URL;
    }
    
    public static log(message: string, ...args: any[]): void {
        if (this.DEBUG_MODE) {
            console.log(`[ExtensionConfig] ${message}`, ...args);
        }
    }
}

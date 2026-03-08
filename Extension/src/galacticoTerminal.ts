import * as vscode from 'vscode';
import { exec } from 'child_process';
import { promisify } from 'util';
import { GitHubAuthService } from './githubAuth';
import { GalacticoAuthService } from './galacticoAuthService';

const execAsync = promisify(exec);

/**
 * Galactico Smart Terminal — a VS Code pseudoterminal that intercepts
 * `git commit -m "..."` commands, asks for TaskName and status, auto-fetches
 * the GitHub username, and reformats the commit to:
 *   TaskName : message -> username -> status
 * All other commands pass through unchanged.
 */
export class GalacticoTerminal implements vscode.Pseudoterminal {
    private writeEmitter = new vscode.EventEmitter<string>();
    private closeEmitter = new vscode.EventEmitter<number | void>();

    onDidWrite = this.writeEmitter.event;
    onDidClose = this.closeEmitter.event;

    private inputBuffer = '';
    private cwd: string;
    private history: string[] = [];
    private historyIndex = -1;

    // ANSI escape helpers
    private readonly CYAN = '\x1b[36m';
    private readonly GREEN = '\x1b[32m';
    private readonly YELLOW = '\x1b[33m';
    private readonly RED = '\x1b[31m';
    private readonly MAGENTA = '\x1b[35m';
    private readonly BOLD = '\x1b[1m';
    private readonly DIM = '\x1b[2m';
    private readonly RESET = '\x1b[0m';

    constructor(
        private githubAuth: GitHubAuthService,
        private galacticoAuth: GalacticoAuthService
    ) {
        const ws = vscode.workspace.workspaceFolders?.[0];
        this.cwd = ws ? ws.uri.fsPath : process.cwd();
    }

    open(): void {
        this.printBanner();
        this.printPrompt();
    }

    close(): void { }

    handleInput(data: string): void {
        // Handle special keys
        for (let i = 0; i < data.length; i++) {
            const ch = data[i];

            if (ch === '\r' || ch === '\n') {
                // Enter — execute the line
                this.writeEmitter.fire('\r\n');
                const line = this.inputBuffer.trim();
                this.inputBuffer = '';
                this.historyIndex = -1;
                if (line) {
                    this.history.push(line);
                    this.processCommand(line);
                } else {
                    this.printPrompt();
                }
            } else if (ch === '\x7f' || ch === '\b') {
                // Backspace
                if (this.inputBuffer.length > 0) {
                    this.inputBuffer = this.inputBuffer.slice(0, -1);
                    this.writeEmitter.fire('\b \b');
                }
            } else if (ch === '\x03') {
                // Ctrl+C
                this.inputBuffer = '';
                this.writeEmitter.fire('^C\r\n');
                this.printPrompt();
            } else if (ch === '\x1b') {
                // Escape sequence (arrows etc.)
                if (i + 2 < data.length && data[i + 1] === '[') {
                    const arrow = data[i + 2];
                    i += 2;
                    if (arrow === 'A') {
                        // Up arrow — history back
                        this.navigateHistory(1);
                    } else if (arrow === 'B') {
                        // Down arrow — history forward
                        this.navigateHistory(-1);
                    }
                }
            } else {
                // Normal character
                this.inputBuffer += ch;
                this.writeEmitter.fire(ch);
            }
        }
    }

    // ── Private helpers ─────────────────────────────────────

    private navigateHistory(direction: number): void {
        if (this.history.length === 0) { return; }

        if (direction === 1) {
            // Go back in history
            if (this.historyIndex === -1) {
                this.historyIndex = this.history.length - 1;
            } else if (this.historyIndex > 0) {
                this.historyIndex--;
            }
        } else {
            // Go forward in history
            if (this.historyIndex < this.history.length - 1) {
                this.historyIndex++;
            } else {
                this.historyIndex = -1;
            }
        }

        // Clear current line
        this.clearLine();

        if (this.historyIndex >= 0) {
            this.inputBuffer = this.history[this.historyIndex];
            this.writeEmitter.fire(this.inputBuffer);
        } else {
            this.inputBuffer = '';
        }
    }

    private clearLine(): void {
        // Erase whatever is on the current input
        const len = this.inputBuffer.length;
        this.writeEmitter.fire('\b \b'.repeat(len));
        this.inputBuffer = '';
    }

    private printBanner(): void {
        this.writeEmitter.fire(
            `\r\n${this.CYAN}${this.BOLD}` +
            `  ╔══════════════════════════════════════════════════╗\r\n` +
            `  ║          🚀 Galactico Smart Terminal 🚀          ║\r\n` +
            `  ║──────────────────────────────────────────────────║\r\n` +
            `  ║  git commit commands are auto-formatted for     ║\r\n` +
            `  ║  Galactico contribution tracking.               ║\r\n` +
            `  ║  All other commands run normally.                ║\r\n` +
            `  ╚══════════════════════════════════════════════════╝${this.RESET}\r\n\r\n` +
            `${this.DIM}  Format: TaskName : message -> username -> status${this.RESET}\r\n` +
            `${this.DIM}  Type 'help' for smart terminal info.${this.RESET}\r\n\r\n`
        );
    }

    private printPrompt(): void {
        const folder = this.cwd.split(/[\\/]/).pop() || '';
        this.writeEmitter.fire(
            `${this.GREEN}${this.BOLD}galactico${this.RESET} ` +
            `${this.YELLOW}${folder}${this.RESET}` +
            `${this.CYAN} ❯${this.RESET} `
        );
    }

    private async processCommand(line: string): Promise<void> {
        // Built-in commands
        if (line === 'help') {
            this.printHelp();
            this.printPrompt();
            return;
        }
        if (line === 'clear' || line === 'cls') {
            this.writeEmitter.fire('\x1b[2J\x1b[H');
            this.printPrompt();
            return;
        }
        if (line.startsWith('cd ')) {
            this.handleCd(line.substring(3).trim());
            this.printPrompt();
            return;
        }
        if (line === 'exit') {
            this.closeEmitter.fire(0);
            return;
        }

        // Check if this is a git commit command
        const commitMatch = this.parseGitCommit(line);
        if (commitMatch) {
            await this.handleGitCommit(commitMatch, line);
        } else {
            // Pass through — run normally
            await this.runShellCommand(line);
        }

        this.printPrompt();
    }

    /**
     * Parse a git commit command and extract the user's message.
     * Supports: git commit -m "msg", git commit -m 'msg', git commit -m msg
     */
    private parseGitCommit(line: string): string | null {
        // Match: git commit ... -m "message" or -m 'message' or -m message
        const patterns = [
            /git\s+commit\s+(?:.*\s)?-m\s+"([^"]+)"/i,
            /git\s+commit\s+(?:.*\s)?-m\s+'([^']+)'/i,
            /git\s+commit\s+(?:.*\s)?-m\s+(\S+)/i,
        ];
        for (const pat of patterns) {
            const m = line.match(pat);
            if (m) { return m[1]; }
        }
        return null;
    }

    /**
     * Intercept git commit — ask for TaskName & status, auto-fetch username,
     * then reformat and execute.
     */
    private async handleGitCommit(userMessage: string, originalLine: string): Promise<void> {
        this.writeEmitter.fire(
            `${this.MAGENTA}${this.BOLD}⚡ Galactico Smart Commit Detected${this.RESET}\r\n`
        );

        // 1. Ask for Task Name (TaskID)
        const taskName = await vscode.window.showInputBox({
            prompt: 'Enter Task Name (e.g. LoginFeature, Feature01, BugFix-Auth)',
            placeHolder: 'TaskName — this becomes the commit prefix',
            validateInput: v => (!v || !v.trim()) ? 'Task Name is required' : null
        });
        if (!taskName) {
            this.writeEmitter.fire(`${this.YELLOW}Commit cancelled — no Task Name provided.${this.RESET}\r\n`);
            return;
        }

        // 2. Ask for Status
        const statusPick = await vscode.window.showQuickPick(
            [
                { label: 'todo', description: 'Task is planned but not started' },
                { label: 'in-progress', description: 'Task is being worked on' },
                { label: 'done', description: 'Task is completed' }
            ],
            { placeHolder: 'Select task status for this commit' }
        );
        if (!statusPick) {
            this.writeEmitter.fire(`${this.YELLOW}Commit cancelled — no status selected.${this.RESET}\r\n`);
            return;
        }

        // 3. Auto-fetch GitHub username
        let username = await this.getGitHubUsername();
        if (!username) {
            // Fallback: try git config
            try {
                const { stdout } = await execAsync('git config user.name', { cwd: this.cwd });
                username = stdout.trim();
            } catch { }
        }
        if (!username) {
            // Last resort: ask
            username = await vscode.window.showInputBox({
                prompt: 'Could not detect GitHub username. Enter manually:',
                placeHolder: 'your-github-username'
            }) || 'unknown';
        }

        // 4. Build formatted message
        const formatted = `${taskName.trim()} : ${userMessage} -> ${username} -> ${statusPick.label}`;

        this.writeEmitter.fire(
            `${this.DIM}Original : ${this.RESET}${userMessage}\r\n` +
            `${this.DIM}Formatted: ${this.RESET}${this.GREEN}${formatted}${this.RESET}\r\n\r\n`
        );

        // 5. Reconstruct the git commit command with other flags preserved
        // Replace the original -m "msg" with the formatted message
        const escapedFormatted = formatted.replace(/"/g, '\\"');
        const finalCommand = originalLine.replace(
            /-m\s+(?:"[^"]*"|'[^']*'|\S+)/,
            `-m "${escapedFormatted}"`
        );

        this.writeEmitter.fire(
            `${this.DIM}Executing: ${this.RESET}${this.CYAN}${finalCommand}${this.RESET}\r\n\r\n`
        );

        await this.runShellCommand(finalCommand);
    }

    /**
     * Try to get the GitHub username from auth services.
     */
    private async getGitHubUsername(): Promise<string | null> {
        // Try GitHub auth first (has .login)
        try {
            const info = await this.githubAuth.getUserInfoIfAuthenticated();
            if (info?.login) { return info.login; }
        } catch { }

        // Try Galactico auth
        try {
            const info = await this.galacticoAuth.getUserInfoIfAuthenticated();
            if (info?.username) { return info.username; }
        } catch { }

        return null;
    }

    /**
     * Run a shell command and stream output to the terminal.
     */
    private async runShellCommand(command: string): Promise<void> {
        return new Promise<void>(resolve => {
            const shell = process.platform === 'win32' ? 'powershell.exe' : '/bin/sh';
            const flag = process.platform === 'win32' ? '-Command' : '-c';

            const child = exec(`${shell} ${flag} "${command.replace(/"/g, '\\"')}"`, {
                cwd: this.cwd,
                timeout: 60000
            });

            child.stdout?.on('data', (data: string) => {
                // Normalise newlines for terminal rendering
                const lines = data.toString().replace(/\r?\n/g, '\r\n');
                this.writeEmitter.fire(lines);
            });

            child.stderr?.on('data', (data: string) => {
                const lines = data.toString().replace(/\r?\n/g, '\r\n');
                this.writeEmitter.fire(`${this.RED}${lines}${this.RESET}`);
            });

            child.on('close', (code) => {
                if (code !== 0 && code !== null) {
                    this.writeEmitter.fire(
                        `${this.RED}Process exited with code ${code}${this.RESET}\r\n`
                    );
                }
                resolve();
            });

            child.on('error', (err) => {
                this.writeEmitter.fire(
                    `${this.RED}Error: ${err.message}${this.RESET}\r\n`
                );
                resolve();
            });
        });
    }

    private handleCd(dir: string): void {
        const path = require('path');
        try {
            const target = path.resolve(this.cwd, dir.replace(/['"]/g, ''));
            const fs = require('fs');
            if (fs.existsSync(target) && fs.statSync(target).isDirectory()) {
                this.cwd = target;
                this.writeEmitter.fire(`${this.DIM}Changed directory to: ${target}${this.RESET}\r\n`);
            } else {
                this.writeEmitter.fire(`${this.RED}Directory not found: ${target}${this.RESET}\r\n`);
            }
        } catch (e: any) {
            this.writeEmitter.fire(`${this.RED}cd error: ${e.message}${this.RESET}\r\n`);
        }
    }

    private printHelp(): void {
        this.writeEmitter.fire(
            `\r\n${this.CYAN}${this.BOLD}Galactico Smart Terminal${this.RESET}\r\n` +
            `${this.DIM}─────────────────────────────────────────${this.RESET}\r\n` +
            `${this.BOLD}Smart Commit:${this.RESET}\r\n` +
            `  When you type a ${this.GREEN}git commit -m "message"${this.RESET} command,\r\n` +
            `  the terminal will ask for:\r\n` +
            `    ${this.YELLOW}1.${this.RESET} Task Name   (e.g. LoginFeature)\r\n` +
            `    ${this.YELLOW}2.${this.RESET} Status      (todo / in-progress / done)\r\n` +
            `  Username is auto-detected from GitHub.\r\n\r\n` +
            `  ${this.DIM}Input :${this.RESET}  git commit -m "fixed login form"\r\n` +
            `  ${this.DIM}Output:${this.RESET}  ${this.GREEN}git commit -m "LoginFeature : fixed login form -> misbah7172 -> todo"${this.RESET}\r\n\r\n` +
            `${this.BOLD}Built-in Commands:${this.RESET}\r\n` +
            `  ${this.CYAN}help${this.RESET}   — Show this help\r\n` +
            `  ${this.CYAN}clear${this.RESET}  — Clear screen\r\n` +
            `  ${this.CYAN}cd${this.RESET}     — Change directory\r\n` +
            `  ${this.CYAN}exit${this.RESET}   — Close terminal\r\n\r\n` +
            `${this.DIM}All other commands run normally without modification.${this.RESET}\r\n\r\n`
        );
    }
}

/**
 * Create and show the Galactico Smart Terminal.
 */
export function createGalacticoTerminal(
    githubAuth: GitHubAuthService,
    galacticoAuth: GalacticoAuthService
): vscode.Terminal {
    const pty = new GalacticoTerminal(githubAuth, galacticoAuth);
    const terminal = vscode.window.createTerminal({
        name: '🚀 Galactico Terminal',
        pty
    });
    terminal.show();
    return terminal;
}

import * as vscode from 'vscode';
import { GitHubAuthService } from './githubAuth';
import { exec } from 'child_process';
import { promisify } from 'util';

const execAsync = promisify(exec);

/**
 * Simple Git commit service — works like the command line.
 * No complex commit format; just add, commit, push.
 */
export class AutoTrackCommitService {

    constructor(private githubAuth: GitHubAuthService) {}

    /**
     * Quick commit — prompts for a message, then add + commit.
     */
    public async createAutoTrackCommit(): Promise<void> {
        try {
            const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
            if (!workspaceFolder) {
                vscode.window.showErrorMessage('No workspace folder open.');
                return;
            }
            const cwd = workspaceFolder.uri.fsPath;

            // Check for changes
            const { stdout: status } = await execAsync('git status --porcelain', { cwd });
            if (!status.trim()) {
                vscode.window.showInformationMessage('Nothing to commit — working tree clean.');
                return;
            }

            // Ask for commit message
            const message = await vscode.window.showInputBox({
                prompt: 'Commit message',
                placeHolder: 'e.g. fix: resolve null pointer on dashboard',
                validateInput: v => (!v || !v.trim()) ? 'Message cannot be empty' : null
            });
            if (!message) { return; }

            // git add . && git commit
            await execAsync('git add .', { cwd, timeout: 15000 });
            await execAsync(`git commit -m "${message.replace(/"/g, '\\"')}"`, { cwd, timeout: 15000 });

            vscode.window.showInformationMessage(`Committed: ${message}`);
        } catch (err: any) {
            vscode.window.showErrorMessage(`Commit failed: ${err.message}`);
        }
    }

    /**
     * Quick commit + push in one step.
     */
    public async quickCommitAndPush(): Promise<void> {
        try {
            const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
            if (!workspaceFolder) {
                vscode.window.showErrorMessage('No workspace folder open.');
                return;
            }
            const cwd = workspaceFolder.uri.fsPath;

            const { stdout: status } = await execAsync('git status --porcelain', { cwd });
            if (!status.trim()) {
                vscode.window.showInformationMessage('Nothing to commit — working tree clean.');
                return;
            }

            const message = await vscode.window.showInputBox({
                prompt: 'Commit message (will add + commit + push)',
                placeHolder: 'e.g. feat: add user profile page',
                validateInput: v => (!v || !v.trim()) ? 'Message cannot be empty' : null
            });
            if (!message) { return; }

            await vscode.window.withProgress({
                location: vscode.ProgressLocation.Notification,
                title: 'Git',
                cancellable: false
            }, async (progress) => {
                progress.report({ message: 'Adding files...' });
                await execAsync('git add .', { cwd, timeout: 15000 });

                progress.report({ message: 'Committing...' });
                await execAsync(`git commit -m "${message.replace(/"/g, '\\"')}"`, { cwd, timeout: 15000 });

                progress.report({ message: 'Pushing...' });
                await execAsync('git push', { cwd, timeout: 30000 });

                vscode.window.showInformationMessage(`Pushed: ${message}`);
            });
        } catch (err: any) {
            vscode.window.showErrorMessage(`Push failed: ${err.message}`);
        }
    }

}

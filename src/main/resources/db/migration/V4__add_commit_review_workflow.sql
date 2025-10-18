-- V4: Add commit review workflow tables

-- Create pending_commits table
CREATE TABLE pending_commits (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL,
    commit_message TEXT NOT NULL,
    branch VARCHAR(255) NOT NULL,
    task_id VARCHAR(255),
    commit_time DATETIME NOT NULL,
    commit_url VARCHAR(500),
    commit_sha VARCHAR(255),
    project_id BIGINT,
    user_id BIGINT,
    status ENUM('PENDING_REVIEW', 'APPROVED', 'REJECTED', 'MERGED') NOT NULL DEFAULT 'PENDING_REVIEW',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at DATETIME,
    reviewed_by BIGINT,
    rejection_reason TEXT,
    merged_at DATETIME,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_status (status),
    INDEX idx_username (username),
    INDEX idx_project_id (project_id),
    INDEX idx_created_at (created_at)
);

-- Create approved_commits table
CREATE TABLE approved_commits (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL,
    commit_message TEXT NOT NULL,
    original_branch VARCHAR(255) NOT NULL,
    task_id VARCHAR(255),
    commit_time DATETIME NOT NULL,
    merge_time DATETIME NOT NULL,
    commit_url VARCHAR(500),
    commit_sha VARCHAR(255),
    project_id BIGINT,
    user_id BIGINT,
    approved_by BIGINT NOT NULL,
    approved_time DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE RESTRICT,
    INDEX idx_username (username),
    INDEX idx_project_id (project_id),
    INDEX idx_approved_by (approved_by),
    INDEX idx_merge_time (merge_time),
    INDEX idx_approved_time (approved_time)
);

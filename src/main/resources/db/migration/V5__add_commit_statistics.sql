-- Add commit statistics tracking fields
-- Migration: V5__add_commit_statistics.sql

ALTER TABLE commits ADD COLUMN lines_added INT DEFAULT 0;
ALTER TABLE commits ADD COLUMN lines_modified INT DEFAULT 0;
ALTER TABLE commits ADD COLUMN lines_deleted INT DEFAULT 0;
ALTER TABLE commits ADD COLUMN files_changed INT DEFAULT 0;

-- Update existing records to have default values
UPDATE commits SET 
    lines_added = 0, 
    lines_modified = 0, 
    lines_deleted = 0, 
    files_changed = 0 
WHERE lines_added IS NULL;

-- Add indexes for performance
CREATE INDEX idx_commits_lines_added ON commits(lines_added);
CREATE INDEX idx_commits_lines_modified ON commits(lines_modified);
CREATE INDEX idx_commits_lines_deleted ON commits(lines_deleted);
CREATE INDEX idx_commits_project_author ON commits(project_id, author_name);

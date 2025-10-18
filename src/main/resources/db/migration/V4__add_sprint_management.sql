-- Sprint Management Migration
-- Creates sprint table and adds sprint_id to tasks table

-- Create sprints table
CREATE TABLE IF NOT EXISTS sprints (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UPCOMING',
    project_id BIGINT NOT NULL,
    created_by_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_sprint_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_sprint_created_by FOREIGN KEY (created_by_id) REFERENCES users(id),
    CONSTRAINT chk_sprint_status CHECK (status IN ('UPCOMING', 'ACTIVE', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_sprint_dates CHECK (start_date < end_date),
    
    INDEX idx_sprint_project_status (project_id, status),
    INDEX idx_sprint_dates (start_date, end_date),
    INDEX idx_sprint_status (status)
);

-- Add sprint_id column to tasks table
ALTER TABLE tasks 
ADD COLUMN sprint_id BIGINT NULL,
ADD CONSTRAINT fk_task_sprint FOREIGN KEY (sprint_id) REFERENCES sprints(id) ON DELETE SET NULL;

-- Add index for task-sprint relationship
CREATE INDEX idx_task_sprint ON tasks(sprint_id);

-- Insert sample sprints for existing projects (optional)
-- You can customize this based on your existing data
INSERT INTO sprints (name, description, start_date, end_date, status, project_id, created_by_id, created_at, updated_at) 
SELECT 
    CONCAT('Sprint 1 - ', p.name) as name,
    CONCAT('Initial sprint for project ', p.name) as description,
    DATE_ADD(NOW(), INTERVAL FLOOR(RAND() * 30) DAY) as start_date,
    DATE_ADD(NOW(), INTERVAL (FLOOR(RAND() * 30) + 14) DAY) as end_date,
    'UPCOMING' as status,
    p.id as project_id,
    p.owner_id as created_by_id,
    NOW() as created_at,
    NOW() as updated_at
FROM projects p 
WHERE p.deleted_at IS NULL
LIMIT 10;

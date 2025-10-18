-- Enhanced Sprint, Backlog, and Timeline Features Migration
-- Version 6: Sprint Management, Backlog Prioritization, Timeline Tracking

-- Create backlog_items table for product backlog management
CREATE TABLE IF NOT EXISTS backlog_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    priority_rank INT NOT NULL DEFAULT 0,
    priority_level ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL DEFAULT 'MEDIUM',
    story_points INT DEFAULT 0,
    business_value INT DEFAULT 0,
    effort_estimate INT DEFAULT 0,
    acceptance_criteria TEXT,
    labels VARCHAR(1000),
    epic_name VARCHAR(255),
    user_story TEXT,
    
    -- Status tracking
    backlog_status ENUM('PRODUCT_BACKLOG', 'SPRINT_BACKLOG', 'IN_PROGRESS', 'COMPLETED', 'ARCHIVED') NOT NULL DEFAULT 'PRODUCT_BACKLOG',
    
    -- Relationships
    project_id BIGINT NOT NULL,
    task_id BIGINT,
    sprint_id BIGINT,
    created_by_id BIGINT NOT NULL,
    assigned_to_id BIGINT,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    moved_to_sprint_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    
    -- Foreign key constraints
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL,
    FOREIGN KEY (sprint_id) REFERENCES sprints(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_to_id) REFERENCES users(id) ON DELETE SET NULL,
    
    -- Indexes for performance
    INDEX idx_backlog_project (project_id),
    INDEX idx_backlog_priority (priority_level, priority_rank),
    INDEX idx_backlog_sprint (sprint_id),
    INDEX idx_backlog_status (backlog_status),
    INDEX idx_backlog_created (created_at)
);

-- Create sprint_statistics table for sprint analytics
CREATE TABLE IF NOT EXISTS sprint_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sprint_id BIGINT NOT NULL,
    
    -- Sprint metrics
    planned_story_points INT DEFAULT 0,
    completed_story_points INT DEFAULT 0,
    total_tasks INT DEFAULT 0,
    completed_tasks INT DEFAULT 0,
    
    -- Velocity metrics
    daily_burndown JSON, -- Store daily progress as JSON
    velocity_trend DECIMAL(5,2) DEFAULT 0.00,
    
    -- Code metrics
    total_commits INT DEFAULT 0,
    total_lines_added INT DEFAULT 0,
    total_lines_modified INT DEFAULT 0,
    total_lines_deleted INT DEFAULT 0,
    total_files_changed INT DEFAULT 0,
    
    -- Time tracking
    estimated_hours DECIMAL(8,2) DEFAULT 0.00,
    actual_hours DECIMAL(8,2) DEFAULT 0.00,
    
    -- Sprint health indicators
    sprint_health ENUM('ON_TRACK', 'AT_RISK', 'OFF_TRACK') DEFAULT 'ON_TRACK',
    completion_prediction_date DATE,
    
    -- Timestamps
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    FOREIGN KEY (sprint_id) REFERENCES sprints(id) ON DELETE CASCADE,
    
    -- Unique constraint
    UNIQUE KEY unique_sprint_stats (sprint_id),
    
    -- Indexes
    INDEX idx_sprint_stats_health (sprint_health),
    INDEX idx_sprint_stats_velocity (velocity_trend)
);

-- Create timeline_insights table for timeline tracking and predictions
CREATE TABLE IF NOT EXISTS timeline_insights (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- Reference entity (can be task, sprint, or project)
    entity_type ENUM('TASK', 'SPRINT', 'PROJECT') NOT NULL,
    entity_id BIGINT NOT NULL,
    
    -- Timeline data
    estimated_start_date DATE,
    actual_start_date DATE,
    estimated_end_date DATE,
    actual_end_date DATE,
    predicted_end_date DATE,
    
    -- Progress tracking
    progress_percentage DECIMAL(5,2) DEFAULT 0.00,
    estimated_duration_days INT DEFAULT 0,
    actual_duration_days INT DEFAULT 0,
    
    -- Risk assessment
    timeline_risk_level ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') DEFAULT 'LOW',
    risk_factors JSON, -- Store risk factors as JSON
    
    -- Dependency tracking
    dependencies JSON, -- Store dependencies as JSON
    blocking_issues JSON, -- Store blocking issues as JSON
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes for performance
    INDEX idx_timeline_entity (entity_type, entity_id),
    INDEX idx_timeline_risk (timeline_risk_level),
    INDEX idx_timeline_dates (estimated_end_date, actual_end_date)
);

-- Enhance existing tasks table with new fields
ALTER TABLE tasks 
ADD COLUMN IF NOT EXISTS story_points INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS business_value INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS effort_estimate INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS acceptance_criteria TEXT,
ADD COLUMN IF NOT EXISTS epic_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS user_story TEXT,
ADD COLUMN IF NOT EXISTS original_estimate_hours DECIMAL(8,2) DEFAULT 0.00,
ADD COLUMN IF NOT EXISTS remaining_estimate_hours DECIMAL(8,2) DEFAULT 0.00,
ADD COLUMN IF NOT EXISTS logged_hours DECIMAL(8,2) DEFAULT 0.00,
ADD COLUMN IF NOT EXISTS priority_level ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') DEFAULT 'MEDIUM',
ADD COLUMN IF NOT EXISTS task_type ENUM('STORY', 'BUG', 'EPIC', 'TASK', 'SUBTASK') DEFAULT 'STORY',
ADD COLUMN IF NOT EXISTS blocked_by JSON, -- Store blocking dependencies
ADD COLUMN IF NOT EXISTS blocks JSON; -- Store what this task blocks

-- Enhance existing sprints table with new fields
ALTER TABLE sprints 
ADD COLUMN IF NOT EXISTS sprint_goal TEXT,
ADD COLUMN IF NOT EXISTS planned_velocity INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS actual_velocity INT DEFAULT 0,
ADD COLUMN IF NOT EXISTS sprint_type ENUM('REGULAR', 'HOTFIX', 'RELEASE', 'PLANNING') DEFAULT 'REGULAR',
ADD COLUMN IF NOT EXISTS retrospective_notes TEXT,
ADD COLUMN IF NOT EXISTS sprint_health ENUM('ON_TRACK', 'AT_RISK', 'OFF_TRACK') DEFAULT 'ON_TRACK';

-- Enhance existing commits table with new fields for better analytics
ALTER TABLE commits 
ADD COLUMN IF NOT EXISTS commit_type VARCHAR(50), -- feat, fix, docs, etc.
ADD COLUMN IF NOT EXISTS scope VARCHAR(100), -- component/module affected
ADD COLUMN IF NOT EXISTS breaking_change BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS story_points_completed DECIMAL(3,1) DEFAULT 0.0,
ADD COLUMN IF NOT EXISTS estimated_effort_hours DECIMAL(5,2) DEFAULT 0.00,
ADD COLUMN IF NOT EXISTS complexity_score INT DEFAULT 0;

-- Create sprint_backlog_items junction table for many-to-many relationship
CREATE TABLE IF NOT EXISTS sprint_backlog_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sprint_id BIGINT NOT NULL,
    backlog_item_id BIGINT NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    added_by_id BIGINT NOT NULL,
    commitment_level ENUM('COMMITTED', 'STRETCH', 'OPTIONAL') DEFAULT 'COMMITTED',
    
    -- Foreign key constraints
    FOREIGN KEY (sprint_id) REFERENCES sprints(id) ON DELETE CASCADE,
    FOREIGN KEY (backlog_item_id) REFERENCES backlog_items(id) ON DELETE CASCADE,
    FOREIGN KEY (added_by_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Unique constraint to prevent duplicates
    UNIQUE KEY unique_sprint_backlog (sprint_id, backlog_item_id),
    
    -- Indexes
    INDEX idx_sprint_backlog_sprint (sprint_id),
    INDEX idx_sprint_backlog_item (backlog_item_id)
);

-- Create team_velocity table for velocity tracking
CREATE TABLE IF NOT EXISTS team_velocity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    sprint_id BIGINT NOT NULL,
    
    -- Velocity metrics
    story_points_committed INT DEFAULT 0,
    story_points_completed INT DEFAULT 0,
    tasks_committed INT DEFAULT 0,
    tasks_completed INT DEFAULT 0,
    
    -- Time metrics
    working_days INT DEFAULT 0,
    team_capacity_hours DECIMAL(8,2) DEFAULT 0.00,
    actual_effort_hours DECIMAL(8,2) DEFAULT 0.00,
    
    -- Quality metrics
    bugs_found INT DEFAULT 0,
    code_review_time_hours DECIMAL(8,2) DEFAULT 0.00,
    
    -- Calculated velocity
    velocity_score DECIMAL(8,2) DEFAULT 0.00,
    velocity_trend ENUM('INCREASING', 'STABLE', 'DECREASING') DEFAULT 'STABLE',
    
    -- Timestamps
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (sprint_id) REFERENCES sprints(id) ON DELETE CASCADE,
    
    -- Unique constraint
    UNIQUE KEY unique_project_sprint_velocity (project_id, sprint_id),
    
    -- Indexes
    INDEX idx_velocity_project (project_id),
    INDEX idx_velocity_trend (velocity_trend)
);

-- Create commit_sprint_tracking for linking commits to sprints
CREATE TABLE IF NOT EXISTS commit_sprint_tracking (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    commit_id BIGINT NOT NULL,
    sprint_id BIGINT NOT NULL,
    
    -- Sprint contribution metrics
    story_points_contributed DECIMAL(3,1) DEFAULT 0.0,
    task_progress_percentage DECIMAL(5,2) DEFAULT 0.00,
    
    -- Automatic detection flags
    auto_detected BOOLEAN DEFAULT TRUE,
    manual_override BOOLEAN DEFAULT FALSE,
    
    -- Timestamps
    linked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraints
    FOREIGN KEY (commit_id) REFERENCES commits(id) ON DELETE CASCADE,
    FOREIGN KEY (sprint_id) REFERENCES sprints(id) ON DELETE CASCADE,
    
    -- Unique constraint
    UNIQUE KEY unique_commit_sprint (commit_id, sprint_id),
    
    -- Indexes
    INDEX idx_commit_sprint_commit (commit_id),
    INDEX idx_commit_sprint_sprint (sprint_id)
);

-- Insert sample data for testing (optional)
INSERT INTO backlog_items (title, description, priority_level, story_points, project_id, created_by_id) 
SELECT 
    'Sample Backlog Item: User Authentication',
    'Implement secure user authentication with OAuth2 integration',
    'HIGH',
    8,
    p.id,
    u.id
FROM projects p, users u 
WHERE p.name LIKE '%AutoTrack%' 
AND u.role = 'TEAM_LEAD' 
LIMIT 1;

-- Create indexes for performance optimization
CREATE INDEX IF NOT EXISTS idx_tasks_story_points ON tasks(story_points);
CREATE INDEX IF NOT EXISTS idx_tasks_priority ON tasks(priority_level);
CREATE INDEX IF NOT EXISTS idx_tasks_type ON tasks(task_type);
CREATE INDEX IF NOT EXISTS idx_sprints_health ON sprints(sprint_health);
CREATE INDEX IF NOT EXISTS idx_sprints_type ON sprints(sprint_type);
CREATE INDEX IF NOT EXISTS idx_commits_type ON commits(commit_type);

-- Add comments for documentation
ALTER TABLE backlog_items COMMENT = 'Product backlog items with prioritization and story points';
ALTER TABLE sprint_statistics COMMENT = 'Sprint analytics and burndown data';
ALTER TABLE timeline_insights COMMENT = 'Timeline tracking and prediction data';
ALTER TABLE team_velocity COMMENT = 'Team velocity metrics per sprint';
ALTER TABLE commit_sprint_tracking COMMENT = 'Links commits to sprints for analytics';
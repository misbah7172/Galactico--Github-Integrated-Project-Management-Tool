-- Initial data for Galactico application-- Insert default roles

INSERT INTO roles (name, description, created_at) VALUES 

-- Insert default users (if needed for demo/testing)('ADMIN', 'System Administrator with full access', CURRENT_TIMESTAMP),

-- Note: In production, users would register through GitHub OAuth('TEAM_LEAD', 'Team Leader with team management access', CURRENT_TIMESTAMP),

('MEMBER', 'Regular team member', CURRENT_TIMESTAMP),

-- Insert sample project statuses if they don't exist('VIEWER', 'Read-only access to projects', CURRENT_TIMESTAMP);

-- INSERT INTO project_status (name) VALUES ('Active'), ('Completed'), ('On Hold') 

-- ON CONFLICT (name) DO NOTHING;-- Insert default task statuses

INSERT INTO task_statuses (name, description, color, sort_order, created_at) VALUES 

-- Insert sample task statuses if they don't exist('TODO', 'Task is planned but not started', '#6c757d', 1, CURRENT_TIMESTAMP),

-- INSERT INTO task_status (name) VALUES ('Todo'), ('In Progress'), ('Done'), ('Blocked')('IN_PROGRESS', 'Task is currently being worked on', '#007bff', 2, CURRENT_TIMESTAMP),

-- ON CONFLICT (name) DO NOTHING;('IN_REVIEW', 'Task is completed and under review', '#ffc107', 3, CURRENT_TIMESTAMP),

('DONE', 'Task is completed and verified', '#28a745', 4, CURRENT_TIMESTAMP),

-- Insert sample team roles if they don't exist('BLOCKED', 'Task is blocked by dependencies', '#dc3545', 5, CURRENT_TIMESTAMP),

-- INSERT INTO team_role (name) VALUES ('Owner'), ('Admin'), ('Member'), ('Viewer')('CANCELLED', 'Task has been cancelled', '#6f42c1', 6, CURRENT_TIMESTAMP);
-- ON CONFLICT (name) DO NOTHING;

-- Note: Actual data will be populated through the application's GitHub integration
-- This file serves as a placeholder for any initial seed data that might be needed
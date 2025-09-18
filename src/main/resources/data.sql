-- Insert default roles
INSERT INTO roles (name, description, created_at) VALUES 
('ADMIN', 'System Administrator with full access', CURRENT_TIMESTAMP),
('TEAM_LEAD', 'Team Leader with team management access', CURRENT_TIMESTAMP),
('MEMBER', 'Regular team member', CURRENT_TIMESTAMP),
('VIEWER', 'Read-only access to projects', CURRENT_TIMESTAMP);

-- Insert default task statuses
INSERT INTO task_statuses (name, description, color, sort_order, created_at) VALUES 
('TODO', 'Task is planned but not started', '#6c757d', 1, CURRENT_TIMESTAMP),
('IN_PROGRESS', 'Task is currently being worked on', '#007bff', 2, CURRENT_TIMESTAMP),
('IN_REVIEW', 'Task is completed and under review', '#ffc107', 3, CURRENT_TIMESTAMP),
('DONE', 'Task is completed and verified', '#28a745', 4, CURRENT_TIMESTAMP),
('BLOCKED', 'Task is blocked by dependencies', '#dc3545', 5, CURRENT_TIMESTAMP),
('CANCELLED', 'Task has been cancelled', '#6f42c1', 6, CURRENT_TIMESTAMP);
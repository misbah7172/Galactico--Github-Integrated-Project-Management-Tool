-- Add decline-related columns to tasks table
ALTER TABLE tasks 
ADD COLUMN declined_by_id BIGINT,
ADD COLUMN declined_at TIMESTAMP,
ADD COLUMN decline_reason TEXT;

-- Add foreign key constraint for declined_by_id
ALTER TABLE tasks 
ADD CONSTRAINT FK_tasks_declined_by 
FOREIGN KEY (declined_by_id) REFERENCES users(id);

-- Add index for better performance on declined tasks queries
CREATE INDEX idx_tasks_declined_by ON tasks(declined_by_id);
CREATE INDEX idx_tasks_declined_at ON tasks(declined_at);

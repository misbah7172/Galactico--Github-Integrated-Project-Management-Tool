-- Migration to add issue_type column to backlog_items table
-- Run this if you have existing data

ALTER TABLE backlog_items 
ADD COLUMN issue_type VARCHAR(20) DEFAULT 'STORY' CHECK (issue_type IN ('STORY', 'TASK', 'BUG', 'EPIC'));

-- Update existing records to have default issue_type
UPDATE backlog_items SET issue_type = 'STORY' WHERE issue_type IS NULL;
-- Create database for AutoTrack/Galactico application (PostgreSQL)
-- This file is used as a Docker PostgreSQL init script
-- Note: The database is created automatically by POSTGRES_DB env var in docker-compose
-- Hibernate (ddl-auto=update) manages the schema automatically

SELECT 'Galactico database initialized successfully!' as message;

-- Migration: Support multiple filters per user
-- Version: V2__support_multiple_filters.sql
-- Description:
--   - Introduces a new "filters" table to support multiple filters per user
--   - Migrates existing users.filter_code into the new table
--   - Adds a default application setting for max active filters per user
--   - Keeps users.filter_code for backward compatibility (DO NOT DROP)

-- =========================================================
-- 1. Create filters table
-- =========================================================
-- This table stores multiple filters per user (Many-to-One relationship)
-- IF NOT EXISTS is used to make the migration safer in dev environments
CREATE TABLE IF NOT EXISTS filters (
    id BIGINT NOT NULL AUTO_INCREMENT,        -- Primary key
    user_id BIGINT NOT NULL,                  -- Reference to users table
    name VARCHAR(255) NOT NULL,               -- User-defined filter name
    filter_code TEXT,                         -- The actual filter logic/code
    is_active BIT(1) NOT NULL DEFAULT b'1',   -- Whether the filter is active
    PRIMARY KEY (id),

    -- Foreign key constraint to enforce relationship with users table
    CONSTRAINT fk_filter_user 
        FOREIGN KEY (user_id) 
        REFERENCES users(id) 
        ON DELETE CASCADE
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci;

-- =========================================================
-- 2. Migrate existing filter_code from users table
-- =========================================================
-- For each user that has a filter_code:
--   - Create a new row in filters table
--   - Generate a default name for the filter
-- IMPORTANT:
--   - We DO NOT delete or modify users.filter_code
--   - This ensures backward compatibility with existing logic
INSERT INTO filters (user_id, name, filter_code, is_active)
SELECT 
    u.id,                                            -- Link to user
    CONCAT('Default Filter #', u.id),                 -- Auto-generated name
    u.filter_code,                                   -- Existing filter logic
    b'1'                                             -- Default to active
FROM users u
WHERE u.filter_code IS NOT NULL
  AND TRIM(u.filter_code) != ''

-- =========================================================
-- 3. Insert default application setting
-- =========================================================
-- This setting limits how many filters a user can activate at once
-- INSERT IGNORE prevents duplicate entries if it already exists
INSERT IGNORE INTO app_settings (setting_key, setting_value) 
VALUES ('max_active_filters_per_user', '3');

-- =========================================================
-- Notes:
-- =========================================================
-- - DO NOT drop users.filter_code column
-- - It should remain in the User entity as:
--     // deprecated: use filters table instead
--     private String filterCode;
--
-- - Future migrations can safely remove it once all logic is migrated
-- - This migration assumes users table already exists
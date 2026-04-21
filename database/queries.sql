USE self_regulation_app;

-- ========================================
-- SESSION ANALYSIS
-- ========================================

-- Count how many times the user exceeded their limit today
-- This helps determine if restrictions or penalties should apply
SELECT COUNT(*) AS breaches_today
FROM sessions
WHERE user_id = 1
  AND breached = TRUE
  AND DATE(start_time) = CURDATE();

-- ========================================
-- USER LOCK STATUS
-- ========================================

-- Check if the user is currently locked out of apps
-- 'locked_until' shows when access will be restored
SELECT user_id, user_name, locked_until
FROM users
WHERE user_id = 1;

-- Lock the user for 15 minutes from the current time
-- Used when the session limit is exceeded
UPDATE users
SET locked_until = DATE_ADD(NOW(), INTERVAL 15 MINUTE)
WHERE user_id = 1;

-- Unlock the user immediately
-- Typically used after completing a challenge
UPDATE users
SET locked_until = NULL
WHERE user_id = 1;

-- ========================================
-- CHALLENGE SYSTEM
-- ========================================

-- Record that the user has completed a challenge
-- 'result' can be completed, skipped, or failed
INSERT INTO user_challenges (user_id, challenge_id, result)
VALUES (1, 3, 'completed');

-- Get all available challenges from the system
-- These are displayed to the user in the app
SELECT challenge_id, title, description, type
FROM challenges;

-- ========================================
-- SESSION TRACKING
-- ========================================

-- End the most recent active session for the user
-- Calculates how long the user used the app
UPDATE sessions
SET end_time = NOW(),
    duration_seconds = TIMESTAMPDIFF(SECOND, start_time, NOW())
WHERE user_id = 1
  AND end_time IS NULL
ORDER BY start_time DESC
LIMIT 1;

-- ========================================
-- USER MANAGEMENT
-- ========================================

-- Get all users ordered by last login time (most recent first)
-- Useful for admin monitoring or analytics
SELECT user_id, user_name, email, last_login_at
FROM users
ORDER BY last_login_at DESC;

-- Update the user's last login timestamp
-- This should be called after a successful login
UPDATE users
SET last_login_at = NOW()
WHERE user_id = 1;

-- Count total number of registered users
SELECT COUNT(*) AS total_users
FROM users;

-- Get all users sorted by registration date (newest first)
SELECT user_id, user_name, email, created_at
FROM users
ORDER BY created_at DESC;

-- ========================================
-- DEBUG / TESTING QUERIES
-- ========================================

-- Disable safe updates to allow DELETE without strict WHERE conditions
SET SQL_SAFE_UPDATES = 0;

-- View all users in the system
SELECT *
FROM users
ORDER BY user_id DESC;

-- Delete all session records (for testing/resetting)
DELETE FROM sessions;

-- View all session records (latest first)
SELECT *
FROM sessions
ORDER BY session_id DESC;

-- ========================================
-- GAMIFICATION SYSTEM (STREAKS & POINTS)
-- ========================================

-- View user progress including streaks, points, and level
SELECT user_id, user_name, current_streak, longest_streak, last_streak_date, points, level
FROM users
ORDER BY user_id;

-- View challenge completion history for all users
SELECT *
FROM user_challenges
ORDER BY user_challenge_id DESC;
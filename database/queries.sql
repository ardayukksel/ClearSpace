USE self_regulation_app;

-- Count today's breached sessions
SELECT COUNT(*) AS breaches_today
FROM sessions
WHERE user_id = 1
  AND breached = TRUE
  AND DATE(start_time) = CURDATE();

-- Check if the user is currently locked
SELECT user_id, user_name, locked_until
FROM users
WHERE user_id = 1;

-- Lock the user for 15 minutes
UPDATE users
SET locked_until = DATE_ADD(NOW(), INTERVAL 15 MINUTE)
WHERE user_id = 1;

-- Unlock the user
UPDATE users
SET locked_until = NULL
WHERE user_id = 1;

-- Record challenge completion
INSERT INTO user_challenges (user_id, challenge_id, result)
VALUES (1, 3, 'completed');

-- Get all active challenges
SELECT challenge_id, title, description, type
FROM challenges;

-- End the latest open session for user 1
UPDATE sessions
SET end_time = NOW(),
    duration_seconds = TIMESTAMPDIFF(SECOND, start_time, NOW())
WHERE user_id = 1
  AND end_time IS NULL
ORDER BY start_time DESC
LIMIT 1;

-- Find all users
SELECT user_id, user_name, email, last_login_at
FROM users
ORDER BY last_login_at DESC;

-- Update login time after success
UPDATE users
SET last_login_at = NOW()
WHERE user_id = 1;

-- See how many users exist
SELECT COUNT(*) AS total_users
FROM users;

-- See the latest registered users
SELECT user_id, user_name, email, created_at
FROM users
ORDER BY created_at DESC;

SET SQL_SAFE_UPDATES = 0;

SELECT *
FROM users
ORDER BY user_id DESC;

DELETE FROM sessions;

SELECT *
FROM sessions
ORDER BY session_id DESC;

SELECT user_id, user_name, current_streak, longest_streak, last_streak_date
FROM users
ORDER BY user_id;

SELECT *
FROM user_challenges
ORDER BY user_challenge_id DESC;
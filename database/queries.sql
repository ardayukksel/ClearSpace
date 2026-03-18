USE self_regulation_app;

--  Count today's breached sessions
SELECT COUNT(*) AS breaches_today
FROM sessions
WHERE user_id = 1
	AND breached = TRUE
    AND DATE(start_time) = CURDATE();
  
--  Check if the user is currently locked
SELECT user_id, name, locked_until
FROM users
WHERE user_id = 1;

--  Lock the user for 15 minutes
UPDATE users
SET locked_until = DATE_ADD(NOW(), INTERVAL 15 MINUTE)
WHERE user_id = 1;

--  Unlock the user
UPDATE users
SET locked_until = NULL
WHERE user_id = 1;

-- Record challenge completion
INSERT INTO user_challenges (user_id, challenge_id, result)
VALUES (1, 3, 'completed');

-- Get all active challenges
SELECT challenge_id, title, description, type
FROM challenges
WHERE active = TRUE;

-- Start a new session
INSERT INTO sessions (user_id, regulated_app, start_time)
VALUES (1, 'Instagram', NOW());

-- End a session and calculate duration
UPDATE sessions
SET end_time = NOW(),
	duration_seconds = TIMESTAMPDIFF(SECOND, start_time, NOW())
WHERE session_id = 1
	AND end_time IS NULL
ORDER BY start_time DESC
LIMIT 1;

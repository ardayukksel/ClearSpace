USE self_regulation_app;

SELECT COUNT(*) AS breaches_today
FROM sessions
WHERE user_id = 1
	AND breached = TRUE
    AND DATE(start_time) = CURDATE();
    
SELECT user_id, name, locked_until
FROM users
WHERE user_id = 1;

UPDATE users
SET locked_until = DATE_ADD(NOW(), INTERVAL 15 MINUTE)
WHERE user_id = 1;

UPDATE users
SET locked_until = NULL
WHERE user_id = 1;

UPDATE users
SET locked_until = NULL 
WHERE user_id = 1;

INSERT INTO user_challenges (user_id, challenge_id, result)
VALUES (1, 3, 'completed');

SELECT challenge_id, title, description, type
FROM challenges
WHERE active = TRUE;    

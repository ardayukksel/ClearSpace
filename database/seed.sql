USE self_regulation_app;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE user_challenges;
TRUNCATE TABLE sessions;
TRUNCATE TABLE challenges;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO users (name, email, daily_limit_minutes, locked_until) VALUES
('Sean', 'sean@example.com', 60, NULL),
('Test User', 'test@example.com', 45, NULL);

INSERT INTO challenges (title, description, type, active) VALUES
('Reflection Prompt', 'Why are you opening this app right now?', 'reflection', TRUE),
('Deep Breathing', 'Take three deep breaths before continuing.', 'breathing', TRUE),
('Quick Maths', 'Answer a simple maths question before continuing.', 'maths', TRUE),
('Typing Prompt', 'Type: I want to focus.', 'typing', TRUE);

INSERT INTO sessions (user_id, regulated_app, start_time, end_time, duration_seconds, breached) VALUES
(1, 'Instagram', DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR 40 MINUTE), 1200, TRUE),
(1, 'TikTok', DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR 35 MINUTE), 1500, TRUE),
(1, 'Instagram', DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 45 MINUTE), 900, TRUE);

INSERT INTO user_challenges (user_id, challenge_id, result) VALUES
(1, 1, 'completed'),
(1, 2, 'completed');
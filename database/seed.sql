USE self_regulation_app;

-- ========================================
-- USERS
-- ========================================
INSERT INTO users (user_name, email, session_limit_minutes, daily_limit_minutes)
VALUES
('Sean', 'sean@example.com', 15, 60),
('Alex', 'alex@example.com', 15, 60),
('Jordan', 'jordan@example.com', 15, 60);


-- ========================================
-- CHALLENGES
-- ========================================
INSERT INTO challenges (title, description, type)
VALUES
('Deep Breathing', 'Take 10 deep breaths slowly', 'breathing'),
('Quick Maths', 'Solve a simple math problem', 'maths'),
('Reflection Prompt', 'Why are you using this app right now?', 'reflection');

-- ========================================
-- SESSIONS (simulate usage + breaches)
-- ========================================
INSERT INTO sessions (user_id, regulated_app, start_time, end_time, duration_seconds, breached)
VALUES
(1, 'Instagram', NOW(), NOW(), 1200, TRUE),
(1, 'Instagram', NOW(), NOW(), 1300, TRUE),
(1, 'Instagram', NOW(), NOW(), 1400, TRUE);


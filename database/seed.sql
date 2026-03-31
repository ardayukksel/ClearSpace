USE self_regulation_app;

-- ========================================
-- USERS
-- ========================================
INSERT INTO users (name, email, session_limit_minutes, daily_limit_minutes)
VALUES
('Sean', 'sean@example.com', 15, 60),
('Alex', 'alex@example.com', 15, 60),
('Jordan', 'jordan@example.com', 15, 60);

-- ========================================
-- FRIENDS (Sean selects Alex & Jordan)
-- ========================================
INSERT INTO friends (user_id, friend_user_id, status)
VALUES
(1, 2, 'accepted'),
(1, 3, 'accepted');

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

-- ========================================
-- UNLOCK REQUEST (triggered after breaches)
-- ========================================
INSERT INTO unlock_requests (user_id, reason, status, created_at)
VALUES
(1, 'Repeated session limit breaches', 'pending', NOW());

-- ========================================
-- UNLOCK APPROVALS (Alex + Jordan must approve)
-- ========================================
INSERT INTO unlock_request_approvals (unlock_request_id, friend_user_id, decision)
VALUES
(1, 2, 'pending'),
(1, 3, 'pending');

-- ========================================
-- NOTIFICATIONS (friends get request)
-- ========================================
INSERT INTO notifications (user_id, type, payload)
VALUES
(2, 'unlock_request', JSON_OBJECT('request_id', 1, 'from_user', 1)),
(3, 'unlock_request', JSON_OBJECT('request_id', 1, 'from_user', 1));
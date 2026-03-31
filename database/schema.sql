DROP DATABASE IF EXISTS self_regulation_app;

CREATE DATABASE self_regulation_app
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE self_regulation_app;

-- ========================================
-- USERS
-- ========================================
CREATE TABLE users (
    user_id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name                    VARCHAR(120) NOT NULL,
    email                   VARCHAR(190) NOT NULL,
    password_hash           VARCHAR(255) NULL,
    session_limit_minutes   INT UNSIGNED NOT NULL DEFAULT 15,
    daily_limit_minutes     INT UNSIGNED NOT NULL DEFAULT 60,
    locked_until            DATETIME NULL,
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_users_email (email),
    CONSTRAINT chk_users_limits
        CHECK (session_limit_minutes > 0 AND daily_limit_minutes > 0)
) ENGINE=InnoDB;

-- ========================================
-- SESSIONS
-- ========================================
CREATE TABLE sessions (
    session_id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id             BIGINT UNSIGNED NOT NULL,
    regulated_app       VARCHAR(80) NOT NULL,
    start_time          DATETIME NOT NULL,
    end_time            DATETIME NULL,
    duration_seconds    INT UNSIGNED NOT NULL DEFAULT 0,
    breached            BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (session_id),
    KEY idx_sessions_user_time (user_id, start_time),
    KEY idx_sessions_breached (user_id, breached, start_time),
    CONSTRAINT fk_sessions_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT chk_session_time
        CHECK (end_time IS NULL OR end_time >= start_time)
) ENGINE=InnoDB;

-- ========================================
-- CHALLENGES
-- ========================================
CREATE TABLE challenges (
    challenge_id    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    title           VARCHAR(120) NOT NULL,
    description     TEXT NULL,
    type            ENUM('reflection','breathing','maths','typing') NOT NULL,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (challenge_id),
    KEY idx_challenges_active (active)
) ENGINE=InnoDB;

-- ========================================
-- USER CHALLENGES
-- ========================================
CREATE TABLE user_challenges (
    user_challenge_id   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id             BIGINT UNSIGNED NOT NULL,
    challenge_id        BIGINT UNSIGNED NOT NULL,
    result              ENUM('completed','skipped','failed') NOT NULL DEFAULT 'completed',
    completed_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_challenge_id),
    KEY idx_uc_user_time (user_id, completed_at),
    KEY idx_uc_challenge (challenge_id),
    CONSTRAINT fk_uc_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_uc_challenge
        FOREIGN KEY (challenge_id) REFERENCES challenges(challenge_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ========================================
-- FRIENDS
-- Stores trusted friends chosen by user
-- ========================================
CREATE TABLE friends (
    friendship_id    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id          BIGINT UNSIGNED NOT NULL,
    friend_user_id   BIGINT UNSIGNED NOT NULL,
    status           ENUM('pending','accepted','blocked') NOT NULL DEFAULT 'pending',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (friendship_id),
    UNIQUE KEY uq_friends_pair (user_id, friend_user_id),
    KEY idx_friends_user (user_id),
    KEY idx_friends_friend (friend_user_id),
    CONSTRAINT fk_friends_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_friends_friend_user
        FOREIGN KEY (friend_user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT chk_no_self_friend
        CHECK (user_id <> friend_user_id)
) ENGINE=InnoDB;

-- ========================================
-- UNLOCK REQUESTS
-- Created when repeated session breaches occur
-- ========================================
CREATE TABLE unlock_requests (
    unlock_request_id   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id             BIGINT UNSIGNED NOT NULL,
    reason              VARCHAR(255) NOT NULL DEFAULT 'Repeated session limit breaches',
    status              ENUM('pending','approved','denied','expired') NOT NULL DEFAULT 'pending',
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at          DATETIME NULL,
    resolved_at         DATETIME NULL,
    PRIMARY KEY (unlock_request_id),
    KEY idx_ur_user_created (user_id, created_at),
    KEY idx_ur_status (status),
    CONSTRAINT fk_ur_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ========================================
-- UNLOCK REQUEST APPROVALS
-- One row per friend per unlock request
-- ========================================
CREATE TABLE unlock_request_approvals (
    approval_id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    unlock_request_id   BIGINT UNSIGNED NOT NULL,
    friend_user_id      BIGINT UNSIGNED NOT NULL,
    decision            ENUM('pending','approved','denied') NOT NULL DEFAULT 'pending',
    decision_time       DATETIME NULL,
    PRIMARY KEY (approval_id),
    UNIQUE KEY uq_approval_per_friend (unlock_request_id, friend_user_id),
    KEY idx_approval_request (unlock_request_id),
    KEY idx_approval_friend (friend_user_id, decision),
    CONSTRAINT fk_ura_request
        FOREIGN KEY (unlock_request_id) REFERENCES unlock_requests(unlock_request_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_ura_friend_user
        FOREIGN KEY (friend_user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ========================================
-- NOTIFICATIONS
-- Can be used for unlock request alerts
-- ========================================
CREATE TABLE notifications (
    notification_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id         BIGINT UNSIGNED NOT NULL,
    type            ENUM('unlock_request','warning','reminder','system') NOT NULL DEFAULT 'system',
    payload         JSON NULL,
    sent_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at         DATETIME NULL,
    PRIMARY KEY (notification_id),
    KEY idx_notif_user_time (user_id, sent_at),
    KEY idx_notif_unread (user_id, read_at),
    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;
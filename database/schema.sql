DROP DATABASE IF EXISTS self_regulation_app;

CREATE DATABASE self_regulation_app
	DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE self_regulation_app;

CREATE TABLE users (
  user_id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name                 VARCHAR(120) NOT NULL,
  email                VARCHAR(190) NOT NULL,
  password_hash        VARCHAR(255) NULL,
  session_limit_minutes INT UNSIGNED NOT NULL DEFAULT 15,
  daily_limit_minutes  INT UNSIGNED NOT NULL DEFAULT 60,
  locked_until         DATETIME NULL,
  created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  UNIQUE KEY uq_users_email (email),
  CONSTRAINT chk_users_limits CHECK (session_limit_minutes > 0 AND daily_limit_minutes > 0)
) ENGINE=InnoDB;

CREATE TABLE sessions (
  session_id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id            BIGINT UNSIGNED NOT NULL,
  regulated_app      VARCHAR(80) NOT NULL,
  start_time         DATETIME NOT NULL,
  end_time           DATETIME NULL,
  duration_seconds   INT UNSIGNED NOT NULL DEFAULT 0,
  breached           BOOLEAN NOT NULL DEFAULT FALSE,
  created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (session_id),
  KEY idx_sessions_user_time (user_id, start_time),
  KEY idx_sessions_breached (user_id, breached, start_time),
  CONSTRAINT fk_sessions_user
    FOREIGN KEY (user_id) REFERENCES users(user_id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT chk_session_time
    CHECK (end_time IS NULL OR end_time >= start_time)
) ENGINE=InnoDB;

CREATE TABLE challenges (
  challenge_id     BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  title            VARCHAR(120) NOT NULL,
  description      TEXT NULL,
  type             ENUM('reflection','breathing','maths','typing') NOT NULL,
  active           BOOLEAN NOT NULL DEFAULT TRUE,
  created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (challenge_id),
  KEY idx_challenges_active (active)
) ENGINE=InnoDB;

CREATE TABLE user_challenges (
  user_challenge_id  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id            BIGINT UNSIGNED NOT NULL,
  challenge_id       BIGINT UNSIGNED NOT NULL,
  result             ENUM('completed','skipped','failed') NOT NULL DEFAULT 'completed',
  completed_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_challenge_id),
  KEY idx_uc_user_time (user_id, completed_at),
  KEY idx_uc_challenge (challenge_id),
  CONSTRAINT fk_uc_user
    FOREIGN KEY (user_id) REFERENCES users(user_id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_uc_challenge
    FOREIGN KEY (challenge_id) REFERENCES challenges(challenge_id)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

SHOW TABLES;
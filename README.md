# ClearSpace

> **Reclaim your attention.** ClearSpace is an Android application that enforces healthy screen time limits by requiring users to complete focus challenges before regaining access to blocked apps.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [System Architecture](#system-architecture)
- [Tech Stack](#tech-stack)
- [Database Schema](#database-schema)
- [API Reference](#api-reference)
- [Installation & Setup](#installation--setup)
- [How It Works](#how-it-works)
- [Known Limitations](#known-limitations)
- [Future Improvements](#future-improvements)
- [Team](#team)

---

## Overview

ClearSpace combines real-time app monitoring, behavioral interruption, and gamification to build healthier digital habits. When a user exceeds their self-set time limit on a distracting app, ClearSpace locks it behind a focus challenge — making mindless scrolling a conscious, effortful choice.

**This is an academic prototype.** It is built to demonstrate UX flow and system logic, not for production deployment.

---

## Features

### 🔒 App Blocking & Monitoring
- Select apps to regulate (e.g., Instagram, TikTok, YouTube)
- Real-time foreground app tracking via Android `UsageStatsManager`
- Full-screen overlay-based blocking when time limits are exceeded
- Anti-bypass logic to prevent circumvention

### ⏱️ Session Management
- Custom time limits per session (1 minute → 8 hours)
- Live countdown with elapsed vs. remaining time display
- Automatic session lifecycle tracking (start / update / end) via backend API

### 🧠 Challenge-Based Unlock System
To regain app access, users must complete one of the following:

| Challenge | Description |
|-----------|-------------|
| Breathing Exercise | Guided inhale/exhale cycles |
| Rapid Tap | Speed-based tap challenge |
| Hold Interaction | Sustained press challenge |
| Math Problem | Simple arithmetic check |
| Random Mode | Randomly selected challenge |

### 🏆 Gamification
- Points awarded on challenge completion
- Level progression system
- Daily streak tracking
- All-time longest streak record

### 👤 User Accounts
- Registration and login via REST API
- Persistent local state via `SharedPreferences`
- Profile data: points, level, current streak, longest streak

---

## System Architecture

```
┌──────────────────────────────┐
│        Android App           │
│  (Kotlin — Frontend + Logic) │
│                              │
│  AppMonitorService           │
│  OverlayService              │
│  ChallengeActivity           │
│  DashboardActivity           │
│  LoginActivity / SignupActivity│
│  ClearSpaceStateManager      │
└──────────────┬───────────────┘
               │ Retrofit (HTTP)
               ▼
┌──────────────────────────────┐
│      Node.js Backend         │
│      (Express.js REST API)   │
│                              │
│  /users   /sessions          │
│  /challenges  /gamification  │
└──────────────┬───────────────┘
               │ mysql2
               ▼
┌──────────────────────────────┐
│      MySQL Database          │
│      (Local — Prototype)     │
└──────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Mobile | Kotlin, Android SDK |
| App Monitoring | `UsageStatsManager`, Foreground Service |
| Overlay | `TYPE_APPLICATION_OVERLAY` |
| Networking (Android) | Retrofit2 |
| Backend | Node.js, Express.js |
| Database | MySQL (`mysql2`) |
| Local State | `SharedPreferences` |

---

## Database Schema

### `users`

| Column | Type | Description |
|--------|------|-------------|
| `user_id` | `INT` AUTO_INCREMENT | Primary key |
| `user_name` | `VARCHAR(100)` | Display name |
| `email` | `VARCHAR(100)` UNIQUE | Login email |
| `password_hash` | `VARCHAR(255)` | Password (plain text — prototype only) |
| `session_limit_minutes` | `INT` DEFAULT 15 | Per-session time limit |
| `daily_limit_minutes` | `INT` DEFAULT 60 | Daily total limit |
| `points` | `INT` DEFAULT 0 | Gamification points |
| `level` | `INT` DEFAULT 1 | User level |
| `current_streak` | `INT` DEFAULT 0 | Current daily streak |
| `longest_streak` | `INT` DEFAULT 0 | All-time best streak |
| `last_streak_date` | `DATE` | Last streak activity date |

---

## API Reference

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/users/register` | Create a new user account |
| `POST` | `/users/login` | Authenticate and return user data |

### Sessions
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/sessions/start` | Begin a new app session |
| `POST` | `/sessions/end` | End the current session |
| `POST` | `/sessions/update-duration` | Update elapsed session time |

### Challenges
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/user-challenges/complete` | Record challenge completion & award points |
| `GET` | `/challenges/active` | Fetch available challenges |

### Gamification
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/users/:id/gamification` | Get points, level, and streaks |
| `GET` | `/users/:id/streak` | Get streak data only |

---

## Installation & Setup

### Prerequisites
- Node.js + npm
- MySQL
- Android Studio
- Android device or emulator (API 21+)

---

### 1. Start MySQL

```bash
# macOS (Homebrew)
brew services start mysql

# Or manually
mysql.server start
```

### 2. Create the Database

```bash
mysql -u root -p
```

```sql
CREATE DATABASE self_regulation_app;
USE self_regulation_app;
```

### 3. Create the Users Table

```sql
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(100),
    email VARCHAR(100) UNIQUE,
    password_hash VARCHAR(255),
    session_limit_minutes INT DEFAULT 15,
    daily_limit_minutes INT DEFAULT 60,
    points INT DEFAULT 0,
    level INT DEFAULT 1,
    current_streak INT DEFAULT 0,
    longest_streak INT DEFAULT 0,
    last_streak_date DATE
);
```

### 4. Start the Backend Server

```bash
npm install
npm start
```

Expected output:
```
Server is running on port 3000
Connected to Self_Regulation_App database
```

### 5. Run the Android App

1. Open the project in **Android Studio**
2. Connect a physical device or launch an emulator
3. Enable required permissions manually:
   - **Usage Access** — Settings → Apps → Special App Access → Usage Access
   - **Display Over Other Apps** — Settings → Apps → Special App Access → Appear on Top
4. Press **Run ▶**

---

## How It Works

```
1. User selects apps to block & sets a time limit
         ↓
2. AppMonitorService tracks foreground app in real-time
         ↓
3. Time limit exceeded?
         ↓
4. OverlayService launches full-screen block UI
         ↓
5. User completes a focus challenge (math, breathing, tap, etc.)
         ↓
6. Backend records completion:
   → Awards points
   → Updates level
   → Updates streak
         ↓
7. Access restored — overlay dismissed
```

---

## Known Limitations

-  Passwords stored in **plain text** (prototype only — no bcrypt)
-  Permissions must be enabled **manually** by the user
-  Overlay behavior may vary by **Android version** and OEM customizations (e.g., MIUI, One UI)
-  Backend runs **locally** — no cloud hosting

---

## Future Improvements

- [ ] Password hashing with `bcrypt`
- [ ] Cloud deployment (AWS, Firebase, or Railway)
- [ ] Push notifications for streak reminders
- [ ] Advanced analytics dashboard (screen time trends)
- [ ] Social accountability features (friend streaks)
- [ ] Multi-device sync
- [ ] Scheduled blocking windows (e.g., no TikTok after 10PM)

---

## Team

| Name | Primary Role | Secondary Role |
|------|-------------|----------------|
| **David Njoku** | Team Coordinator | Chairperson |
| **Arda Yuksel** | Backend & Architecture | Developer |
| **Sumin Chung** | Frontend Developer | UI Design |
| **Sayo Owolabi** | UX Design Lead & QA | Tester |
| **Sean K. Iroanya** | Database & Backend | Documentation |

---

## License

This project was developed as an **academic prototype** and is not intended for production use. All rights reserved by the contributing team members.

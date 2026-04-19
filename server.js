const express = require("express");
const mysql = require("mysql2");
const cors = require("cors");

const app = express();
app.use(cors());
app.use(express.json());

const db = mysql.createConnection({
  host: "localhost",
  user: "root",
  password: "SeanIroanya77",
  database: "self_regulation_app"
});

db.connect((err) => {
  if (err) {
    console.error("Error connecting to database:", err);
    return;
  }
  console.log("Connected to Self_Regulation_App database");
});

function isValidEmail(email) {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
}

function isValidPassword(password) {
  return typeof password === "string" &&
    password.length >= 8 &&
    /[A-Z]/.test(password) &&
    /[a-z]/.test(password) &&
    /\d/.test(password);
}

function formatDateOnly(dateValue) {
  if (!dateValue) return null;
  const d = new Date(dateValue);
  return d.toISOString().split("T")[0];
}

function getStreakBonus(currentStreak) {
  if (currentStreak <= 1) return 5;
  if (currentStreak === 2) return 10;
  return 15;
}

function updateUserPointsAndLevel(userId, pointsToAdd, callback) {
  const getSql = `
    SELECT points, level
    FROM users
    WHERE user_id = ?
    LIMIT 1
  `;

  db.query(getSql, [userId], (err, results) => {
    if (err) return callback(err);

    if (results.length === 0) {
      return callback(new Error("User not found"));
    }

    const currentPoints = results[0].points || 0;
    const newPoints = currentPoints + pointsToAdd;
    const newLevel = Math.floor(newPoints / 50) + 1;

    const updateSql = `
      UPDATE users
      SET points = ?, level = ?
      WHERE user_id = ?
    `;

    db.query(updateSql, [newPoints, newLevel, userId], (updateErr) => {
      if (updateErr) return callback(updateErr);

      callback(null, {
        points: newPoints,
        level: newLevel,
        points_added: pointsToAdd
      });
    });
  });
}

function updateUserStreak(userId, callback) {
  const sql = `
    SELECT user_id, current_streak, longest_streak, last_streak_date
    FROM users
    WHERE user_id = ?
    LIMIT 1
  `;

  db.query(sql, [userId], (err, results) => {
    if (err) return callback(err);

    if (results.length === 0) {
      return callback(new Error("User not found"));
    }

    const user = results[0];
    const todayStr = new Date().toISOString().split("T")[0];
    const lastDate = formatDateOnly(user.last_streak_date);

    let newCurrentStreak = user.current_streak || 0;
    let newLongestStreak = user.longest_streak || 0;

    if (lastDate === todayStr) {
      return callback(null, {
        user_id: Number(userId),
        current_streak: newCurrentStreak,
        longest_streak: newLongestStreak,
        last_streak_date: todayStr,
        already_counted_today: true
      });
    }

    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    const yesterdayStr = yesterday.toISOString().split("T")[0];

    if (!lastDate) {
      newCurrentStreak = 1;
    } else if (lastDate === yesterdayStr) {
      newCurrentStreak += 1;
    } else {
      newCurrentStreak = 1;
    }

    if (newCurrentStreak > newLongestStreak) {
      newLongestStreak = newCurrentStreak;
    }

    const updateSql = `
      UPDATE users
      SET current_streak = ?,
          longest_streak = ?,
          last_streak_date = ?
      WHERE user_id = ?
    `;

    db.query(
      updateSql,
      [newCurrentStreak, newLongestStreak, todayStr, userId],
      (updateErr) => {
        if (updateErr) return callback(updateErr);

        callback(null, {
          user_id: Number(userId),
          current_streak: newCurrentStreak,
          longest_streak: newLongestStreak,
          last_streak_date: todayStr,
          already_counted_today: false
        });
      }
    );
  });
}

app.post("/users/find-or-create", (req, res) => {
  const { email, password } = req.body;

  if (!email || !isValidEmail(email)) {
    return res.status(400).json({
      success: false,
      message: "Invalid email format"
    });
  }

  if (!password || !isValidPassword(password)) {
    return res.status(400).json({
      success: false,
      message: "Password must be at least 8 characters and include uppercase, lowercase, and a number"
    });
  }

  const findSql = `
    SELECT user_id, user_name, email
    FROM users
    WHERE email = ?
    LIMIT 1
  `;

  db.query(findSql, [email], (findErr, results) => {
    if (findErr) {
      return res.status(500).json({ success: false, error: findErr.message });
    }

    if (results.length > 0) {
      return res.json({
        success: true,
        message: "User found",
        user_id: results[0].user_id,
        user_name: results[0].user_name,
        email: results[0].email
      });
    }

    const derivedName = email.split("@")[0];

    const insertSql = `
      INSERT INTO users (user_name, email, password_hash, session_limit_minutes, daily_limit_minutes, points, level)
      VALUES (?, ?, ?, 15, 60, 0, 1)
    `;

    db.query(insertSql, [derivedName, email, password], (insertErr, insertResult) => {
      if (insertErr) {
        return res.status(500).json({ success: false, error: insertErr.message });
      }

      res.json({
        success: true,
        message: "User created",
        user_id: insertResult.insertId,
        user_name: derivedName,
        email: email
      });
    });
  });
});

app.post("/sessions/start", (req, res) => {
  const { user_id, regulated_app } = req.body;

  const sql = `
    INSERT INTO sessions (user_id, regulated_app, start_time, breached)
    VALUES (?, ?, NOW(), FALSE)
  `;

  db.query(sql, [user_id, regulated_app], (err, result) => {
    if (err) {
      return res.status(500).json({ success: false, error: err.message });
    }

    res.json({
      success: true,
      message: "Session started successfully",
      session_id: result.insertId
    });
  });
});

app.post("/sessions/update-duration", (req, res) => {
  const { user_id, regulated_app } = req.body;

  const sql = `
    UPDATE sessions
    SET duration_seconds = TIMESTAMPDIFF(SECOND, start_time, NOW())
    WHERE user_id = ?
      AND regulated_app = ?
      AND end_time IS NULL
    ORDER BY start_time DESC
    LIMIT 1
  `;

  db.query(sql, [user_id, regulated_app], (err) => {
    if (err) {
      return res.status(500).json({ success: false, error: err.message });
    }

    res.json({
      success: true,
      message: "Session duration updated"
    });
  });
});

app.post("/sessions/end", (req, res) => {
  const { user_id, regulated_app } = req.body;

  const endSessionSql = `
    UPDATE sessions
    SET end_time = NOW(),
        duration_seconds = TIMESTAMPDIFF(SECOND, start_time, NOW())
    WHERE user_id = ?
      AND regulated_app = ?
      AND end_time IS NULL
    ORDER BY start_time DESC
    LIMIT 1
  `;

  db.query(endSessionSql, [user_id, regulated_app], (err, result) => {
    if (err) {
      return res.status(500).json({ success: false, error: err.message });
    }

    if (result.affectedRows === 0) {
      return res.json({
        success: true,
        message: "No active session found to end",
        rows_affected: 0,
        streak: null,
        rewards: null
      });
    }

    updateUserStreak(user_id, (streakErr, streakData) => {
      if (streakErr) {
        return res.status(500).json({ success: false, error: streakErr.message });
      }

      let totalPointsToAdd = 0;
      let streakBonus = 0;
      const sessionPoints = 10;

      if (!streakData.already_counted_today) {
        streakBonus = getStreakBonus(streakData.current_streak);
        totalPointsToAdd = sessionPoints + streakBonus;
      } else {
        totalPointsToAdd = sessionPoints;
      }

      updateUserPointsAndLevel(user_id, totalPointsToAdd, (pointsErr, rewardsData) => {
        if (pointsErr) {
          return res.status(500).json({ success: false, error: pointsErr.message });
        }

        res.json({
          success: true,
          message: "Session ended successfully and rewards updated",
          rows_affected: result.affectedRows,
          streak: streakData,
          rewards: {
            session_points: sessionPoints,
            streak_bonus: streakBonus,
            total_added: totalPointsToAdd,
            total_points: rewardsData.points,
            level: rewardsData.level
          }
        });
      });
    });
  });
});

app.get("/challenges/active", (req, res) => {
  const sql = `
    SELECT challenge_id, title, description, type
    FROM challenges
    WHERE active = TRUE
  `;

  db.query(sql, (err, results) => {
    if (err) {
      return res.status(500).json({ success: false, error: err.message });
    }

    res.json(results);
  });
});

app.post("/user-challenges/complete", (req, res) => {
  const { user_id, challenge_id, result } = req.body;

  const sql = `
    INSERT INTO user_challenges (user_id, challenge_id, result)
    VALUES (?, ?, ?)
  `;

  db.query(sql, [user_id, challenge_id, result], (err) => {
    if (err) {
      return res.status(500).json({ success: false, error: err.message });
    }

    if (result !== "completed") {
      return res.json({
        success: true,
        message: "Challenge recorded successfully",
        streak: null,
        rewards: null
      });
    }

    updateUserStreak(user_id, (streakErr, streakData) => {
      if (streakErr) {
        return res.status(500).json({ success: false, error: streakErr.message });
      }

      let totalPointsToAdd = 0;
      let streakBonus = 0;
      const challengePoints = 10;

      if (!streakData.already_counted_today) {
        streakBonus = getStreakBonus(streakData.current_streak);
        totalPointsToAdd = challengePoints + streakBonus;
      } else {
        totalPointsToAdd = challengePoints;
      }

      updateUserPointsAndLevel(user_id, totalPointsToAdd, (pointsErr, rewardsData) => {
        if (pointsErr) {
          return res.status(500).json({ success: false, error: pointsErr.message });
        }

        res.json({
          success: true,
          message: "Challenge completed successfully",
          streak: streakData,
          rewards: {
            challenge_points: challengePoints,
            streak_bonus: streakBonus,
            total_added: totalPointsToAdd,
            total_points: rewardsData.points,
            level: rewardsData.level
          }
        });
      });
    });
  });
});

app.get("/users/:userId/streak", (req, res) => {
  const userId = req.params.userId;

  const sql = `
    SELECT user_id, current_streak, longest_streak, last_streak_date
    FROM users
    WHERE user_id = ?
    LIMIT 1
  `;

  db.query(sql, [userId], (err, results) => {
    if (err) {
      return res.status(500).json({ success: false, error: err.message });
    }

    if (results.length === 0) {
      return res.status(404).json({ success: false, message: "User not found" });
    }

    const streak = results[0];

    res.json({
      success: true,
      streak: {
        user_id: streak.user_id,
        current_streak: streak.current_streak,
        longest_streak: streak.longest_streak,
        last_streak_date: formatDateOnly(streak.last_streak_date)
      }
    });
  });
});

app.get("/users/:userId/gamification", (req, res) => {
  const userId = req.params.userId;

  const sql = `
    SELECT user_id, user_name, points, level, current_streak, longest_streak, last_streak_date
    FROM users
    WHERE user_id = ?
    LIMIT 1
  `;

  db.query(sql, [userId], (err, results) => {
    if (err) {
      return res.status(500).json({ success: false, error: err.message });
    }

    if (results.length === 0) {
      return res.status(404).json({ success: false, message: "User not found" });
    }

    const user = results[0];

    res.json({
      success: true,
      gamification: {
        user_id: user.user_id,
        user_name: user.user_name,
        points: user.points,
        level: user.level,
        current_streak: user.current_streak,
        longest_streak: user.longest_streak,
        last_streak_date: formatDateOnly(user.last_streak_date)
      }
    });
  });
});

app.listen(3000, "0.0.0.0", () => {
  console.log("Server is running on port 3000");
});
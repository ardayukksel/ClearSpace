const express = require("express");
const mysql = require("mysql2");
const cors = require("cors");

const app = express();
app.use(cors());
app.use(express.json());

const db = mysql.createConnection({
  host: "127.0.0.1",
  port: 3306,
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
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function isValidPassword(password) {
  return (
    typeof password === "string" &&
    password.length >= 8 &&
    /[A-Z]/.test(password) &&
    /[a-z]/.test(password) &&
    /\d/.test(password)
  );
}

// ================= REGISTER =================
app.post("/users/register", (req, res) => {
  const { email, password, user_name } = req.body;

  if (!email || !isValidEmail(email)) {
    return res.status(400).json({ success: false, message: "Invalid email" });
  }

  if (!password || !isValidPassword(password)) {
    return res.status(400).json({ success: false, message: "Invalid password" });
  }

  const checkSql = `SELECT user_id FROM users WHERE email = ? LIMIT 1`;

  db.query(checkSql, [email], (err, results) => {
    if (err) {
      return res.status(500).json({ success: false, error: err.message });
    }

    if (results.length > 0) {
      return res.status(400).json({ success: false, message: "User already exists" });
    }

    const finalName =
      user_name && user_name.trim() !== ""
        ? user_name.trim()
        : email.split("@")[0];

    const insertSql = `
      INSERT INTO users (
        user_name,
        email,
        password_hash,
        session_limit_minutes,
        daily_limit_minutes,
        points,
        level,
        current_streak,
        longest_streak,
        last_streak_date
      )
      VALUES (?, ?, ?, 15, 60, 0, 1, 0, 0, NULL)
    `;

    db.query(insertSql, [finalName, email, password], (insertErr, result) => {
      if (insertErr) {
        return res.status(500).json({ success: false, error: insertErr.message });
      }

      res.json({
        success: true,
        message: "User registered",
        user_id: result.insertId,
        user_name: finalName,
        email: email,
        session_limit_minutes: 15,
        daily_limit_minutes: 60,
        points: 0,
        level: 1,
        current_streak: 0,
        longest_streak: 0,
        last_streak_date: null
      });
    });
  });
});

// ================= LOGIN =================
app.post("/users/login", (req, res) => {
  const { email, password } = req.body;

  const sql = `
    SELECT
      user_id,
      user_name,
      email,
      password_hash,
      session_limit_minutes,
      daily_limit_minutes,
      points,
      level,
      current_streak,
      longest_streak,
      last_streak_date
    FROM users
    WHERE email = ?
    LIMIT 1
  `;

  db.query(sql, [email], (err, results) => {
    if (err) {
      return res.status(500).json({ success: false, error: err.message });
    }

    if (results.length === 0) {
      return res.status(401).json({ success: false, message: "User not found" });
    }

    const user = results[0];

    if ((user.password_hash || "").trim() !== (password || "").trim()) {
      return res.status(401).json({ success: false, message: "Wrong password" });
    }

    res.json({
      success: true,
      message: "Login successful",
      user_id: user.user_id,
      user_name: user.user_name,
      email: user.email,
      session_limit_minutes: user.session_limit_minutes,
      daily_limit_minutes: user.daily_limit_minutes,
      points: user.points,
      level: user.level,
      current_streak: user.current_streak,
      longest_streak: user.longest_streak,
      last_streak_date: user.last_streak_date
    });
  });
});

// ================= USER PROFILE =================
app.get("/users/:userId", (req, res) => {
  const { userId } = req.params;

  const sql = `
    SELECT
      user_id,
      user_name,
      email,
      session_limit_minutes,
      daily_limit_minutes,
      points,
      level,
      current_streak,
      longest_streak,
      last_streak_date
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
      user_id: user.user_id,
      user_name: user.user_name,
      email: user.email,
      session_limit_minutes: user.session_limit_minutes,
      daily_limit_minutes: user.daily_limit_minutes,
      points: user.points,
      level: user.level,
      current_streak: user.current_streak,
      longest_streak: user.longest_streak,
      last_streak_date: user.last_streak_date
    });
  });
});

// ================= USER GAMIFICATION =================
app.get("/users/:userId/gamification", (req, res) => {
  const { userId } = req.params;

  const sql = `
    SELECT
      points,
      level,
      current_streak,
      longest_streak,
      last_streak_date
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
        points: user.points ?? 0,
        level: user.level ?? 1,
        current_streak: user.current_streak ?? 0,
        longest_streak: user.longest_streak ?? 0,
        last_streak_date: user.last_streak_date
      }
    });
  });
});

app.listen(3000, "0.0.0.0", () => {
  console.log("Server is running on port 3000");
});
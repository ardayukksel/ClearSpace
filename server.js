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

app.post("/users/find-or-create", (req, res) => {
  const { email } = req.body;

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
      INSERT INTO users (user_name, email, session_limit_minutes, daily_limit_minutes)
      VALUES (?, ?, 15, 60)
    `;

    db.query(insertSql, [derivedName, email], (insertErr, insertResult) => {
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

  const sql = `
    UPDATE sessions
    SET end_time = NOW(),
        duration_seconds = TIMESTAMPDIFF(SECOND, start_time, NOW())
    WHERE user_id = ?
      AND regulated_app = ?
      AND end_time IS NULL
    ORDER BY start_time DESC
    LIMIT 1
  `;

  db.query(sql, [user_id, regulated_app], (err, result) => {
    if (err) {
      return res.status(500).json({ success: false, error: err.message });
    }

    res.json({
      success: true,
      message: "Session ended successfully",
      rows_affected: result.affectedRows
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

    res.json({
      success: true,
      message: "Challenge completed successfully"
    });
  });
});

app.listen(3000, "0.0.0.0", () => {
  console.log("Server is running on port 3000");
});
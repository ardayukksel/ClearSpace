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

app.post("/sessions/start", (req, res) => {
    const { user_id, regulated_app } = req.body;

    const sql = 'INSERT INTO sessions (user_id, regulated_app, start_time, breached) VALUES (?, ?, NOW(), FALSE)';

    db.query(sql, [user_id, regulated_app], (err, result) => {
        if (err) {
            return res.status(500).json({ success: false, error: err.message });
        }
        res.json({ success: true, message: "Session started successfully", session_id: result.insertId });
    });
});

app.get("/challenges/active", (req, res) => {
    const sql = 'SELECT challenge_id, title, description, type FROM challenges WHERE active = TRUE';

    db.query(sql, (err, results) => {
        if (err) {
            return res.status(500).json({ success: false, error: err.message });
        }

        res.json(results);
    });
});

app.post("/user-challenges/complete", (req, res) => {
    const { user_id, challenge_id, result } = req.body;

    const sql = 'INSERT INTO user_challenges (user_id, challenge_id, result) VALUES (?, ?, ?)';

    db.query(sql, [user_id, challenge_id, result], (err, dbResult) => {
        if (err) {
            return res.status(500).json({ success: false, error: err.message });
        }
        res.json({ success: true, message: "Challenge completed successfully" });
    });
});

app.listen(3000, () => {
  console.log("Server is running on port 3000");
});
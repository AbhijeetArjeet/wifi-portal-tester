// server.js — Express backend for Render (utilizes ES Module syntax to match package.json)
import express from 'express';
import cors from 'cors';

const app = express();

app.use(cors());
app.use(express.json());

// In-Memory Leaderboard (Starts empty)
let campusLeaderboard = [];

// GET: Fetch live campus rankings
app.get("/api/leaderboard", (req, res) => {
  res.json(campusLeaderboard);
});

// POST: Submit new speed test result from any student/college
app.post("/api/submit-speed", (req, res) => {
  const { college, speed, ping } = req.body;
  if (college && speed) {
    const newSpeed = parseFloat(speed);
    const newPing = parseInt(ping) || 15;
    
    // Insert new submission
    campusLeaderboard.push({
      college: String(college).substring(0, 40),
      speed: newSpeed,
      ping: newPing
    });

    // Re-rank by highest download speed
    campusLeaderboard.sort((a, b) => b.speed - a.speed);
    campusLeaderboard = campusLeaderboard.slice(0, 10);
    campusLeaderboard.forEach((item, idx) => item.rank = idx + 1);
  }
  res.json({ success: true, leaderboard: campusLeaderboard });
});

// Root Healthcheck Endpoint
app.get("/", (req, res) => {
  res.send("📶 WiFi Portal Tester Render Backend Running!");
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server listening on port ${PORT}`);
});

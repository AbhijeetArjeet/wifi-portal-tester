// api/submit-speed.js — Vercel Serverless Function (POST /api/submit-speed)
// Accepts speed test submissions globally; stores in Upstash Redis.

const MAX_SIZE = 50;

async function getFromRedis() {
  const url = process.env.UPSTASH_REDIS_REST_KV_REST_API_URL || 
              process.env.KV_REST_API_URL || 
              process.env.UPSTASH_REDIS_REST_URL;

  const token = process.env.UPSTASH_REDIS_REST_KV_REST_API_TOKEN || 
                process.env.KV_REST_API_TOKEN || 
                process.env.UPSTASH_REDIS_REST_TOKEN;

  if (!url || !token) return null;
  const res = await fetch(`${url}/get/wifi_leaderboard`, { headers: { Authorization: `Bearer ${token}` } });
  const data = await res.json();
  return data.result ? JSON.parse(data.result) : null;
}

async function saveToRedis(leaderboard) {
  const url = process.env.UPSTASH_REDIS_REST_KV_REST_API_URL || 
              process.env.KV_REST_API_URL || 
              process.env.UPSTASH_REDIS_REST_URL;

  const token = process.env.UPSTASH_REDIS_REST_KV_REST_API_TOKEN || 
                process.env.KV_REST_API_TOKEN || 
                process.env.UPSTASH_REDIS_REST_TOKEN;

  if (!url || !token) return;
  await fetch(`${url}/set/wifi_leaderboard`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify(JSON.stringify(leaderboard)),
  });
}

export default async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') return res.status(200).end();
  if (req.method !== 'POST') return res.status(405).json({ error: 'Method not allowed' });

  const { college, speed, ping, upload, jitter, country } = req.body || {};

  if (!college || typeof speed !== 'number' || speed <= 0 || speed > 10000) {
    return res.status(400).json({ error: 'Invalid data' });
  }

  let leaderboard;
  try {
    leaderboard = await getFromRedis() || [];
  } catch {
    leaderboard = [];
  }

  const newEntry = {
    college: String(college).substring(0, 60).replace(/[<>]/g, ''),
    speed: parseFloat(parseFloat(speed).toFixed(2)),
    ping: parseInt(ping) || 20,
    upload: parseFloat(upload) || 0,
    jitter: parseFloat(jitter) || 0,
    country: String(country || 'IN').substring(0, 3),
    submittedAt: new Date().toISOString(),
  };

  leaderboard.push(newEntry);
  leaderboard.sort((a, b) => b.speed - a.speed);
  leaderboard = leaderboard.slice(0, MAX_SIZE);
  leaderboard.forEach((item, idx) => { item.rank = idx + 1; });

  try { await saveToRedis(leaderboard); } catch(e) { console.error('Redis write error:', e); }

  return res.status(200).json({ success: true, leaderboard });
}

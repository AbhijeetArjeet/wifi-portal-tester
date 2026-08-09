// api/submit-speed.js — Vercel Serverless Function (POST /api/submit-speed)
// Accepts speed test submissions globally; stores in Upstash Redis.
//
// SETUP: In Vercel dashboard → Storage → Create Redis → link to project.
// Env vars auto-added: KV_REST_API_URL, KV_REST_API_TOKEN

const SEED_DATA = [
  { rank: 1, college: 'KL University', speed: 142.5, ping: 12, country: 'IN' },
  { rank: 2, college: 'SRM IST', speed: 118.2, ping: 15, country: 'IN' },
  { rank: 3, college: 'VIT Vellore', speed: 95.8, ping: 18, country: 'IN' },
  { rank: 4, college: 'Manipal University', speed: 84.4, ping: 22, country: 'IN' },
  { rank: 5, college: 'Amity University', speed: 76.1, ping: 25, country: 'IN' },
];

const MAX_SIZE = 50;

async function getFromRedis() {
  const url = process.env.KV_REST_API_URL || process.env.UPSTASH_REDIS_REST_URL;
  const token = process.env.KV_REST_API_TOKEN || process.env.UPSTASH_REDIS_REST_TOKEN;
  if (!url || !token) return null;
  const res = await fetch(`${url}/get/wifi_leaderboard`, { headers: { Authorization: `Bearer ${token}` } });
  const data = await res.json();
  return data.result ? JSON.parse(data.result) : null;
}

async function saveToRedis(leaderboard) {
  const url = process.env.KV_REST_API_URL || process.env.UPSTASH_REDIS_REST_URL;
  const token = process.env.KV_REST_API_TOKEN || process.env.UPSTASH_REDIS_REST_TOKEN;
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
    leaderboard = await getFromRedis() || [...SEED_DATA];
  } catch {
    leaderboard = [...SEED_DATA];
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

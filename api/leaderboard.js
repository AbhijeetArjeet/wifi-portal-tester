// api/leaderboard.js — Vercel Serverless Function (GET /api/leaderboard)
// Uses Upstash Redis (new Vercel KV) for persistent global storage.
// Fallback to seed data if Redis not configured.
// 
// SETUP: In Vercel dashboard → Storage → Create Redis → Copy env vars to project:
//   KV_REST_API_URL + KV_REST_API_TOKEN (auto-added when linked in Vercel)

const SEED_DATA = [
  { rank: 1, college: 'KL University', speed: 142.5, ping: 12, country: 'IN' },
  { rank: 2, college: 'SRM IST', speed: 118.2, ping: 15, country: 'IN' },
  { rank: 3, college: 'VIT Vellore', speed: 95.8, ping: 18, country: 'IN' },
  { rank: 4, college: 'Manipal University', speed: 84.4, ping: 22, country: 'IN' },
  { rank: 5, college: 'Amity University', speed: 76.1, ping: 25, country: 'IN' },
];

async function getFromRedis() {
  const url = process.env.KV_REST_API_URL || process.env.UPSTASH_REDIS_REST_URL;
  const token = process.env.KV_REST_API_TOKEN || process.env.UPSTASH_REDIS_REST_TOKEN;
  if (!url || !token) throw new Error('Redis not configured');

  const res = await fetch(`${url}/get/wifi_leaderboard`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const data = await res.json();
  if (data.result) {
    return JSON.parse(data.result);
  }
  return null;
}

export default async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') return res.status(200).end();
  if (req.method !== 'GET') return res.status(405).json({ error: 'Method not allowed' });

  try {
    const leaderboard = await getFromRedis();
    return res.status(200).json(leaderboard || SEED_DATA);
  } catch (err) {
    // Redis not configured or error — return seed data
    return res.status(200).json(SEED_DATA);
  }
}

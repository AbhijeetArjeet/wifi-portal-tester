// api/leaderboard.js — Vercel Serverless Function (GET /api/leaderboard)
// Uses Upstash Redis for persistent global storage.
// Empty leaderboard by default if not configured.

async function getFromRedis() {
  const url = process.env.UPSTASH_REDIS_REST_KV_REST_API_URL || 
              process.env.KV_REST_API_URL || 
              process.env.UPSTASH_REDIS_REST_URL;

  const token = process.env.UPSTASH_REDIS_REST_KV_REST_API_TOKEN || 
                process.env.KV_REST_API_TOKEN || 
                process.env.UPSTASH_REDIS_REST_TOKEN;

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
    return res.status(200).json(leaderboard || []);
  } catch (err) {
    // Return empty leaderboard when database not configured
    return res.status(200).json([]);
  }
}

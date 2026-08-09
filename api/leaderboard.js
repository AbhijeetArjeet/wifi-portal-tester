// api/leaderboard.js — Vercel Serverless Function (GET /api/leaderboard)
// Uses Upstash Redis for persistent global storage.
// Empty leaderboard by default if not configured.

function getRedisCreds() {
  const url = process.env.UPSTASH_REDIS_REST_KV_REST_API_URL ||
              process.env.KV_REST_API_URL ||
              process.env.UPSTASH_REDIS_REST_URL;

  const token = process.env.UPSTASH_REDIS_REST_KV_REST_API_TOKEN ||
                process.env.KV_REST_API_TOKEN ||
                process.env.UPSTASH_REDIS_REST_TOKEN;

  return { url, token };
}

// Sends a single Redis command as a JSON array to the Upstash REST endpoint.
// This is the format Upstash recommends for values that may contain JSON /
// special characters, since it avoids URL path-encoding issues entirely.
async function redisCommand(command) {
  const { url, token } = getRedisCreds();
  if (!url || !token) throw new Error('Redis not configured');

  const res = await fetch(url, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(command),
  });

  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(`Upstash error ${res.status}: ${text}`);
  }

  return res.json(); // { result: ... } or { error: ... }
}

async function getLeaderboardFromRedis() {
  const data = await redisCommand(['GET', 'wifi_leaderboard']);
  if (data.error) throw new Error(data.error);
  return data.result ? JSON.parse(data.result) : null;
}

export default async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') return res.status(200).end();
  if (req.method !== 'GET') return res.status(405).json({ error: 'Method not allowed' });

  try {
    const leaderboard = await getLeaderboardFromRedis();
    return res.status(200).json(leaderboard || []);
  } catch (err) {
    // Return empty leaderboard when database not configured or on error
    console.error('Redis read error:', err);
    return res.status(200).json([]);
  }
}

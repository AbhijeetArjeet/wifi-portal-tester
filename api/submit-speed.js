// api/submit-speed.js — Vercel Serverless Function (POST /api/submit-speed)
// Accepts speed test submissions globally; stores in Upstash Redis.

const MAX_SIZE = 50;

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
  if (!url || !token) return null;

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
  if (!data || data.error || !data.result) return null;
  let parsed;
  try {
    parsed = JSON.parse(data.result);
  } catch {
    return null;
  }
  // Guard against stale/corrupted data (e.g. from a prior double-encoding
  // bug) so a bad value in Redis can never crash the handler. Any non-array
  // result is treated as "no data yet" and gets overwritten on next save.
  return Array.isArray(parsed) ? parsed : null;
}

async function saveLeaderboardToRedis(leaderboard) {
  // IMPORTANT: stringify the leaderboard exactly once. The old code did
  // JSON.stringify(JSON.stringify(leaderboard)) which double-encoded the
  // value, so GET requests could no longer parse it back into an array.
  await redisCommand(['SET', 'wifi_leaderboard', JSON.stringify(leaderboard)]);
}

export default async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') return res.status(200).end();
  if (req.method !== 'POST') return res.status(405).json({ error: 'Method not allowed' });

  try {
    const { college, speed, ping, upload, jitter, country } = req.body || {};

    if (!college || typeof speed !== 'number' || speed <= 0 || speed > 10000) {
      return res.status(400).json({ error: 'Invalid data' });
    }

    let leaderboard;
    try {
      leaderboard = (await getLeaderboardFromRedis()) || [];
    } catch (e) {
      console.error('Redis read error:', e);
      leaderboard = [];
    }
    // Belt-and-suspenders: even if getLeaderboardFromRedis somehow returns
    // something odd, never let a non-array reach .push()/.sort()/.slice().
    if (!Array.isArray(leaderboard)) leaderboard = [];

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

    try {
      await saveLeaderboardToRedis(leaderboard);
    } catch (e) {
      console.error('Redis write error:', e);
    }

    return res.status(200).json({ success: true, leaderboard });
  } catch (err) {
    console.error('submit-speed handler error:', err);
    return res.status(500).json({ error: 'Internal error', message: err.message });
  }
}

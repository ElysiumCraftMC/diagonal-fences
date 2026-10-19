// Vanilla 1.19.2 client (mineflayer, vanilla block-state table from minecraft-data) joins a server and records
// what it decodes at the probe positions. usage: node probe.js <out.json>
const mineflayer = require('mineflayer'); const fs = require('fs'); const { Vec3 } = require('vec3');
const bot = mineflayer.createBot({ host: '127.0.0.1', port: 25565, username: 'probe', version: '1.19.2', auth: 'offline' });
bot.on('error', e => { console.error('ERR', e.message); });
bot.on('kicked', r => { console.error('KICKED', r); process.exit(2); });
bot.once('spawn', async () => {
  const out = {};
  const deadline = Date.now() + 40000;
  const want = [];
  for (const y of [150, 152]) for (let x = 96; x < 112; x++) for (let z = 96; z < 112; z++) want.push(new Vec3(x, y, z));
  while (Date.now() < deadline && want.some(p => !bot.blockAt(p))) await new Promise(r => setTimeout(r, 500));
  for (const p of want) {
    const b = bot.blockAt(p);
    if (!b) { out[`${p.x},${p.y},${p.z}`] = null; continue; }
    const props = b.getProperties();
    const ps = Object.keys(props).sort().map(k => `${k}=${props[k]}`).join(',');
    out[`${p.x},${p.y},${p.z}`] = { stateId: b.stateId, state: 'minecraft:' + b.name + (ps ? `[${ps}]` : '') };
  }
  fs.writeFileSync(process.argv[2], JSON.stringify(out));
  console.log('probed', Object.keys(out).length, 'positions');
  bot.quit(); setTimeout(() => process.exit(0), 500);
});

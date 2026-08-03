#!/usr/bin/env node
/**
 * 从材料表(app_game_item, MATERIAL)读取物品，检测缺失的 img/items 图标并生成占位图 + 更新 SQL。
 *
 * 用法:
 *   node tools/generate-missing-item-icons.js          # 仅检测
 *   node tools/generate-missing-item-icons.js --generate  # 生成缺失 PNG + 写 SQL
 *   node tools/generate-missing-item-icons.js --generate --apply  # 并执行 SQL 更新 icon
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const ROOT = path.join(__dirname, '..');
const STATIC_DIR = path.join(ROOT, 'src/main/resources/static');
const ITEMS_DIR = path.join(STATIC_DIR, 'img/items');
const SQL_OUT = path.join(ROOT, 'src/main/resources/sql/update_missing_item_icons.sql');

const MYSQL = process.env.MYSQL_BIN || 'C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe';
const DB = process.env.GAME_DB || 'game2';
const DB_USER = process.env.GAME_DB_USER || 'root';
const DB_PASS = process.env.GAME_DB_PASS || '123456';

const GENERATE = process.argv.includes('--generate');
const APPLY = process.argv.includes('--apply');
const ALL_ITEMS = process.argv.includes('--all');
const ICON_SIZE = 1024;

async function loadSharp() {
  try {
    return require('sharp');
  } catch (e) {
    execSync('npm install sharp --no-save --prefix "' + __dirname + '"', { stdio: 'inherit' });
    return require(path.join(__dirname, 'node_modules', 'sharp'));
  }
}

function codeToFileName(code) {
  return String(code || 'item')
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '') + '.png';
}

function isImageIcon(icon) {
  if (!icon) return false;
  const s = String(icon);
  return s.startsWith('/') || /^https?:\/\//i.test(s) || /\.(png|jpg|jpeg|webp|gif|svg)$/i.test(s);
}

function resolveIconPath(icon) {
  if (!isImageIcon(icon)) return null;
  const rel = icon.replace(/^\//, '').replace(/\//g, path.sep);
  return path.join(STATIC_DIR, rel);
}

function queryItems() {
  const sql = ALL_ITEMS
    ? 'SELECT id, code, name, icon, item_tags FROM app_game_item ORDER BY sort, id'
    : "SELECT id, code, name, icon, item_tags FROM app_game_item WHERE FIND_IN_SET('MATERIAL', REPLACE(item_tags,' ', '')) OR item_tags = 'MATERIAL' ORDER BY sort, id";
  const cmd = `"${MYSQL}" -u${DB_USER} -p${DB_PASS} --default-character-set=utf8mb4 ${DB} -N -e "${sql}"`;
  const out = execSync(cmd, { encoding: 'utf8', maxBuffer: 10 * 1024 * 1024 });
  return out.trim().split('\n').filter(Boolean).map((line) => {
    const [id, code, name, icon, tags] = line.split('\t');
    return { id, code, name, icon: icon || '', tags: tags || '' };
  });
}

function pickPalette(code, name) {
  const key = (code + name).toLowerCase();
  const palettes = [
    { bg: '#241a3d', a: '#7cff6b', b: '#2ecc71' },
    { bg: '#1f1835', a: '#ffd166', b: '#f4a261' },
    { bg: '#22182e', a: '#ff6b9d', b: '#c44569' },
    { bg: '#1a2438', a: '#54a0ff', b: '#2e86de' },
    { bg: '#2a1a1a', a: '#ff9f43', b: '#ee5a24' },
    { bg: '#1e2a24', a: '#1dd1a1', b: '#10ac84' },
    { bg: '#2b2030', a: '#a29bfe', b: '#6c5ce7' },
    { bg: '#252018', a: '#dfe6e9', b: '#b2bec3' }
  ];
  let hash = 0;
  for (let i = 0; i < key.length; i++) hash = (hash * 31 + key.charCodeAt(i)) >>> 0;
  return palettes[hash % palettes.length];
}

function buildSvg(name, code, palette) {
  const label = (name || code || '?').slice(0, 4);
  return `<svg width="1024" height="1024" xmlns="http://www.w3.org/2000/svg">
  <defs>
    <radialGradient id="bg" cx="50%" cy="42%" r="70%">
      <stop offset="0%" stop-color="#3d2f5c"/>
      <stop offset="100%" stop-color="${palette.bg}"/>
    </radialGradient>
    <radialGradient id="orb" cx="35%" cy="30%" r="70%">
      <stop offset="0%" stop-color="${palette.a}"/>
      <stop offset="100%" stop-color="${palette.b}"/>
    </radialGradient>
    <filter id="glow" x="-20%" y="-20%" width="140%" height="140%">
      <feGaussianBlur stdDeviation="8" result="blur"/>
      <feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge>
    </filter>
  </defs>
  <rect width="1024" height="1024" fill="url(#bg)"/>
  <ellipse cx="512" cy="560" rx="290" ry="70" fill="#000" opacity="0.25"/>
  <circle cx="512" cy="470" r="250" fill="url(#orb)" filter="url(#glow)"/>
  <ellipse cx="430" cy="390" rx="90" ry="55" fill="#fff" opacity="0.22"/>
  <text x="512" y="820" text-anchor="middle" font-size="72" font-family="Segoe UI, Microsoft YaHei, sans-serif" fill="#f5f7ff" opacity="0.85">${label}</text>
</svg>`;
}

async function generateIcon(sharp, item, outPath) {
  const palette = pickPalette(item.code, item.name);
  const svg = buildSvg(item.name, item.code, palette);
  const tmp = outPath + '.tmp.png';
  await sharp(Buffer.from(svg))
    .resize(ICON_SIZE, ICON_SIZE, { fit: 'cover', position: 'centre' })
    .png()
    .toFile(tmp);
  fs.renameSync(tmp, outPath);
}

function analyze(items) {
  const missing = [];
  const ok = [];
  for (const item of items) {
    const expectedFile = codeToFileName(item.code);
    const expectedPath = path.join(ITEMS_DIR, expectedFile);
    const expectedIcon = '/img/items/' + expectedFile;
    let iconPath = resolveIconPath(item.icon);
    let reason = '';

    if (!isImageIcon(item.icon)) {
      reason = 'icon_not_image';
    } else if (!iconPath || !fs.existsSync(iconPath)) {
      reason = 'file_missing';
      iconPath = expectedPath;
    }

    if (reason) {
      missing.push({
        ...item,
        reason,
        expectedIcon,
        outputPath: expectedPath
      });
    } else {
      ok.push({ ...item, iconPath });
    }
  }
  return { missing, ok };
}

function writeSql(updates) {
  const lines = ['SET NAMES utf8mb4;', ''];
  for (const u of updates) {
    lines.push(`UPDATE app_game_item SET icon = '${u.expectedIcon}' WHERE id = '${u.id}';`);
  }
  lines.push('');
  fs.writeFileSync(SQL_OUT, lines.join('\n'), 'utf8');
  return SQL_OUT;
}

function applySql() {
  const cmd = `"${MYSQL}" -u${DB_USER} -p${DB_PASS} --default-character-set=utf8mb4 ${DB} < "${SQL_OUT}"`;
  execSync(cmd, { stdio: 'inherit', shell: true });
}

async function main() {
  if (!fs.existsSync(ITEMS_DIR)) fs.mkdirSync(ITEMS_DIR, { recursive: true });

  let items;
  try {
    items = queryItems();
  } catch (err) {
    console.error('读取材料表失败，请确认 MySQL 可连接:', err.message);
    process.exit(1);
  }

  const { missing, ok } = analyze(items);

  console.log(`\n材料/物品检测 (${ALL_ITEMS ? '全部物品' : '仅 MATERIAL'})`);
  console.log(`  已有素材: ${ok.length}`);
  console.log(`  缺失素材: ${missing.length}\n`);

  if (ok.length) {
    console.log('--- 已有 ---');
    ok.forEach((x) => console.log(`  [OK] ${x.code}\t${x.name}\t${x.icon}`));
  }

  if (!missing.length) {
    console.log('\n没有缺失素材。');
    return;
  }

  console.log('\n--- 缺失 ---');
  missing.forEach((x) => {
    console.log(`  [${x.reason}] ${x.code}\t${x.name}\t当前=${x.icon || '(空)'} -> ${x.expectedIcon}`);
  });

  if (!GENERATE) {
    console.log('\n运行 node tools/generate-missing-item-icons.js --generate 生成 PNG 并写 SQL');
    return;
  }

  const sharp = await loadSharp();
  for (const item of missing) {
    await generateIcon(sharp, item, item.outputPath);
    console.log('  生成:', path.relative(ROOT, item.outputPath));
  }

  const sqlFile = writeSql(missing);
  console.log('\n已写入 SQL:', path.relative(ROOT, sqlFile));

  if (APPLY) {
    applySql();
    console.log('已更新数据库 icon 字段');
  } else {
    console.log('加 --apply 可自动执行 SQL 更新');
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});

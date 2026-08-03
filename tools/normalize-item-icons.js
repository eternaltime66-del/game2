#!/usr/bin/env node
/**
 * 将 static/img/items 下所有 PNG 统一为 1024×1024（透明边距 contain 缩放）。
 *
 * 用法: node tools/normalize-item-icons.js
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const ICON_SIZE = 1024;
const ITEMS_DIR = path.join(__dirname, '../src/main/resources/static/img/items');

async function loadSharp() {
  try {
    return require('sharp');
  } catch (e) {
    execSync('npm install sharp --no-save --prefix "' + __dirname + '"', { stdio: 'inherit' });
    return require(path.join(__dirname, 'node_modules', 'sharp'));
  }
}

async function normalizeFile(sharp, filePath) {
  const meta = await sharp(filePath).metadata();
  if (meta.width === ICON_SIZE && meta.height === ICON_SIZE) {
    return { changed: false, from: `${meta.width}x${meta.height}` };
  }
  const tmp = filePath + '.tmp.png';
  // cover：铺满正方形，避免横版素材 contain 后上下留白
  await sharp(filePath)
    .resize(ICON_SIZE, ICON_SIZE, {
      fit: 'cover',
      position: 'centre'
    })
    .png()
    .toFile(tmp);
  fs.renameSync(tmp, filePath);
  return { changed: true, from: `${meta.width}x${meta.height}`, to: `${ICON_SIZE}x${ICON_SIZE}` };
}

async function main() {
  if (!fs.existsSync(ITEMS_DIR)) {
    console.error('目录不存在:', ITEMS_DIR);
    process.exit(1);
  }
  const sharp = await loadSharp();
  const files = fs.readdirSync(ITEMS_DIR).filter((f) => f.endsWith('.png')).sort();
  let changed = 0;
  for (const file of files) {
    const fp = path.join(ITEMS_DIR, file);
    const result = await normalizeFile(sharp, fp);
    if (result.changed) {
      changed++;
      console.log(`[FIX] ${file}\t${result.from} -> ${result.to}`);
    } else {
      console.log(`[OK]  ${file}\t${result.from}`);
    }
  }
  console.log(`\n完成：${changed} 个文件已调整，标准尺寸 ${ICON_SIZE}x${ICON_SIZE}`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});

const fs = require('fs');
const path = require('path');

async function loadSharp() {
  try {
    return require('sharp');
  } catch (e) {
    const { execSync } = require('child_process');
    execSync('npm install sharp --no-save --prefix "' + __dirname + '"', { stdio: 'inherit' });
    return require(path.join(__dirname, 'node_modules', 'sharp'));
  }
}

const NAMES = [
  'weapon-sword-short',
  'weapon-bow',
  'weapon-dagger-dark',
  'weapon-shield',
  'item-scroll',
  'prop-shrine',
  'item-compass'
];

const INPUT = path.join(__dirname, '../src/main/resources/static/img/weapon-spritesheet.png');
const OUTPUT_DIR = path.join(__dirname, '../src/main/resources/static/img/weapons');

function sortRegions(a, b) {
  const rowA = Math.floor(a.cy / 120);
  const rowB = Math.floor(b.cy / 120);
  if (rowA !== rowB) return rowA - rowB;
  return a.cx - b.cx;
}

function findRegions(data, width, height) {
  const visited = new Uint8Array(width * height);
  const regions = [];

  function isSolid(idx) {
    const r = data[idx];
    const g = data[idx + 1];
    const b = data[idx + 2];
    const a = data[idx + 3];
    return a > 16 && (r + g + b) > 40;
  }

  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      const start = (y * width + x) * 4;
      if (visited[y * width + x] || !isSolid(start)) continue;

      let minX = x;
      let maxX = x;
      let minY = y;
      let maxY = y;
      let sumX = 0;
      let sumY = 0;
      let count = 0;
      const stack = [[x, y]];
      visited[y * width + x] = 1;

      while (stack.length) {
        const [cx, cy] = stack.pop();
        minX = Math.min(minX, cx);
        maxX = Math.max(maxX, cx);
        minY = Math.min(minY, cy);
        maxY = Math.max(maxY, cy);
        sumX += cx;
        sumY += cy;
        count += 1;

        const neighbors = [
          [cx + 1, cy], [cx - 1, cy], [cx, cy + 1], [cx, cy - 1]
        ];
        for (const [nx, ny] of neighbors) {
          if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue;
          const nIdx = ny * width + nx;
          if (visited[nIdx]) continue;
          const nStart = nIdx * 4;
          if (!isSolid(nStart)) continue;
          visited[nIdx] = 1;
          stack.push([nx, ny]);
        }
      }

      const boxW = maxX - minX + 1;
      const boxH = maxY - minY + 1;
      if (count < 500 || boxW < 20 || boxH < 20) continue;

      regions.push({
        left: minX,
        top: minY,
        width: boxW,
        height: boxH,
        cx: sumX / count,
        cy: sumY / count,
        area: count
      });
    }
  }

  return regions.sort(sortRegions);
}

async function main() {
  const sharp = await loadSharp();
  if (!fs.existsSync(INPUT)) {
    throw new Error('Missing input: ' + INPUT);
  }

  fs.mkdirSync(OUTPUT_DIR, { recursive: true });

  const image = sharp(INPUT);
  const meta = await image.metadata();
  const { data, info } = await image.ensureAlpha().raw().toBuffer({ resolveWithObject: true });
  const regions = findRegions(data, info.width, info.height);

  if (regions.length === 0) {
    throw new Error('No sprite regions detected');
  }

  console.log('Detected regions:', regions.length);

  const manifest = [];
  for (let i = 0; i < regions.length; i++) {
    const region = regions[i];
    const pad = 8;
    const left = Math.max(0, region.left - pad);
    const top = Math.max(0, region.top - pad);
    const width = Math.min(info.width - left, region.width + pad * 2);
    const height = Math.min(info.height - top, region.height + pad * 2);
    const name = NAMES[i] || ('sprite-' + String(i + 1).padStart(2, '0'));
    const output = path.join(OUTPUT_DIR, name + '.png');

    await sharp(INPUT)
      .extract({ left, top, width, height })
      .ensureAlpha()
      .raw()
      .toBuffer({ resolveWithObject: true })
      .then(async ({ data, info }) => {
        for (let i = 0; i < data.length; i += 4) {
          const r = data[i];
          const g = data[i + 1];
          const b = data[i + 2];
          if (r < 24 && g < 24 && b < 24) {
            data[i + 3] = 0;
          }
        }
        await sharp(data, {
          raw: { width: info.width, height: info.height, channels: 4 }
        }).png().toFile(output);
      });

    manifest.push({
      file: 'img/weapons/' + name + '.png',
      name,
      width,
      height
    });
    console.log('Saved', output);
  }

  fs.writeFileSync(
    path.join(OUTPUT_DIR, 'manifest.json'),
    JSON.stringify(manifest, null, 2),
    'utf8'
  );
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});

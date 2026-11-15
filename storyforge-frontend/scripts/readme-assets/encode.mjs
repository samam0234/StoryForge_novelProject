import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { log } from './lib.mjs';

// ffmpeg는 시스템에 없어도 됩니다. `pip install imageio-ffmpeg` 후 그 실행 파일 경로를 FFMPEG로 넘깁니다.
const FFMPEG = process.env.FFMPEG ?? 'ffmpeg';

function run(args) {
  execFileSync(FFMPEG, ['-hide_banner', '-loglevel', 'error', '-y', ...args], { stdio: ['ignore', 'inherit', 'inherit'] });
}

function tmp(name) {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), `sf-${name}-`));
  return dir;
}

/** frames: [{ png: Buffer, seconds }] → GIF (팔레트 최적화) */
export function toGif(frames, outPath, { width = 960, fps = 12 } = {}) {
  const dir = tmp('gif');
  const lines = ['ffconcat version 1.0'];
  frames.forEach((f, i) => {
    const file = path.join(dir, `f${String(i).padStart(4, '0')}.png`);
    fs.writeFileSync(file, f.png);
    lines.push(`file '${file.replace(/\\/g, '/')}'`, `duration ${f.seconds.toFixed(3)}`);
  });
  lines.push(`file '${path.join(dir, `f${String(frames.length - 1).padStart(4, '0')}.png`).replace(/\\/g, '/')}'`);
  fs.writeFileSync(path.join(dir, 'list.txt'), lines.join('\n'));
  fs.mkdirSync(path.dirname(outPath), { recursive: true });
  run(['-f', 'concat', '-safe', '0', '-i', path.join(dir, 'list.txt'),
    '-vf', `fps=${fps},scale=${width}:-1:flags=lanczos,split[a][b];[a]palettegen=max_colors=160:stats_mode=diff[p];[b][p]paletteuse=dither=bayer:bayer_scale=4:diff_mode=rectangle`,
    '-loop', '0', outPath]);
  fs.rmSync(dir, { recursive: true, force: true });
  log(`  ${path.relative(process.cwd(), outPath)}  ${(fs.statSync(outPath).size / 1024 / 1024).toFixed(2)}MB`);
}

/** slides: [{ png: Buffer }] → 크로스페이드가 있는 MP4 (H.264) */
export function toSlideshowMp4(slides, outPath, { seconds = 3.2, fade = 0.6, width = 1920 } = {}) {
  const dir = tmp('mp4');
  const inputs = [];
  slides.forEach((s, i) => {
    const file = path.join(dir, `s${i}.png`);
    fs.writeFileSync(file, s.png);
    inputs.push('-loop', '1', '-t', String(seconds + fade), '-i', file);
  });
  let filter = '';
  let last = '[0:v]';
  for (let i = 1; i < slides.length; i++) {
    const offset = (seconds * i).toFixed(2);
    filter += `${last}[${i}:v]xfade=transition=fade:duration=${fade}:offset=${offset}[v${i}];`;
    last = `[v${i}]`;
  }
  filter += `${last}scale=${width}:-2:flags=lanczos,format=yuv420p[out]`;
  fs.mkdirSync(path.dirname(outPath), { recursive: true });
  run([...inputs, '-filter_complex', filter, '-map', '[out]', '-r', '30', '-c:v', 'libx264', '-preset', 'slow', '-crf', '21',
    '-movflags', '+faststart', outPath]);
  fs.rmSync(dir, { recursive: true, force: true });
  log(`  ${path.relative(process.cwd(), outPath)}  ${(fs.statSync(outPath).size / 1024 / 1024).toFixed(2)}MB`);
}

/** 같은 프레임 목록을 MP4로도 만듭니다 (스트리밍 장면용). */
export function framesToMp4(frames, outPath, { width = 1920 } = {}) {
  const dir = tmp('fmp4');
  const lines = ['ffconcat version 1.0'];
  frames.forEach((f, i) => {
    const file = path.join(dir, `f${String(i).padStart(4, '0')}.png`);
    fs.writeFileSync(file, f.png);
    lines.push(`file '${file.replace(/\\/g, '/')}'`, `duration ${f.seconds.toFixed(3)}`);
  });
  lines.push(`file '${path.join(dir, `f${String(frames.length - 1).padStart(4, '0')}.png`).replace(/\\/g, '/')}'`);
  fs.writeFileSync(path.join(dir, 'list.txt'), lines.join('\n'));
  fs.mkdirSync(path.dirname(outPath), { recursive: true });
  run(['-f', 'concat', '-safe', '0', '-i', path.join(dir, 'list.txt'), '-vf', `fps=30,scale=${width}:-2:flags=lanczos,format=yuv420p`,
    '-c:v', 'libx264', '-preset', 'slow', '-crf', '21', '-movflags', '+faststart', outPath]);
  fs.rmSync(dir, { recursive: true, force: true });
  log(`  ${path.relative(process.cwd(), outPath)}  ${(fs.statSync(outPath).size / 1024 / 1024).toFixed(2)}MB`);
}

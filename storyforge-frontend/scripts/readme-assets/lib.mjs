import fs from 'node:fs';
import path from 'node:path';
import { chromium } from 'playwright-core';

export const WEB = process.env.WEB ?? 'http://localhost:4200';
export const CONSOLE = process.env.CONSOLE ?? 'http://localhost:4201';
export const API = process.env.API ?? 'http://localhost:8080/api/v1';
export const ROOT = path.resolve(import.meta.dirname, '../../..');
export const OUT = path.join(ROOT, 'docs/images');

export function log(...a) { console.log(...a); }

export async function api(method, route, token, body) {
  const res = await fetch(`${API}${route}`, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await res.text();
  if (!res.ok) {
    const err = new Error(`${method} ${route} → ${res.status} ${text.slice(0, 160)}`);
    err.status = res.status;
    throw err;
  }
  return text && res.headers.get('content-type')?.includes('json') ? JSON.parse(text) : text;
}

export async function launch() {
  const opts = { headless: true };
  return chromium.launch({ channel: 'chrome', ...opts }).catch(() => chromium.launch({ channel: 'msedge', ...opts }));
}

/** HTML 조각을 이미지로 그립니다. 표지, 배너, 프레임 같은 합성에 씁니다. */
export async function renderHtml(browser, html, { width, height, scale = 1, omitBackground = false }, outPath) {
  const ctx = await browser.newContext({ viewport: { width, height }, deviceScaleFactor: scale });
  const page = await ctx.newPage();
  await page.setContent(html, { waitUntil: 'networkidle' });
  await page.evaluate(() => document.fonts.ready);
  const buffer = await page.screenshot({ type: 'png', omitBackground, clip: { x: 0, y: 0, width, height } });
  await ctx.close();
  if (outPath) {
    fs.mkdirSync(path.dirname(outPath), { recursive: true });
    fs.writeFileSync(outPath, buffer);
  }
  return buffer;
}

export const FONT_LINKS = `
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=Noto+Serif+KR:wght@500;700;900&family=Noto+Sans+KR:wght@400;500;700;900&display=swap" rel="stylesheet">
<link rel="stylesheet" as="style" crossorigin href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/static/pretendard.min.css">`;

/** 게시판 카드(약 2.16:1)에 맞춘 가로형 표지입니다. 세로로 잘려도 제목이 가운데에 남도록 배치합니다. */
export const COVER_SIZE = { width: 1000, height: 462 };
export function coverHtml({ title, cover, author, kind }) {
  const { from, to, accent, mark } = cover;
  return `<!doctype html><html><head><meta charset="utf-8">${FONT_LINKS}<style>
  *{box-sizing:border-box;margin:0}
  body{width:1000px;height:462px;position:relative;overflow:hidden;font-family:'Pretendard','Noto Sans KR',sans-serif;
    background:radial-gradient(90% 140% at 0% 0%, ${from} 0%, ${to} 75%);color:#fff}
  .glow{position:absolute;width:640px;height:640px;border-radius:50%;right:-140px;top:-190px;background:radial-gradient(circle, ${accent}66 0%, transparent 62%)}
  .ring{position:absolute;width:420px;height:420px;border-radius:50%;right:70px;top:21px;border:1px solid ${accent}44}
  .ring.b{width:290px;height:290px;right:135px;top:86px;border-color:${accent}2a}
  .mark{position:absolute;right:120px;top:50%;transform:translateY(-52%);font-family:'Noto Serif KR',serif;font-weight:900;font-size:260px;line-height:1;color:${accent};opacity:.28}
  .text{position:absolute;left:64px;top:0;bottom:0;width:560px;display:flex;flex-direction:column;justify-content:center}
  .meta{font-size:17px;letter-spacing:.2em;color:${accent};font-weight:700;margin-bottom:18px}
  .bar{width:56px;height:4px;background:${accent};border-radius:2px;margin-bottom:22px}
  h1{font-family:'Noto Serif KR',serif;font-weight:900;font-size:56px;line-height:1.22;letter-spacing:-.01em;word-break:keep-all;text-shadow:0 2px 26px rgba(0,0,0,.4)}
  .by{margin-top:22px;font-size:19px;opacity:.82;font-weight:500}
  </style></head><body><div class="glow"></div><div class="ring"></div><div class="ring b"></div><div class="mark">${mark}</div>
  <div class="text"><div class="meta">${kind === 'LONG' ? 'NOVEL' : kind === 'PACKAGE' ? 'PACKAGE' : 'SHORT STORY'}</div><div class="bar"></div><h1>${title}</h1><div class="by">${author}</div></div></body></html>`;
}

export async function upload(token, buffer) {
  const form = new FormData();
  form.append('file', new Blob([buffer], { type: 'image/png' }), 'cover.png');
  const res = await fetch(`${API}/media`, { method: 'POST', headers: { Authorization: `Bearer ${token}` }, body: form });
  if (!res.ok) throw new Error(`media upload → ${res.status} ${(await res.text()).slice(0, 120)}`);
  return (await res.json()).url;
}

export function sleep(ms) { return new Promise((r) => setTimeout(r, ms)); }

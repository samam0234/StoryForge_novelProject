import { FONT_LINKS } from './lib.mjs';

const BG = 'radial-gradient(120% 90% at 15% 0%, #1f4e79 0%, #12304d 38%, #0a1624 100%)';

/** 화면 캡처를 브라우저 창 틀에 넣어 소개용 이미지로 만듭니다. */
export function frameHtml({ shot, url, caption, step, canvas, windowWidth, top = 44 }) {
  const { width, height } = canvas;
  const left = Math.round((width - windowWidth) / 2);
  return `<!doctype html><html><head><meta charset="utf-8">${FONT_LINKS}<style>
  *{box-sizing:border-box;margin:0}
  body{width:${width}px;height:${height}px;position:relative;overflow:hidden;background:${BG};font-family:'Pretendard','Noto Sans KR',sans-serif}
  .grid{position:absolute;inset:0;background-image:radial-gradient(rgba(255,255,255,.06) 1px, transparent 1px);background-size:28px 28px;mask-image:radial-gradient(80% 70% at 50% 40%, #000 30%, transparent 100%)}
  .win{position:absolute;left:${left}px;top:${top}px;width:${windowWidth}px;border-radius:14px;overflow:hidden;background:#0d1b2a;
    box-shadow:0 40px 90px rgba(0,0,0,.55),0 0 0 1px rgba(255,255,255,.1)}
  .bar{height:38px;display:flex;align-items:center;gap:8px;padding:0 16px;background:linear-gradient(#1b2b3c,#16242f);border-bottom:1px solid rgba(255,255,255,.07)}
  .dot{width:12px;height:12px;border-radius:50%}
  .url{margin-left:14px;flex:1;max-width:520px;height:24px;border-radius:12px;background:rgba(255,255,255,.07);color:#9fb3c8;font-size:13px;display:flex;align-items:center;padding:0 14px;letter-spacing:.01em}
  img.shot{display:block;width:100%}
  .cap{position:absolute;left:0;right:0;bottom:34px;display:flex;justify-content:center}
  .pill{display:flex;align-items:center;gap:14px;padding:12px 26px 12px 14px;border-radius:999px;background:rgba(10,22,36,.82);backdrop-filter:blur(8px);
    box-shadow:0 0 0 1px rgba(255,255,255,.12),0 12px 30px rgba(0,0,0,.4);color:#fff;font-size:26px;font-weight:600;letter-spacing:-.01em}
  .step{min-width:42px;height:42px;border-radius:21px;background:#E8A838;color:#1b2b3c;font-weight:800;font-size:22px;display:flex;align-items:center;justify-content:center;padding:0 12px}
  </style></head><body><div class="grid"></div>
  <div class="win"><div class="bar"><span class="dot" style="background:#ff5f57"></span><span class="dot" style="background:#febc2e"></span><span class="dot" style="background:#28c840"></span>
  <span class="url">${url ?? ''}</span></div><img class="shot" src="${shot}"></div>
  ${caption ? `<div class="cap"><div class="pill">${step ? `<span class="step">${step}</span>` : ''}<span>${caption}</span></div></div>` : ''}
  </body></html>`;
}

/** 정지 이미지용: 창 높이에 맞춰 캔버스를 정합니다. */
export function staticSize(viewport, windowWidth = 1504) {
  const inner = Math.round((windowWidth * viewport.height) / viewport.width);
  return { windowWidth, canvas: { width: windowWidth + 96, height: inner + 38 + 96 }, top: 48 };
}

/** 영상·GIF용: 고정 캔버스(1920x1200)에 캡션 자리를 둡니다. */
export function videoSize(viewport) {
  const windowWidth = 1640;
  return { windowWidth, canvas: { width: 1920, height: 1200 }, top: 40 };
}

export async function composeShot(page, shotBuffer, opts) {
  const shot = `data:image/png;base64,${shotBuffer.toString('base64')}`;
  await page.setContent(frameHtml({ ...opts, shot }), { waitUntil: 'load' });
  await page.evaluate(() => document.fonts.ready);
  return page.screenshot({ type: 'png', clip: { x: 0, y: 0, ...opts.canvas } });
}

/** 모바일 화면 두 장을 휴대폰 틀에 넣어 나란히 보여 줍니다. */
export function phonesHtml(shots, { width = 1600, height = 1000 } = {}) {
  const phone = (src, i) => `<div class="phone" style="transform:translateY(${i % 2 ? 28 : -10}px)"><div class="notch"></div><img src="${src}"></div>`;
  return `<!doctype html><html><head><meta charset="utf-8"><style>
  *{box-sizing:border-box;margin:0}
  body{width:${width}px;height:${height}px;background:${BG};display:flex;align-items:center;justify-content:center;gap:90px;overflow:hidden;position:relative}
  body:before{content:"";position:absolute;inset:0;background-image:radial-gradient(rgba(255,255,255,.06) 1px, transparent 1px);background-size:28px 28px;mask-image:radial-gradient(70% 70% at 50% 45%, #000 30%, transparent 100%)}
  .phone{position:relative;width:420px;border-radius:54px;padding:50px 14px 22px;background:linear-gradient(145deg,#2b3a4a,#0d1620);box-shadow:0 50px 100px rgba(0,0,0,.6),0 0 0 2px #3a4b5d,inset 0 0 0 2px #0a1118}
  .phone img{display:block;width:100%;border-radius:30px}
  .notch{position:absolute;top:16px;left:50%;transform:translateX(-50%);width:120px;height:26px;border-radius:14px;background:#05090e;z-index:2}
  </style></head><body>${shots.map(phone).join('')}</body></html>`;
}

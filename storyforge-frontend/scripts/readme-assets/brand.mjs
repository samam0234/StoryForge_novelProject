// 로고 타일, 기술 스택, 아키텍처, 파이프라인, 히어로 배너를 HTML로 그려 PNG로 저장합니다.
// 로고는 Simple Icons(CC0)의 SVG이며, 상표는 각 소유자에게 있습니다. docs/images/README.md 참고.
import fs from 'node:fs';
import path from 'node:path';
import { FONT_LINKS, log, OUT, renderHtml } from './lib.mjs';

const SRC = path.join(OUT, 'logos/src');

/** [파일, 표시 이름, 색] — 어두운 타일 위에서 잘 보이도록 밝게 조정한 브랜드 색입니다. */
export const LOGOS = {
  angular: ['Angular', '#F2385A'], typescript: ['TypeScript', '#4A9BE8'], reactivex: ['RxJS', '#E24FB8'], materialdesign: ['Material', '#E6E6E6'], vitest: ['Vitest', '#9AD14B'],
  springboot: ['Spring Boot', '#6DB33F'], openjdk: ['Java 21', '#F5A24B'], flyway: ['Flyway', '#F0524F'], apachemaven: ['Maven', '#E5415E'], junit5: ['JUnit 5', '#3FC785'],
  fastapi: ['FastAPI', '#14B8A6'], python: ['Python', '#5DA9E9'], pydantic: ['Pydantic', '#F0457E'], jinja: ['Jinja2', '#E8685C'], ollama: ['Ollama', '#FFFFFF'], anthropic: ['Claude', '#E8A27C'],
  postgresql: ['PostgreSQL', '#5B8DEF'], docker: ['Docker', '#3AA2F0'], githubactions: ['GitHub Actions', '#4A9BFF'], nodedotjs: ['Node.js', '#7CC36B'],
};

const svgOf = (key) => {
  const raw = fs.readFileSync(path.join(SRC, `${key}.svg`), 'utf-8');
  return raw.replace('<svg ', `<svg fill="${LOGOS[key][1]}" `).replace(/<title>.*?<\/title>/, '');
};

const BASE_CSS = `
  *{box-sizing:border-box;margin:0}
  body{font-family:'Pretendard','Noto Sans KR',sans-serif;color:#fff;-webkit-font-smoothing:antialiased}
  .tile{display:flex;align-items:center;justify-content:center;background:linear-gradient(160deg,#1b3047,#0e1b2a);border:1px solid rgba(255,255,255,.09);box-shadow:0 10px 24px rgba(0,0,0,.35), inset 0 1px 0 rgba(255,255,255,.06)}
  .tile svg{width:54%;height:54%}`;

const BG = 'radial-gradient(120% 100% at 12% 0%, #1f4e79 0%, #12304d 42%, #0a1624 100%)';
const GRID = `<div style="position:absolute;inset:0;background-image:radial-gradient(rgba(255,255,255,.055) 1px, transparent 1px);background-size:28px 28px;mask-image:radial-gradient(85% 75% at 50% 40%, #000 30%, transparent 100%)"></div>`;
const MATERIAL = '<link href="https://fonts.googleapis.com/icon?family=Material+Icons+Round" rel="stylesheet">';

/** 개별 로고 타일 PNG (README 표와 문장 사이에서 씁니다). */
export async function logos(browser) {
  log('· 로고 타일');
  for (const key of Object.keys(LOGOS)) {
    const html = `<!doctype html><html><head><meta charset="utf-8"><style>${BASE_CSS}
      html,body{background:transparent}.tile{width:192px;height:192px;border-radius:44px}</style></head><body><div class="tile">${svgOf(key)}</div></body></html>`;
    await renderHtml(browser, html, { width: 192, height: 192, scale: 1, omitBackground: true }, path.join(OUT, `logos/${key}.png`));
  }
  log(`  ${Object.keys(LOGOS).length}개`);
}

/** 기술 스택 한 장 요약 */
export async function stack(browser) {
  log('· 기술 스택');
  const groups = [
    ['화면', 'Angular 21 · 사용자 웹과 운영 콘솔', ['angular', 'typescript', 'reactivex', 'materialdesign', 'vitest']],
    ['오케스트레이션', 'Spring Boot 4.1 · 인증, 상태 머신, 저장', ['springboot', 'openjdk', 'flyway', 'apachemaven', 'junit5']],
    ['AI 서비스', 'FastAPI · 프롬프트, 생성, 검증', ['fastapi', 'python', 'pydantic', 'jinja', 'ollama', 'anthropic']],
    ['데이터와 운영', 'PostgreSQL 16 · 컨테이너 · CI', ['postgresql', 'docker', 'githubactions', 'nodedotjs']],
  ];
  const card = ([title, sub, keys]) => `<section class="card"><h3>${title}</h3><p>${sub}</p>
    <div class="row">${keys.map((k) => `<div class="item"><div class="tile">${svgOf(k)}</div><span>${LOGOS[k][0]}</span></div>`).join('')}</div></section>`;
  const html = `<!doctype html><html><head><meta charset="utf-8">${FONT_LINKS}<style>${BASE_CSS}
    body{width:1600px;height:520px;position:relative;overflow:hidden;background:${BG}}
    .wrap{position:absolute;inset:0;padding:44px 48px;display:grid;grid-template-columns:repeat(4,1fr);gap:22px}
    .card{background:rgba(10,22,36,.55);border:1px solid rgba(255,255,255,.1);border-radius:22px;padding:26px 24px;backdrop-filter:blur(6px);box-shadow:0 24px 50px rgba(0,0,0,.35)}
    h3{font-size:25px;font-weight:800;letter-spacing:-.01em}
    .card p{margin:6px 0 24px;font-size:15px;color:#9fb3c8;line-height:1.5;min-height:46px}
    .row{display:flex;flex-wrap:wrap;gap:18px 14px}
    .item{width:88px;display:flex;flex-direction:column;align-items:center;gap:9px}
    .item .tile{width:88px;height:88px;border-radius:22px}
    .item span{font-size:14px;color:#d6e2ee;font-weight:600;text-align:center;line-height:1.25}
  </style></head><body>${GRID}<div class="wrap">${groups.map(card).join('')}</div></body></html>`;
  await renderHtml(browser, html, { width: 1600, height: 520, scale: 1 }, path.join(OUT, 'stack.png'));
}

/** 파이프라인 인포그래픽 */
export async function pipeline(browser) {
  log('· 파이프라인');
  const steps = [
    ['auto_stories', 'Story Bible', '세계관, 인물, 테마, 톤', '사람이 확정', '#2E75B6'],
    ['account_tree', 'Outline', '챕터별 사건과 연속성 포인트', '사람이 확정', '#2E75B6'],
    ['edit_note', '챕터 생성', '앞선 설정을 넣어 순서대로', 'AI 스트리밍', '#E8A838'],
    ['fact_check', 'Continuity 검증', '설정·인물·시간·플롯 점검', '리포트로 공개', '#E8A838'],
    ['task_alt', '사용자 승인', '확인하고 다음 장으로', '사람이 승인', '#2E75B6'],
    ['public', '게시와 공유', 'Clone, 공유, 패키지', '선택', '#26A69A'],
  ];
  const html = `<!doctype html><html><head><meta charset="utf-8">${FONT_LINKS}${MATERIAL}<style>${BASE_CSS}
    body{width:1600px;height:430px;position:relative;overflow:hidden;background:${BG}}
    .row{position:absolute;left:48px;right:48px;top:64px;display:flex;align-items:stretch;gap:0}
    .step{flex:1;position:relative;padding:0 10px}
    .ico{width:92px;height:92px;border-radius:26px;display:flex;align-items:center;justify-content:center;margin:0 auto 20px;color:#fff;box-shadow:0 18px 36px rgba(0,0,0,.4), inset 0 1px 0 rgba(255,255,255,.25)}
    .ico i{font-family:'Material Icons Round';font-style:normal;font-size:46px;line-height:1}
    .num{position:absolute;top:-10px;left:50%;margin-left:-62px;width:30px;height:30px;border-radius:15px;background:#0a1624;border:2px solid rgba(255,255,255,.35);font-size:15px;font-weight:800;display:flex;align-items:center;justify-content:center}
    h4{text-align:center;font-size:23px;font-weight:800;letter-spacing:-.01em}
    p{text-align:center;font-size:15px;color:#a9bdd2;margin:8px 8px 16px;line-height:1.5;min-height:46px}
    .tag{display:block;width:max-content;margin:0 auto;padding:6px 16px;border-radius:999px;font-size:14px;font-weight:700;background:rgba(255,255,255,.08);border:1px solid rgba(255,255,255,.16)}
    .arrow{position:absolute;right:-14px;top:36px;color:rgba(255,255,255,.35);z-index:2}
    .arrow i{font-family:'Material Icons Round';font-style:normal;font-size:30px}
    .note{position:absolute;left:48px;right:48px;bottom:34px;text-align:center;font-size:17px;color:#cfe0f0}
    .note b{color:#E8A838}
  </style></head><body>${GRID}
  <div class="row">${steps.map(([icon, t, d, tag, color], i) => `
    <div class="step"><span class="num">${i + 1}</span>
      <div class="ico" style="background:linear-gradient(145deg,${color},${color}aa)"><i>${icon}</i></div>
      <h4>${t}</h4><p>${d}</p><span class="tag">${tag}</span>${i < steps.length - 1 ? '<span class="arrow"><i>chevron_right</i></span>' : ''}</div>`).join('')}</div>
  <div class="note"><b>자동 승인은 없습니다.</b> 검증 결과를 보고 다음으로 넘어갈지는 항상 사람이 정합니다.</div>
  </body></html>`;
  await renderHtml(browser, html, { width: 1600, height: 430, scale: 1 }, path.join(OUT, 'pipeline.png'));
}

/** 아키텍처 다이어그램 (로고 포함) */
export async function architecture(browser) {
  log('· 아키텍처');
  const node = (x, y, w, key, title, sub, accent = '#2E75B6') => `
    <div class="node" style="left:${x}px;top:${y}px;width:${w}px;border-color:${accent}66">
      <div class="tile">${key ? svgOf(key) : ''}</div><div><b>${title}</b><span>${sub}</span></div></div>`;
  const html = `<!doctype html><html><head><meta charset="utf-8">${FONT_LINKS}<style>${BASE_CSS}
    body{width:1600px;height:640px;position:relative;overflow:hidden;background:${BG}}
    .node{position:absolute;height:112px;border-radius:22px;background:rgba(10,22,36,.72);border:1.5px solid;display:flex;align-items:center;gap:18px;padding:0 22px;box-shadow:0 24px 50px rgba(0,0,0,.4)}
    .node .tile{width:68px;height:68px;border-radius:18px;flex:none}
    .node b{display:block;font-size:23px;font-weight:800;letter-spacing:-.01em}
    .node span{display:block;margin-top:4px;font-size:14.5px;color:#a9bdd2;line-height:1.35}
    svg.lines{position:absolute;inset:0}
    .lab{position:absolute;font-size:14px;font-weight:700;padding:5px 12px;border-radius:999px;background:#0a1624;border:1px solid rgba(255,255,255,.18);color:#d6e2ee;transform:translate(-50%,-50%);white-space:nowrap}
    .lab.gold{color:#E8A838;border-color:#E8A83866}
    .zone{position:absolute;border:1.5px dashed rgba(255,255,255,.22);border-radius:26px}
    .zone small{position:absolute;top:-12px;left:22px;background:#0f2437;padding:0 10px;font-size:13px;letter-spacing:.14em;color:#9fb3c8;font-weight:700}
  </style></head><body>${GRID}
  <div class="zone" style="left:560px;top:70px;width:1000px;height:500px"><small>서버 · 내부망</small></div>
  <svg class="lines" width="1600" height="640" viewBox="0 0 1600 640" fill="none">
    <defs><marker id="a" markerWidth="10" markerHeight="10" refX="8" refY="5" orient="auto"><path d="M0 0 L10 5 L0 10 z" fill="#8fb4d9"/></marker>
          <marker id="g" markerWidth="10" markerHeight="10" refX="8" refY="5" orient="auto"><path d="M0 0 L10 5 L0 10 z" fill="#E8A838"/></marker></defs>
    <path d="M388 175 H640" stroke="#8fb4d9" stroke-width="2.5" marker-end="url(#a)"/>
    <path d="M388 465 H500 V215 H640" stroke="#8fb4d9" stroke-width="2.5" marker-end="url(#a)"/>
    <path d="M944 175 H1000 V175 H1120" stroke="#E8A838" stroke-width="2.5" marker-end="url(#g)"/>
    <path d="M1180 222 V326" stroke="#E8A838" stroke-width="2.5" marker-end="url(#g)"/>
    <path d="M792 222 V360" stroke="#8fb4d9" stroke-width="2.5" marker-end="url(#a)"/>
  </svg>
  ${node(36, 110, 352, 'angular', '사용자 웹', 'Angular 21 · 4200')}
  ${node(36, 410, 352, 'angular', '운영 콘솔', 'Angular 21 · 4201 · 운영자 전용', '#E8A838')}
  ${node(640, 110, 304, 'springboot', 'Spring Boot', '인증 · 상태 머신 · 저장 · 8080')}
  ${node(1120, 110, 400, 'fastapi', 'FastAPI', '프롬프트 · 생성 · 검증 · 127.0.0.1:8001', '#14B8A6')}
  ${node(1120, 330, 400, 'ollama', 'Ollama · Claude · mock', '언어 모델. 개발 기본은 mock', '#14B8A6')}
  ${node(640, 364, 304, 'postgresql', 'PostgreSQL 16', '원고 · 계정 · 커뮤니티 · 5433', '#5B8DEF')}
  <div class="lab" style="left:514px;top:175px">REST · SSE · WebSocket</div>
  <div class="lab" style="left:514px;top:340px">REST</div>
  <div class="lab gold" style="left:1060px;top:140px">내부 HTTP · SSE · WS</div>
  <div class="lab gold" style="left:1180px;top:280px">모델 호출</div>
  <div class="lab" style="left:792px;top:292px">JDBC</div>
  </body></html>`;
  await renderHtml(browser, html, { width: 1600, height: 640, scale: 1 }, path.join(OUT, 'architecture-logos.png'));
}

/** 히어로 배너와 소셜 미리보기. 화면 캡처(screens/*.png)가 먼저 있어야 합니다. */
export async function hero(browser) {
  log('· 히어로 배너');
  const img = (name) => `data:image/png;base64,${fs.readFileSync(path.join(OUT, `screens/${name}.png`)).toString('base64')}`;
  const mark = `<div class="mark"><i>auto_stories</i></div>`;
  const html = `<!doctype html><html><head><meta charset="utf-8">${FONT_LINKS}${MATERIAL}<style>${BASE_CSS}
    body{width:1600px;height:760px;position:relative;overflow:hidden;background:${BG}}
    .glow{position:absolute;width:900px;height:900px;border-radius:50%;right:-160px;top:-260px;background:radial-gradient(circle,rgba(232,168,56,.22),transparent 62%)}
    .copy{position:absolute;left:84px;top:0;bottom:0;width:690px;display:flex;flex-direction:column;justify-content:center}
    .brand{display:flex;align-items:center;gap:20px}
    .mark{width:84px;height:84px;border-radius:24px;background:linear-gradient(145deg,#2E75B6,#E8A838);display:flex;align-items:center;justify-content:center;box-shadow:0 18px 40px rgba(0,0,0,.4)}
    .mark i{font-family:'Material Icons Round';font-style:normal;font-size:50px;color:#fff}
    h1{font-size:84px;font-weight:900;letter-spacing:-.035em}
    h2{margin-top:28px;font-size:44px;line-height:1.28;font-weight:800;letter-spacing:-.02em;word-break:keep-all}
    h2 em{font-style:normal;background:linear-gradient(90deg,#E8A838,#ffd58a);-webkit-background-clip:text;color:transparent}
    p{margin-top:22px;font-size:21px;line-height:1.6;color:#b8cce0;word-break:keep-all;max-width:620px}
    .chips{margin-top:34px;display:flex;gap:12px;flex-wrap:wrap}
    .chip{display:flex;align-items:center;gap:10px;padding:8px 16px 8px 8px;border-radius:999px;background:rgba(10,22,36,.6);border:1px solid rgba(255,255,255,.14);font-size:16px;font-weight:700}
    .chip .tile{width:30px;height:30px;border-radius:9px}
    .stage{position:absolute;left:800px;top:0;width:800px;height:760px;perspective:2600px}
    .shot{position:absolute;border-radius:16px;box-shadow:0 50px 90px rgba(0,0,0,.6),0 0 0 1px rgba(255,255,255,.12);transform-origin:left center}
    .s1{width:760px;left:10px;top:118px;transform:rotateY(-9deg) rotateX(2deg);z-index:2}
    .s2{width:520px;left:96px;top:468px;transform:rotateY(-9deg) rotateX(2deg);z-index:3}
    .s3{width:520px;left:250px;top:-52px;transform:rotateY(-9deg) rotateX(2deg);z-index:1;opacity:.8}
  </style></head><body>${GRID}<div class="glow"></div>
  <div class="copy"><div class="brand">${mark}<h1>StoryForge</h1></div>
    <h2>한 줄 아이디어에서<br><em>설정이 무너지지 않는</em> 장편 소설까지</h2>
    <p>Story Bible과 Outline을 먼저 확정하고, 챕터마다 앞선 설정과 어긋나는지 검증합니다. 승인은 언제나 사람이 합니다.</p>
    <div class="chips">${['angular', 'springboot', 'fastapi', 'postgresql'].map((k) => `<div class="chip"><div class="tile">${svgOf(k)}</div>${LOGOS[k][0]}</div>`).join('')}</div></div>
  <div class="stage"><img class="shot s3" src="${img('profile')}"><img class="shot s1" src="${img('chapter')}"><img class="shot s2" src="${img('board')}"></div>
  </body></html>`;
  await renderHtml(browser, html, { width: 1600, height: 760, scale: 1 }, path.join(OUT, 'hero.png'));
  // GitHub 저장소 소셜 미리보기(1280x640): 문구 영역을 좁히고 화면을 오른쪽으로 보냅니다.
  const social = html
    .replace('width:1600px;height:760px', 'width:1280px;height:640px')
    .replace('.copy{position:absolute;left:84px;top:0;bottom:0;width:690px', '.copy{position:absolute;left:60px;top:0;bottom:0;width:620px')
    .replace('h1{font-size:84px', 'h1{font-size:66px')
    .replace('h2{margin-top:28px;font-size:44px', 'h2{margin-top:20px;font-size:35px')
    .replace('p{margin-top:22px;font-size:21px;line-height:1.6;color:#b8cce0;word-break:keep-all;max-width:620px}', 'p{margin-top:18px;font-size:18px;line-height:1.6;color:#b8cce0;word-break:keep-all;max-width:520px}')
    .replace('.chips{margin-top:34px', '.chips{margin-top:26px')
    .replace('.stage{position:absolute;left:800px', '.stage{position:absolute;left:740px')
    .replace('.mark{width:84px;height:84px', '.mark{width:70px;height:70px');
  await renderHtml(browser, social, { width: 1280, height: 640, scale: 1 }, path.join(OUT, 'social-preview.png'));
}


/** GitHub README는 저장소 안의 mp4를 바로 재생하지 못하므로, 재생 버튼을 입힌 포스터를 링크로 씁니다. */
export async function poster(browser) {
  log('· 영상 포스터');
  const img = `data:image/png;base64,${fs.readFileSync(path.join(OUT, 'screens/chapter.png')).toString('base64')}`;
  const html = `<!doctype html><html><head><meta charset="utf-8">${FONT_LINKS}<style>
    *{box-sizing:border-box;margin:0}
    body{width:1600px;height:1074px;position:relative;overflow:hidden;font-family:'Pretendard','Noto Sans KR',sans-serif}
    img{position:absolute;inset:0;width:100%;filter:brightness(.55) saturate(1.05)}
    .shade{position:absolute;inset:0;background:radial-gradient(60% 60% at 50% 48%, rgba(10,22,36,0) 0%, rgba(10,22,36,.55) 100%)}
    .play{position:absolute;left:50%;top:47%;width:190px;height:190px;margin:-95px 0 0 -95px;border-radius:50%;background:rgba(232,168,56,.96);box-shadow:0 30px 70px rgba(0,0,0,.55),0 0 0 14px rgba(232,168,56,.25);display:flex;align-items:center;justify-content:center}
    .play:after{content:"";margin-left:14px;border-left:62px solid #14263a;border-top:40px solid transparent;border-bottom:40px solid transparent}
    .cap{position:absolute;left:0;right:0;bottom:70px;text-align:center;color:#fff}
    .cap b{display:block;font-size:46px;font-weight:800;letter-spacing:-.02em;text-shadow:0 4px 30px rgba(0,0,0,.6)}
    .cap span{display:block;margin-top:12px;font-size:24px;color:#cfe0f0;text-shadow:0 2px 18px rgba(0,0,0,.6)}
  </style></head><body><img src="${img}"><div class="shade"></div><div class="play"></div>
  <div class="cap"><b>투어 영상 보기</b><span>10개 화면을 33초에 · 데모 데이터로 촬영</span></div></body></html>`;
  await renderHtml(browser, html, { width: 1600, height: 1074, scale: 1 }, path.join(OUT, 'demo/tour-poster.png'));
}

export async function brand(browser, only) {
  const steps = { logos, stack, pipeline, architecture, hero, poster };
  for (const [name, fn] of Object.entries(steps)) if (!only || only.includes(name)) await fn(browser);
}

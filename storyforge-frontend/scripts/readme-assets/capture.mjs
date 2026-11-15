import fs from 'node:fs';
import path from 'node:path';
import { api, CONSOLE, log, OUT, sleep, WEB } from './lib.mjs';
import { composeShot, phonesHtml, staticSize, videoSize } from './compose.mjs';
import { toGif, toSlideshowMp4, framesToMp4 } from './encode.mjs';
import { ADMIN, DOMAIN, PASSWORD } from './data.mjs';

const VIEWPORT = { width: 1440, height: 900 };
const STATE = JSON.parse(fs.readFileSync(path.join(import.meta.dirname, '.state.json'), 'utf-8'));
const made = STATE.made;
const dataUrl = (buf) => `data:image/png;base64,${buf.toString('base64')}`;

async function login(email) {
  const r = await api('POST', '/auth/login', null, { email, password: PASSWORD });
  return { token: r.accessToken, expiresAt: r.expiresAt, refreshToken: r.refreshToken, refreshExpiresAt: r.refreshExpiresAt, user: r.user };
}

async function userContext(browser, email, { viewport = VIEWPORT, scale = 1.5, theme } = {}) {
  const session = await login(email);
  const ctx = await browser.newContext({ viewport, deviceScaleFactor: scale, locale: 'ko-KR' });
  await ctx.addInitScript(([s, t]) => {
    localStorage.setItem('storyforge.session', JSON.stringify(s));
    if (t) localStorage.setItem('storyforge.theme', t);
  }, [session, theme]);
  return ctx;
}

async function consoleContext(browser, scale = 1.5) {
  const session = await login(ADMIN.email);
  const ctx = await browser.newContext({ viewport: VIEWPORT, deviceScaleFactor: scale, locale: 'ko-KR' });
  await ctx.addInitScript((s) => {
    sessionStorage.setItem('storyforge.console.token', s.token);
    if (s.refreshToken) sessionStorage.setItem('storyforge.console.refresh', s.refreshToken);
  }, session);
  return ctx;
}

async function settle(page, ms = 900) {
  await page.waitForLoadState('networkidle').catch(() => {});
  await page.evaluate(() => document.fonts.ready);
  await sleep(ms);
}

function save(rel, buffer) {
  const file = path.join(OUT, rel);
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, buffer);
  log(`  ${rel}  ${(buffer.length / 1024).toFixed(0)}KB`);
}

export async function capture(browser) {
  const composerCtx = await browser.newContext({ viewport: { width: 1920, height: 1200 }, deviceScaleFactor: 1 });
  const composer = await composerCtx.newPage();
  const raws = {};
  const memory = made.memory.projectId;
  const mem = made.memory.workId;

  /** 한 화면을 캡처해 창 틀에 넣고 저장합니다. raw는 영상용으로 보관합니다. */
  const only = process.env.ONLY?.split(',');
  async function snap(page, name, { url, host = WEB, prepare, ready, ms, dir = 'screens' } = {}) {
    if (only && !only.includes(name)) return;
    await page.goto(`${host}${url}`);
    if (ready) await page.locator(ready).first().waitFor({ timeout: 15000 });
    await settle(page, ms);
    if (prepare) { await prepare(page); await settle(page, 500); }
    const raw = await page.screenshot({ type: 'png' });
    raws[name] = { raw, url: host.replace('http://', '') + url };
    const out = await composeShot(composer, raw, { ...staticSize(page.viewportSize()), url: raws[name].url });
    save(`${dir}/${name}.png`, out);
  }
  const tab = (label) => async (p) => { await p.getByRole('tab', { name: label }).first().click(); };

  // ───────── 작가 화면 (서하린) ─────────
  log('· 작가 화면');
  const ctx = await userContext(browser, `haerin@${DOMAIN}`);
  const page = await ctx.newPage();
  await snap(page, 'dashboard', { url: '/', ready: 'h1', ms: 1400 });
  await snap(page, 'bible', { url: `/projects/${memory}/bible`, ready: 'h1, h2', prepare: async (p) => { await p.getByRole('tab', { name: /Characters/ }).click(); } });
  await snap(page, 'outline', { url: `/projects/${memory}/outline`, ready: 'h1, h2', prepare: async (p) => { const heads = p.locator('mat-expansion-panel-header'); await heads.nth(0).click(); await sleep(500); await heads.nth(2).click(); } });
  await snap(page, 'chapter', {
    url: `/projects/${memory}/chapters`, ready: '.workspace',
    prepare: async (p) => { await p.locator('.chapter-nav button', { hasText: '2.' }).click(); await sleep(700); },
  });
  await snap(page, 'activity', { url: `/projects/${memory}/activity`, ready: 'h1, h2', ms: 1500 });

  // ───────── 커뮤니티 ─────────
  log('· 커뮤니티');
  await snap(page, 'board', { url: '/board/long', ready: 'sf-work-card, .card', ms: 1500 });
  await snap(page, 'work', { url: `/works/${mem}`, ready: 'h1', ms: 1200 });
  for (const [name, label] of [['work-graph', '작업 그래프'], ['work-share', '공유 그래프'], ['work-network', 'Clone 네트워크']]) {
    await snap(page, name, { url: `/works/${mem}`, ready: 'h1', prepare: tab(label), ms: 1200 });
  }
  // 패키지포지: 독서 순서부터 전체 Continuity 검사까지 한 화면에 담습니다 (아래의 연결 지도는 세로로 길어 잘리므로 제외).
  const tallCtx = await userContext(browser, `haerin@${DOMAIN}`, { viewport: { width: 1440, height: 740 } });
  await snap(await tallCtx.newPage(), 'package', {
    url: `/package-forge/${made.package.workId}`, ready: 'h1', ms: 1500,
    prepare: async (p) => {
      await p.getByRole('heading', { name: '독서 순서' }).evaluate((el) => el.scrollIntoView({ block: 'start' }));
      await p.mouse.wheel(0, -24);
    },
  });
  // 통합 인물 사전: 여러 작품에 나온 인물을 모아 보여 줍니다 (교차 등장 표시).
  await snap(await tallCtx.newPage(), 'package-characters', {
    url: `/works/${made.package.workId}`, ready: 'h1', ms: 1200, prepare: tab('통합 인물 사전'),
  });
  await tallCtx.close();
  await snap(page, 'profile', { url: `/users/${STATE.users.haerin.id}`, ready: 'h1', ms: 1600 });
  await snap(page, 'issues', { url: '/issues', ready: 'h1', ms: 1300 });
  await snap(page, 'lounge', { url: '/create', ready: 'h1', ms: 1300 });

  // ───────── 상호작용 화면 ─────────
  log('· 가이드챗, Clone');
  await snap(page, 'guide', {
    url: '/', ready: 'h1',
    prepare: async (p) => {
      await p.getByRole('button', { name: 'AI 가이드 열기' }).click();
      await p.locator('input[name=message]').fill('/상태');
      await p.locator('input[name=message]').press('Enter');
      await p.locator('.msg').nth(1).waitFor({ timeout: 15000 });
      await sleep(900);
    },
  });
  await snap(page, 'clone', {
    url: `/works/${mem}`, ready: 'h1',
    prepare: async (p) => { await p.getByRole('button', { name: 'Clone' }).first().click(); await sleep(700); },
  });
  await ctx.close();

  // ───────── 라이트 테마, 모바일 ─────────
  log('· 라이트 테마와 모바일');
  const lightCtx = await userContext(browser, `haerin@${DOMAIN}`, { theme: 'light' });
  await snap(await lightCtx.newPage(), 'board-light', { url: '/board/long', ready: 'sf-work-card, .card', ms: 1500 });
  await lightCtx.close();

  const phoneCtx = await userContext(browser, `haerin@${DOMAIN}`, { viewport: { width: 390, height: 844 }, scale: 3 });
  const phone = await phoneCtx.newPage();
  const phoneShots = [];
  for (const [url, ready] of [['/board/long', 'sf-work-card, .card'], [`/works/${mem}`, 'h1']]) {
    await phone.goto(`${WEB}${url}`); await phone.locator(ready).first().waitFor({ timeout: 15000 }); await settle(phone, 1500);
    phoneShots.push(dataUrl(await phone.screenshot({ type: 'png' })));
  }
  await composer.setViewportSize({ width: 1600, height: 1000 });
  await composer.setContent(phonesHtml(phoneShots), { waitUntil: 'load' });
  save('screens/mobile.png', await composer.screenshot({ type: 'png', clip: { x: 0, y: 0, width: 1600, height: 1000 } }));
  await composer.setViewportSize({ width: 1920, height: 1200 });
  await phoneCtx.close();

  // ───────── 운영 콘솔 ─────────
  log('· 운영 콘솔');
  const loginCtx = await browser.newContext({ viewport: VIEWPORT, deviceScaleFactor: 1.5, locale: 'ko-KR' });
  await snap(await loginCtx.newPage(), 'console-login', { url: '/login', host: CONSOLE, ready: 'h2', ms: 1000 });
  await loginCtx.close();
  const adminCtx = await consoleContext(browser);
  const admin = await adminCtx.newPage();
  await snap(admin, 'console-overview', { url: '/', host: CONSOLE, ready: 'h1', ms: 1500 });
  await snap(admin, 'console-users', { url: '/users', host: CONSOLE, ready: 'h1', ms: 1200 });
  await snap(admin, 'console-audit', { url: '/audit', host: CONSOLE, ready: 'h1', ms: 1200 });
  await snap(admin, 'console-reports', {
    url: '/reports', host: CONSOLE, ready: '.report', ms: 1200,
    prepare: async (p) => {
      const boxes = p.locator('input[type=checkbox]');
      await boxes.nth(0).check(); await boxes.nth(1).check();
      await p.locator('select[name=action]').selectOption('HIDE');
      await p.locator('input[name=note]').fill('광고성 게시물로 판단해 숨김');
      await p.locator('.decision').scrollIntoViewIfNeeded();
    },
  });

  if (only) { await composerCtx.close(); return; }

  // ───────── 애니메이션 ─────────
  const vsize = videoSize(VIEWPORT);
  const frame = (raw, url, caption, step) => composeShot(composer, raw, { ...vsize, url, caption, step });

  log('· 투어 영상 (MP4)');
  const tour = [
    ['dashboard', '내 프로젝트 — 지금 무엇을 쓸지 한눈에'],
    ['bible', 'Story Bible — 세계관과 인물을 먼저 고정합니다'],
    ['outline', 'Outline — 챕터별 사건과 연속성 포인트'],
    ['chapter', '챕터 — 이전 설정을 넣어 쓰고, 검증은 사람이 확인합니다'],
    ['activity', '작업 그래프 — 사람과 에이전트가 쓴 양'],
    ['board', '게시판 — 롱품, 숏품, 패키지'],
    ['work-network', 'Clone 네트워크 — 이어 쓴 작품의 계보'],
    ['package', '패키지포지 — 여러 작품을 하나의 세계로'],
    ['profile', '프로필 — 활동 잔디와 업적'],
    ['console-overview', '운영 콘솔 — 현황과 신고 처리'],
  ];
  // 콘솔 raw는 호스트가 달라 별도 키를 씁니다.
  const slides = [];
  for (let i = 0; i < tour.length; i++) {
    const [name, caption] = tour[i];
    const r = raws[name];
    slides.push({ png: await frame(r.raw, r.url, caption, i + 1) });
  }
  toSlideshowMp4(slides, path.join(OUT, 'demo/tour.mp4'));
  save('demo/tour-poster.png', slides[3].png);

  log('· 커뮤니티 GIF');
  const comm = [];
  const commCtx = await userContext(browser, `haerin@${DOMAIN}`);
  const cp = await commCtx.newPage();
  await cp.goto(`${WEB}/works/${mem}`); await cp.locator('h1').first().waitFor(); await settle(cp, 1200);
  const tabs = ['읽기', '세계관', '작업 그래프', '공유 그래프', 'Clone 네트워크', '댓글'];
  for (let i = 0; i < tabs.length; i++) {
    await cp.getByRole('tab', { name: tabs[i] }).first().click(); await settle(cp, 1000);
    const raw = await cp.screenshot({ type: 'png' });
    comm.push({ png: await frame(raw, `localhost:4200/works/${mem}`, `작품 — ${tabs[i]}`, i + 1), seconds: 2.4 });
  }
  toGif(comm, path.join(OUT, 'demo/community.gif'), { width: 960, fps: 10 });
  await commCtx.close();

  log('· 운영 콘솔 GIF');
  const cons = [];
  const conPage = await adminCtx.newPage();
  const consSteps = [
    ['/', 'h1', '운영 현황 — 회원, 작품, 대기 중인 신고', null],
    ['/reports', '.report', '신고 목록 — 같은 대상의 신고는 한 번에', null],
    ['/reports', '.report', '여러 건을 골라 사유와 함께 일괄 조치', async (p) => {
      const boxes = p.locator('input[type=checkbox]'); await boxes.nth(0).check(); await boxes.nth(1).check();
      await p.locator('select[name=action]').selectOption('HIDE');
      await p.locator('input[name=note]').fill('광고성 게시물로 판단해 숨김');
      await p.locator('.decision').scrollIntoViewIfNeeded();
    }],
    ['/users', 'h1', '회원 검색과 정지·해제', null],
    ['/audit', 'h1', '감사 기록 — 누가 무엇을 했는지', null],
  ];
  for (let i = 0; i < consSteps.length; i++) {
    const [url, ready, caption, prepare] = consSteps[i];
    await conPage.goto(`${CONSOLE}${url}`); await conPage.locator(ready).first().waitFor({ timeout: 15000 }); await settle(conPage, 1000);
    if (prepare) { await prepare(conPage); await settle(conPage, 500); }
    const raw = await conPage.screenshot({ type: 'png' });
    cons.push({ png: await frame(raw, `localhost:4201${url}`, caption, i + 1), seconds: 2.6 });
  }
  toGif(cons, path.join(OUT, 'demo/console.gif'), { width: 960, fps: 10 });
  await adminCtx.close();

  // 챕터 생성은 프로젝트 상태를 바꾸므로 마지막에 합니다.
  log('· 챕터 생성 GIF (스트리밍)');
  const genCtx = await userContext(browser, `haerin@${DOMAIN}`);
  const gp = await genCtx.newPage();
  await gp.goto(`${WEB}/projects/${memory}/chapters`); await gp.locator('.workspace').waitFor(); await settle(gp, 1000);
  await gp.locator('.chapter-nav button', { hasText: '4.' }).click(); await settle(gp, 800);
  const caption = '챕터 생성 — 본문이 생성되는 대로 흘러나옵니다 (mock 생성기)';
  const url = `localhost:4200/projects/${memory}/chapters`;
  const gen = [{ png: await frame(await gp.screenshot({ type: 'png' }), url, caption, 1), seconds: 1.4 }];
  await gp.getByRole('button', { name: /챕터 생성/ }).click();
  const raws2 = [];
  const t0 = Date.now();
  let last = Date.now();
  while (Date.now() - t0 < 120000) {
    const writing = await gp.locator('.writing').isVisible().catch(() => false);
    if (!writing && raws2.length > 3) break;
    const raw = await gp.screenshot({ type: 'png' });
    const now = Date.now();
    raws2.push({ raw, seconds: Math.min(0.5, Math.max(0.12, (now - last) / 1000)) });
    last = now;
  }
  await gp.locator('.score').first().waitFor({ timeout: 20000 }).catch(() => {});
  await settle(gp, 800);
  const done = await gp.screenshot({ type: 'png' });
  // 프레임이 너무 많으면 균등하게 줄입니다.
  const keep = raws2.length > 60 ? raws2.filter((_, i) => i % Math.ceil(raws2.length / 60) === 0) : raws2;
  for (const f of keep) gen.push({ png: await frame(f.raw, url, caption, 1), seconds: f.seconds * (raws2.length / keep.length) });
  gen.push({ png: await frame(done, url, '생성이 끝나면 Continuity 검증 결과가 함께 나옵니다', 2), seconds: 3.5 });
  toGif(gen, path.join(OUT, 'demo/generate.gif'), { width: 960, fps: 10 });
  framesToMp4(gen, path.join(OUT, 'demo/generate.mp4'));
  await genCtx.close();

  await composerCtx.close();
}

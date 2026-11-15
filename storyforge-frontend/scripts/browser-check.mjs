// 실제 브라우저로 주요 화면을 확인하는 수동 점검 도구입니다. 자동 회귀 테스트(ng test)와 CI에는 들어가지 않습니다.
//
// 전제: DB, AI(mock), 백엔드(8080), 사용자 웹(4200), 운영 콘솔(4201)이 실행 중이어야 합니다.
//       백엔드는 ADMIN_EMAILS에 점검용 관리자 이메일을 넣고 실행합니다 (기본 browser-admin@example.com).
// 실행: storyforge-frontend 폴더에서
//       npm install --no-save playwright-core
//       node scripts/browser-check.mjs
// 환경 변수: WEB, CONSOLE, API, ADMIN_EMAIL, HEADED=1 (창을 띄움)
//
// 가입과 로그인 화면, 로그인 없이 읽기, 좁은 화면, 운영 콘솔 로그인을 눈으로 확인하는 대신 코드로 확인합니다.
// 프로젝트 준비(Bible, Outline, 챕터, 게시)는 API로 하고, 그 결과가 화면에 나타나는지를 봅니다.
// 새 계정과 작품을 만들므로 운영 DB가 아니라 로컬 개발 DB에서만 실행하세요.
import { chromium } from 'playwright-core';

const WEB = process.env.WEB ?? 'http://localhost:4200';
const CONSOLE = process.env.CONSOLE ?? 'http://localhost:4201';
const API = process.env.API ?? 'http://localhost:8080/api/v1';
const ADMIN_EMAIL = process.env.ADMIN_EMAIL ?? 'browser-admin@example.com';
const PASSWORD = 'check-password-1';
const stamp = Date.now();

let failed = 0;
let passed = 0;
function record(name, ok, detail = '') {
  if (ok) passed++; else failed++;
  console.log(`${ok ? 'PASS' : 'FAIL'} ${name}${ok || !detail ? '' : ` — ${detail}`}`);
}
async function check(name, fn) {
  try { await fn(); record(name, true); } catch (e) { record(name, false, String(e.message ?? e).split('\n')[0]); }
}
function assert(cond, message) { if (!cond) throw new Error(message); }

async function api(method, path, token, body) {
  const res = await fetch(`${API}${path}`, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await res.text();
  if (!res.ok) throw new Error(`${method} ${path} → ${res.status} ${text.slice(0, 120)}`);
  return text && res.headers.get('content-type')?.includes('json') ? JSON.parse(text) : text;
}

const browser = await chromium.launch({ channel: 'chrome', headless: !process.env.HEADED })
  .catch(() => chromium.launch({ channel: 'msedge', headless: !process.env.HEADED }));

const pageErrors = [];
function watch(page, label) {
  page.on('pageerror', (e) => pageErrors.push(`${label}: ${e.message}`));
  page.on('console', (m) => {
    // 외부 글꼴 CDN이 막힌 환경에서 나는 자원 오류와 의도한 401·403·409 응답은 제외합니다.
    if (m.type() === 'error' && !/Failed to load resource/.test(m.text())) pageErrors.push(`${label}: ${m.text()}`);
  });
}

try {
  const user = { email: `check-${stamp}@example.com`, name: `점검 ${stamp}` };
  const title = `브라우저 점검 ${stamp}`;
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 } });
  const page = await ctx.newPage();
  watch(page, 'user');

  // ───────── 가입 ─────────
  await check('가입 화면: 약관에 동의하지 않으면 가입 버튼이 비활성', async () => {
    await page.goto(`${WEB}/login`);
    await page.getByRole('button', { name: /계정이 없나요/ }).click();
    await page.locator('input[name=displayName]').fill(user.name);
    await page.locator('input[name=email]').fill(user.email);
    await page.locator('input[name=password]').fill(PASSWORD);
    assert(await page.getByRole('button', { name: '가입하기' }).isDisabled(), '약관 미동의인데 버튼이 활성입니다');
  });
  await check('가입: 약관에 동의하고 가입하면 내 프로젝트로 이동', async () => {
    await page.locator('mat-checkbox[name=acceptTerms] label').click();
    await page.getByRole('button', { name: '가입하기' }).click();
    await page.getByRole('heading', { name: '내 프로젝트' }).waitFor({ timeout: 10000 });
  });

  const session = JSON.parse(await page.evaluate(() => localStorage.getItem('storyforge.session')));
  const token = session.token;

  // ───────── 프로젝트 준비 (API) ─────────
  let projectId, workId, chapterTitle;
  await check('준비: Bible, Outline, 챕터 1 생성·승인, 게시 (API)', async () => {
    const project = await api('POST', '/projects', token, { title, genre: 'fantasy', form: 'LONG' });
    projectId = project.id;
    await api('POST', `/projects/${projectId}/bible/generate`, token, { prompt: '기억을 사고파는 가게' });
    await api('PUT', `/projects/${projectId}/bible/confirm`, token);
    await api('POST', `/projects/${projectId}/outline/generate`, token, { chapterCount: 2 });
    await api('PUT', `/projects/${projectId}/outline/confirm`, token);
    const res = await fetch(`${API}/projects/${projectId}/chapters/1/generate`, { method: 'POST', headers: { Authorization: `Bearer ${token}` } });
    const stream = await res.text();
    assert(/event:\s*done|"done"/.test(stream) || stream.includes('done'), '챕터 생성 스트림이 done 없이 끝났습니다');
    const chapters = await api('GET', `/projects/${projectId}/chapters`, token);
    assert(chapters.length >= 1, '생성된 챕터가 없습니다');
    chapterTitle = chapters[0].title;
    await api('PUT', `/chapters/${chapters[0].id}/confirm`, token);
    const work = await api('POST', `/projects/${projectId}/publish`, token,
      { kind: 'LONG', title, synopsis: '점검용 작품', genre: 'fantasy', tags: '점검', visibility: 'PUBLIC', clonePolicy: 'OPEN', downloadable: false });
    workId = work.id ?? work.card?.id;
    assert(workId, '게시 응답에 작품 ID가 없습니다');
  });

  // ───────── 로그인 상태 화면 ─────────
  await check('내 프로젝트: 만든 프로젝트가 목록에 보인다', async () => {
    await page.goto(`${WEB}/`);
    await page.getByText(title).first().waitFor({ timeout: 10000 });
  });
  for (const [tab, label] of [['bible', 'Bible'], ['outline', 'Outline'], ['chapters', '챕터'], ['activity', '작업 기록'], ['publish', '게시'], ['settings', '설정']]) {
    await check(`프로젝트 ${label} 화면이 그려진다`, async () => {
      await page.goto(`${WEB}/projects/${projectId}/${tab}`);
      await page.getByText(title).first().waitFor({ timeout: 10000 });
      assert(!(await page.locator('body').innerText()).includes('서버에 연결할 수 없습니다'), '서버 연결 오류가 표시됩니다');
    });
  }
  await check('챕터 화면에 생성된 챕터 제목이 보인다', async () => {
    await page.goto(`${WEB}/projects/${projectId}/chapters`);
    await page.getByText(chapterTitle).first().waitFor({ timeout: 10000 });
  });
  for (const [path, heading] of [['/package-forge', null], ['/me/works', null], ['/me/settings', null], ['/create', null], ['/issues', null]]) {
    await check(`${path} 화면이 그려진다`, async () => {
      await page.goto(`${WEB}${path}`);
      await page.locator('h1, h2').first().waitFor({ timeout: 10000 });
      assert(!page.url().includes('/login'), '로그인 화면으로 튕겼습니다');
    });
  }
  await check('내 프로필에 게시한 작품이 보인다', async () => {
    await page.goto(`${WEB}/users/${session.user.id}`);
    await page.getByText(title).first().waitFor({ timeout: 10000 });
  });

  // ───────── 로그인 없이 읽기 ─────────
  const guest = await (await browser.newContext({ viewport: { width: 1280, height: 900 } })).newPage();
  watch(guest, 'guest');
  await check('로그아웃 상태: 게시판에서 게시한 작품이 보인다', async () => {
    await guest.goto(`${WEB}/board/long`);
    await guest.getByText(title).first().waitFor({ timeout: 10000 });
  });
  await check('로그아웃 상태: 작품을 읽을 수 있다', async () => {
    await guest.goto(`${WEB}/works/${workId}`);
    await guest.getByRole('heading', { name: title }).first().waitFor({ timeout: 10000 });
    await guest.getByText(chapterTitle).first().waitFor({ timeout: 10000 });
  });
  await check('로그아웃 상태: 작품 응답에 이메일이 없다', async () => {
    const body = JSON.stringify(await api('GET', `/works/${workId}`));
    assert(!body.includes(user.email), '작품 응답에 작가 이메일이 들어 있습니다');
  });
  for (const path of ['/terms', '/issues', '/login']) {
    await check(`로그아웃 상태: ${path} 화면이 열린다`, async () => {
      await guest.goto(`${WEB}${path}`);
      await guest.locator('h1, h2, mat-card-title').first().waitFor({ timeout: 10000 });
    });
  }
  await check('로그아웃 상태: 내 프로젝트는 로그인 화면으로 보낸다', async () => {
    await guest.goto(`${WEB}/`);
    await guest.waitForURL(/\/login/, { timeout: 10000 });
  });

  // ───────── 좁은 화면 ─────────
  const phone = await (await browser.newContext({ viewport: { width: 390, height: 800 } })).newPage();
  watch(phone, 'phone');
  for (const path of ['/board/long', `/works/${workId}`, '/login', '/terms']) {
    await check(`좁은 화면(390px) ${path}: 가로로 넘치지 않는다`, async () => {
      await phone.goto(`${WEB}${path}`);
      // 좁은 화면에서는 접힌 사이드 메뉴의 제목이 먼저 잡히므로 보이는 제목만 기다립니다.
      await phone.locator('h1:visible, mat-card-title:visible').first().waitFor({ timeout: 10000 });
      const overflow = await phone.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
      assert(overflow <= 1, `가로 ${overflow}px 넘칩니다`);
    });
  }

  // ───────── 운영 콘솔 ─────────
  const consolePage = await (await browser.newContext({ viewport: { width: 1280, height: 900 } })).newPage();
  watch(consolePage, 'console');
  let adminReady = true;
  try {
    await api('POST', '/auth/register', null, { email: ADMIN_EMAIL, password: PASSWORD, displayName: '점검 관리자', acceptTerms: true });
  } catch { /* 이미 있으면 로그인만 합니다 */ }
  await check('운영 콘솔: 일반 계정은 로그인할 수 없다', async () => {
    await consolePage.goto(`${CONSOLE}/login`);
    await consolePage.locator('input[name=email]').fill(user.email);
    await consolePage.locator('input[name=password]').fill(PASSWORD);
    await consolePage.getByRole('button', { name: /운영 콘솔 접속/ }).click();
    await consolePage.getByRole('alert').waitFor({ timeout: 10000 });
    assert(/login/.test(consolePage.url()), '일반 계정이 콘솔로 들어갔습니다');
  });
  await check('운영 콘솔: 관리자 로그인 후 운영 현황이 보인다', async () => {
    await consolePage.goto(`${CONSOLE}/login`);
    await consolePage.locator('input[name=email]').fill(ADMIN_EMAIL);
    await consolePage.locator('input[name=password]').fill(PASSWORD);
    await consolePage.getByRole('button', { name: /운영 콘솔 접속/ }).click();
    await consolePage.getByRole('heading', { name: '운영 현황' }).waitFor({ timeout: 10000 })
      .catch((e) => { adminReady = false; throw new Error(`관리자 로그인 실패 — 백엔드를 ADMIN_EMAILS=${ADMIN_EMAIL}로 실행했는지 확인하세요`); });
  });
  if (adminReady) {
    for (const [path, heading] of [['/reports', null], ['/users', null], ['/audit', null]]) {
      await check(`운영 콘솔 ${path} 화면이 그려진다`, async () => {
        await consolePage.goto(`${CONSOLE}${path}`);
        await consolePage.locator('h1').first().waitFor({ timeout: 10000 });
        assert(!/login/.test(consolePage.url()), '로그인 화면으로 튕겼습니다');
      });
    }
  }

  await check('페이지 오류와 콘솔 오류가 없다', async () => {
    assert(pageErrors.length === 0, pageErrors.slice(0, 3).join(' | '));
  });
} finally {
  await browser.close();
}

console.log(`\n${failed === 0 ? 'ALL PASS' : 'FAILED'} — 통과 ${passed}, 실패 ${failed}`);
process.exit(failed === 0 ? 0 : 1);

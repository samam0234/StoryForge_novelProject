// 데모 DB에 샘플 계정과 작품을 채웁니다. 반드시 별도의 데모 DB에서만 실행하세요.
import { execFileSync } from 'node:child_process';
import { api, COVER_SIZE, coverHtml, log, renderHtml, upload } from './lib.mjs';
import { ADMIN, COMMENTS, DERIVED, DOMAIN, PACKAGE_COVER, PASSWORD, POSTS, POST_COMMENTS, SHARES, SPAM_COMMENT, SPAM_POST, USERS, WORKS } from './data.mjs';

const tokens = {};
const users = {};

async function register(key, name, email) {
  try {
    const r = await api('POST', '/auth/register', null, { email, password: PASSWORD, displayName: name, acceptTerms: true });
    tokens[key] = r.accessToken; users[key] = r.user;
  } catch (e) {
    if (e.status !== 409) throw e;
    const r = await api('POST', '/auth/login', null, { email, password: PASSWORD });
    tokens[key] = r.accessToken; users[key] = r.user;
  }
}

async function streamChapter(token, projectId, number) {
  const res = await fetch(`${process.env.API ?? 'http://localhost:8080/api/v1'}/projects/${projectId}/chapters/${number}/generate`, {
    method: 'POST', headers: { Authorization: `Bearer ${token}` },
  });
  const text = await res.text();
  if (!text.includes('done')) throw new Error(`챕터 ${number} 생성이 done 없이 끝났습니다`);
}

async function confirmChapter(token, id) {
  try { await api('PUT', `/chapters/${id}/confirm`, token); } catch (e) {
    if (e.status !== 409) throw e;
    await api('PUT', `/chapters/${id}/confirm`, token); // 재검증에서 한 번 멈춘 뒤 다시 승인
  }
}

/** 프로젝트 하나를 Bible → Outline → 챕터 승인 → 게시까지 진행합니다. */
async function build(key, spec, { projectId, startAt = 'new', cover } = {}) {
  const token = tokens[key];
  if (startAt === 'new') {
    const p = await api('POST', '/projects', token, { title: spec.title, genre: spec.genre, form: spec.kind, coverUrl: cover });
    projectId = p.id;
    await api('POST', `/projects/${projectId}/bible/generate`, token, { prompt: spec.prompt });
    await api('PUT', `/projects/${projectId}/bible`, token, spec.bible);
  }
  if (startAt === 'new' || startAt === 'bible') {
    await api('PUT', `/projects/${projectId}/bible/confirm`, token);
    await api('POST', `/projects/${projectId}/outline/generate`, token, { chapterCount: spec.outline.length });
    await api('PUT', `/projects/${projectId}/outline`, token, { chapters: spec.outline });
    await api('PUT', `/projects/${projectId}/outline/confirm`, token);
  }
  const chapters = spec.chapters ?? [spec.chapter];
  for (let n = 1; n <= chapters.length; n++) {
    await streamChapter(token, projectId, n);
    const list = await api('GET', `/projects/${projectId}/chapters`, token);
    const ch = list.find((c) => c.number === n);
    await api('PUT', `/chapters/${ch.id}`, token, { title: chapters[n - 1].title, content: chapters[n - 1].content });
    await confirmChapter(token, ch.id);
  }
  const work = await api('POST', `/projects/${projectId}/publish`, token, {
    kind: spec.kind, title: spec.title, synopsis: spec.synopsis, genre: spec.genre, tags: spec.tags,
    visibility: 'PUBLIC', clonePolicy: 'OPEN', downloadable: true,
  });
  return { projectId, workId: work.id ?? work.card?.id };
}

/** 이미 게시된 작품의 표지를 바꿉니다. */
async function setCover(browser, token, workId, spec) {
  const png = await renderHtml(browser, coverHtml(spec), { ...COVER_SIZE, scale: 1 });
  const url = await upload(token, png);
  const d = await api('GET', `/works/${workId}`, token);
  const c = d.card;
  await api('PUT', `/works/${workId}`, token, {
    title: c.title, synopsis: c.synopsis, genre: c.genre, tags: (c.tags ?? []).join(','), visibility: c.visibility,
    clonePolicy: c.clonePolicy, downloadable: d.downloadable, packageType: c.packageType ?? null,
    worldNote: d.packageView?.worldNote ?? null, coverUrl: url,
  });
}

export async function seed(browser) {
  log('· 계정');
  await register('admin', ADMIN.name, ADMIN.email);
  for (const [key, u] of Object.entries(USERS)) await register(key, u.name, `${key}@${DOMAIN}`);
  for (const [key, u] of Object.entries(USERS)) {
    await api('PUT', '/me/profile', tokens[key], { displayName: u.name, bio: u.bio ?? '', website: u.website ?? '', avatarColor: u.color });
  }

  log('· 작품 (표지 → Bible → Outline → 챕터 → 게시)');
  const made = {};
  for (const [id, spec] of Object.entries(WORKS)) {
    const png = await renderHtml(browser, coverHtml({ title: spec.title, cover: spec.cover, author: USERS[spec.author].name, kind: spec.kind }), { ...COVER_SIZE, scale: 1 });
    const cover = await upload(tokens[spec.author], png);
    made[id] = await build(spec.author, spec, { cover });
    log(`  ${spec.title} → 작품 ${made[id].workId}`);
  }

  log('· 진행 중인 프로젝트 (단계가 다른 두 개)');
  const wip1 = await api('POST', '/projects', tokens.haerin, { title: '폭풍이 오기 전의 도서관', genre: 'fantasy', form: 'LONG' });
  await api('POST', `/projects/${wip1.id}/bible/generate`, tokens.haerin, { prompt: '폭풍이 오기 전날 밤, 책들이 스스로 자리를 옮기는 도서관.' });
  const wip2 = await api('POST', '/projects', tokens.haerin, { title: '바다 건너의 편지', genre: 'drama', form: 'SHORT' });
  await api('POST', `/projects/${wip2.id}/bible/generate`, tokens.haerin, { prompt: '십 년 늦게 도착한 편지 한 통.' });
  await api('PUT', `/projects/${wip2.id}/bible/confirm`, tokens.haerin);
  await api('POST', `/projects/${wip2.id}/outline/generate`, tokens.haerin, { chapterCount: 3 });

  log('· Clone 파생작');
  const full = DERIVED.fullClone;
  const fc = await api('POST', `/works/${made.memory.workId}/clone`, tokens[full.author], { mode: full.mode, title: full.title });
  const fcChapters = await api('GET', `/projects/${fc.projectId}/chapters`, tokens[full.author]);
  for (const ch of fcChapters.sort((a, b) => a.number - b.number)) {
    if (ch.number === full.editChapter.number) {
      const detail = await api('GET', `/chapters/${ch.id}`, tokens[full.author]);
      await api('PUT', `/chapters/${ch.id}`, tokens[full.author], { title: detail.title, content: detail.content + full.editChapter.append });
    }
    await confirmChapter(tokens[full.author], ch.id);
  }
  const fw = await api('POST', `/projects/${fc.projectId}/publish`, tokens[full.author], {
    kind: 'LONG', title: full.title, synopsis: full.synopsis, genre: full.genre, tags: full.tags, visibility: 'PUBLIC', clonePolicy: 'OPEN', downloadable: true });
  made.fullClone = { projectId: fc.projectId, workId: fw.id ?? fw.card?.id };
  await setCover(browser, tokens[full.author], made.fullClone.workId, { title: full.title, cover: full.cover, author: USERS[full.author].name, kind: 'LONG' });

  const wc = DERIVED.worldClone;
  const wcl = await api('POST', `/works/${made.memory.workId}/clone`, tokens[wc.author], { mode: wc.mode, title: wc.title });
  made.worldClone = await build(wc.author, { ...wc, kind: 'LONG', chapter: wc.chapter }, { projectId: wcl.projectId, startAt: 'bible' });
  await setCover(browser, tokens[wc.author], made.worldClone.workId, { title: wc.title, cover: wc.cover, author: USERS[wc.author].name, kind: 'LONG' });

  log('· 함께 창작 (초대, 작업 커밋)');
  const memProject = made.memory.projectId;
  await api('POST', `/projects/${memProject}/collaborators`, tokens.haerin, { email: `doyun@${DOMAIN}` });
  const invites = await api('GET', '/me/invitations', tokens.doyun);
  const invite = (invites.items ?? invites)[0];
  await api('POST', `/me/invitations/${invite.id}/accept`, tokens.doyun);
  // 표시 구간(startOffset~endOffset)은 인용문(quote) 안의 위치입니다.
  const mark = (content, sentence) => {
    const para = content.split(/\n\n/).find((p) => p.includes(sentence));
    return { quote: para, startOffset: para.indexOf(sentence), endOffset: para.indexOf(sentence) + sentence.length };
  };
  const m1 = mark(WORKS.memory.chapters[1].content, '사람은 잊어서 사는 게 아니라, 잊을 걸 정해서 사는 거란다.');
  await api('POST', `/projects/${memProject}/commits`, tokens.doyun, { message: '할머니의 대사 호흡을 다듬음', target: 'CHAPTER', chapterNumber: 2, ...m1 });
  const m2 = mark(WORKS.memory.chapters[2].content, '병은 선물이 아니라, 누군가 그녀에게 건 내기였다.');
  await api('POST', `/projects/${memProject}/commits`, tokens.haerin, { message: '3화 마지막 문장을 확정', target: 'CHAPTER', chapterNumber: 3, ...m2 });

  log('· 패키지포지');
  const pkg = await api('POST', '/packages', tokens.haerin, {
    title: '망각로 유니버스', synopsis: '「기억을 파는 가게」와 그 외전을 한 세계로 묶은 패키지.', genre: 'fantasy', tags: '기억,유니버스',
    packageType: 'UNIVERSE', worldNote: '망각로의 규칙: 기억은 팔면 사라지고, 산 사람만 병을 한 번 열 수 있다.', visibility: 'PUBLIC',
    memberWorkIds: [made.memory.workId, made.lastguest.workId],
  });
  const pkgId = pkg.id ?? pkg.card?.id;
  made.package = { workId: pkgId };
  await setCover(browser, tokens.haerin, pkgId, { title: '망각로 유니버스', cover: PACKAGE_COVER, author: USERS.haerin.name, kind: 'PACKAGE' });
  const pd = await api('GET', `/works/${pkgId}`, tokens.haerin);
  const items = pd.packageView.items;
  if (items.length >= 2) {
    await api('POST', `/packages/${pkgId}/links`, tokens.haerin, { fromItemId: items[0].id, toItemId: items[1].id, type: 'SAME_WORLD', label: '같은 망각로' });
  }
  try { await api('POST', `/packages/${pkgId}/continuity`, tokens.haerin); } catch (e) { log('  (패키지 검사 건너뜀)', e.message); }

  log('· 반응 (추천, 댓글, 공유, 라운지)');
  const likers = Object.keys(USERS).filter((k) => k !== 'promo');
  const likeMap = { memory: likers, station: ['haerin', 'minjae', 'iseo', 'jian'], witness: ['haerin', 'minjae', 'sua', 'seoyun', 'doyun'], library: ['haerin', 'jian', 'iseo'], stars: ['doyun', 'haerin', 'hajun'], harbor: ['iseo', 'minjae'], lastguest: ['doyun', 'iseo', 'minjae', 'sua'] };
  for (const [work, who] of Object.entries(likeMap)) {
    for (const u of who) { if (u !== WORKS[work].author) await api('PUT', `/works/${made[work].workId}/like`, tokens[u]).catch(() => {}); }
  }
  for (const [work, list] of Object.entries(COMMENTS)) {
    for (const [u, text] of list) await api('POST', `/works/${made[work].workId}/comments`, tokens[u], { content: text });
  }
  for (const [channel, target] of SHARES) {
    const sh = await api('POST', `/works/${made.memory.workId}/shares`, tokens.haerin, { channel, target });
    const visits = { LINK: 6, X: 31, BLOG: 18, KAKAO: 24, COMMUNITY: 12 }[channel];
    for (let i = 0; i < visits; i++) await api('POST', `/shares/${sh.token}/visit`, null).catch(() => {});
  }
  const postIds = [];
  for (const p of POSTS) {
    const post = await api('POST', '/posts', tokens[p.author], { category: p.category, title: p.title, content: p.content });
    postIds.push(post.card?.id ?? post.id);
  }
  for (const [idx, u, text] of POST_COMMENTS) await api('POST', `/posts/${postIds[idx]}/comments`, tokens[u], { content: text });
  await api('PUT', `/posts/${postIds[0]}/like`, tokens.doyun).catch(() => {});
  await api('PUT', `/posts/${postIds[0]}/like`, tokens.iseo).catch(() => {});

  log('· 신고와 운영 처리');
  const spamComment = await api('POST', `/works/${made.memory.workId}/comments`, tokens.promo, { content: SPAM_COMMENT });
  const spamPost = await api('POST', '/posts', tokens.promo, SPAM_POST);
  const spamPostId = spamPost.card?.id ?? spamPost.id;
  const commentId = spamComment.id;
  await api('POST', '/reports', tokens.minjae, { targetType: 'COMMENT', targetId: commentId, reason: 'SPAM', detail: '광고성 댓글입니다.' });
  await api('POST', '/reports', tokens.sua, { targetType: 'COMMENT', targetId: commentId, reason: 'SPAM', detail: '같은 문구가 여러 작품에 달리고 있어요.' });
  await api('POST', '/reports', tokens.hajun, { targetType: 'POST', targetId: spamPostId, reason: 'SPAM', detail: '수익 보장 광고 글입니다.' });
  await api('POST', '/reports', tokens.jian, { targetType: 'USER', targetId: users.promo.id, reason: 'SPAM', detail: '여러 게시판에 광고를 반복해서 올립니다.' });
  await api('POST', '/reports', tokens.iseo, { targetType: 'WORK', targetId: made.harbor.workId, reason: 'COPYRIGHT', detail: '다른 작품의 첫 문단과 비슷해 보여 확인을 요청합니다.' });
  await api('POST', '/reports', tokens.seoyun, { targetType: 'POST', targetId: postIds[3], reason: 'OTHER', detail: '질문 글의 분류가 잘못된 것 같아요.' });
  const reports = await api('GET', '/admin/reports?status=OPEN&size=50', tokens.admin);
  const byTarget = (type, id) => (reports.items ?? []).find((r) => r.targetType === type && r.targetId === id);
  const hideComment = byTarget('COMMENT', commentId);
  if (hideComment) await api('PUT', `/admin/reports/${hideComment.id}`, tokens.admin, { action: 'HIDE', note: '광고성 댓글로 판단해 숨김' });
  const dismiss = byTarget('POST', postIds[3]);
  if (dismiss) await api('PUT', `/admin/reports/${dismiss.id}`, tokens.admin, { action: 'DISMISS', note: '분류 문제는 운영 대상이 아님' });
  const spamUser = byTarget('USER', users.promo.id);
  if (spamUser) await api('PUT', `/admin/reports/${spamUser.id}`, tokens.admin, { action: 'SUSPEND', note: '광고 반복 게시로 이용 정지' });

  return { made, users, tokens, postIds };
}

/** 활동 잔디, 조회수, 이슈 소식을 자연스럽게 보이도록 손봅니다. 데모 DB 전용입니다. */
export function spreadDates(made, container = 'storyforge-postgres', db = 'storyforge_demo', user = 'storyforge') {
  const w = (id) => made[id].workId;
  const sql = `
    -- 같은 활동을 여러 날에 복제하고, 원본도 최근 300일에 걸쳐 흩뿌려 잔디가 고르게 보이게 합니다.
    INSERT INTO activity_event (user_id, project_id, work_id, post_id, type, actor, summary, changed_chars, created_at)
    SELECT e.user_id, e.project_id, e.work_id, NULL, e.type, e.actor, e.summary, e.changed_chars, now()
    FROM activity_event e CROSS JOIN generate_series(1, 9) g
    WHERE e.type IN ('CHAPTER_EDITED','CHAPTER_GENERATED','BIBLE_EDITED','OUTLINE_EDITED','COMMENT_WRITTEN','WORK_UPDATED');
    UPDATE activity_event
       SET created_at = now() - ((power(random(), 1.7) * 300)::int || ' days')::interval - ((random() * 20)::int || ' hours')::interval
     WHERE id % 9 <> 0;
    UPDATE work SET view_count = 800 + id*173 + like_count*61;
    UPDATE work SET published_at = now() - (id * interval '9 hours') - interval '2 days' WHERE kind <> 'PACKAGE';
    UPDATE post SET created_at = now() - interval '9 days', updated_at = now() - interval '9 days' WHERE title LIKE '단 3일 만에%';
    INSERT INTO issue_event (work_id, metric, threshold, created_at) VALUES
      (${w('memory')}, 'TOTAL_VIEWS', 1000, now() - interval '3 hours'),
      (${w('memory')}, 'DAILY_LIKES', 5, now() - interval '20 hours'),
      (${w('witness')}, 'TOTAL_VIEWS', 1000, now() - interval '1 day'),
      (${w('memory')}, 'TOTAL_CLONES', 2, now() - interval '2 days'),
      (${w('station')}, 'TOTAL_LIKES', 3, now() - interval '3 days')
    ON CONFLICT DO NOTHING;
  `;
  execFileSync('docker', ['exec', '-i', container, 'psql', '-U', user, '-d', db, '-v', 'ON_ERROR_STOP=1'], { input: sql, stdio: ['pipe', 'inherit', 'inherit'] });
}

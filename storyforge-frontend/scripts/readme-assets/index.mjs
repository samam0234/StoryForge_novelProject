// README 소개 자료(스크린샷, GIF, 영상, 배너, 로고)를 만드는 수동 도구입니다.
//
//   node scripts/readme-assets/index.mjs seed      데모 DB에 샘플 데이터를 채움
//   node scripts/readme-assets/index.mjs capture   화면 캡처, GIF, 영상
//   node scripts/readme-assets/index.mjs brand [logos stack pipeline architecture hero]   로고·배너·다이어그램
//   node scripts/readme-assets/index.mjs all
//
// 전제와 절차는 docs/images/README.md에 있습니다.
import { launch, log } from './lib.mjs';
import { seed, spreadDates } from './seed.mjs';
import fs from 'node:fs';
import path from 'node:path';

const step = process.argv[2] ?? 'all';
const browser = await launch();
try {
  if (step === 'seed' || step === 'all') {
    const state = await seed(browser);
    spreadDates(state.made);
    const file = path.join(import.meta.dirname, '.state.json');
    fs.writeFileSync(file, JSON.stringify({ made: state.made, users: state.users, postIds: state.postIds }, null, 2));
    log('시드 완료 →', file);
  }
  if (step === 'brand' || step === 'all') {
    const { brand } = await import('./brand.mjs');
    await brand(browser, process.argv.slice(3).length ? process.argv.slice(3) : undefined);
  }
  if (step === 'capture' || step === 'all') {
    const { capture } = await import('./capture.mjs');
    await capture(browser);
  }
} finally {
  await browser.close();
}

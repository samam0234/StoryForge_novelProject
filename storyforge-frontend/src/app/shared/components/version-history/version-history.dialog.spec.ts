import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { VersionEntry, VersionHistoryData, VersionHistoryDialog } from './version-history.dialog';

const entry = (version: number, text: string): VersionEntry => ({
  version,
  createdAt: '2026-11-02T00:00:00Z',
  text,
  payload: { version },
});

function render(data: VersionHistoryData) {
  TestBed.configureTestingModule({
    providers: [
      { provide: MAT_DIALOG_DATA, useValue: data },
      { provide: MatDialogRef, useValue: { close: () => undefined } },
    ],
  });
  const fixture = TestBed.createComponent(VersionHistoryDialog);
  fixture.detectChanges();
  return { fixture, el: fixture.nativeElement as HTMLElement };
}

describe('VersionHistoryDialog', () => {
  it('기본으로 직전 버전과 최신 버전의 차이를 보여준다', () => {
    const { el } = render({ title: '이력', canRestore: true, versions: [entry(2, '새 문장'), entry(1, '옛 문장')] });
    expect(el.querySelector('.added')?.textContent).toContain('새');
    expect(el.querySelector('.removed')?.textContent).toContain('옛');
  });

  it('버전이 하나면 차이가 없다고 표시한다', () => {
    const { el } = render({ title: '이력', canRestore: true, versions: [entry(1, '문장')] });
    expect(el.querySelector('.added')).toBeNull();
    expect(el.textContent).toContain('두 버전이 같습니다');
  });

  it('이력이 없으면 안내한다', () => {
    const { el } = render({ title: '이력', canRestore: true, versions: [] });
    expect(el.textContent).toContain('아직 이력이 없습니다');
  });

  it('복원할 수 없으면 불러오기 버튼을 숨긴다', () => {
    const { el } = render({ title: '이력', canRestore: false, versions: [entry(1, '문장')] });
    expect(el.textContent).not.toContain('불러오기');
  });

  it('본문의 HTML을 실행하지 않고 글자로 보여준다', () => {
    const { el } = render({ title: '이력', canRestore: false, versions: [entry(1, '<img src=x onerror=alert(1)>')] });
    expect(el.querySelector('.diff img')).toBeNull();
    expect(el.querySelector('.diff')?.textContent).toContain('<img');
  });
});

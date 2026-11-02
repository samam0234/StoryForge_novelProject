import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Subject, of } from 'rxjs';
import { vi } from 'vitest';
import { CommunityApi } from '../../core/api/community-api.service';
import { GuideReply } from '../../core/models/community';
import { GuideChatComponent } from './guide-chat.component';

describe('GuideChatComponent', () => {
  afterEach(() => vi.restoreAllMocks());

  function setup(reply: GuideReply) {
    const guide = vi.fn(() => of(reply));
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: CommunityApi, useValue: { guide } }],
    });
    const fixture = TestBed.createComponent(GuideChatComponent);
    fixture.detectChanges();
    return { fixture, guide, el: fixture.nativeElement as HTMLElement };
  }

  async function send(fixture: ReturnType<typeof setup>['fixture'], el: HTMLElement) {
    el.querySelector<HTMLButtonElement>('.shortcuts button')!.click();
    await fixture.whenStable();
  }

  const actionButton = (el: HTMLElement) => el.querySelector<HTMLButtonElement>('.actions button')!;

  it('답변을 글자로만 보여 준다', async () => {
    const { fixture, el, guide } = setup({ reply: '<img src=x onerror=alert(1)> 안내', actions: [] });
    await send(fixture, el);
    expect(guide).toHaveBeenCalledWith('/도움말');
    expect(el.querySelector('.log img')).toBeNull();
    expect(el.querySelector('.log')?.textContent).toContain('<img src=x');
  });

  it('앱 안 이동 버튼은 라우터로 이동한다', async () => {
    const { fixture, el } = setup({ reply: '안내', actions: [{ label: '이슈', kind: 'route', target: '/issues' }] });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    await send(fixture, el);
    actionButton(el).click();
    expect(navigate).toHaveBeenCalledWith('/issues');
  });

  it('앱 경로가 아닌 route는 무시한다', async () => {
    const { fixture, el } = setup({ reply: '안내', actions: [{ label: '이상한', kind: 'route', target: 'https://evil.example' }] });
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    await send(fixture, el);
    actionButton(el).click();
    expect(navigate).not.toHaveBeenCalled();
  });

  it('외부 이동은 확인을 받고 새 창으로 연다', async () => {
    const { fixture, el } = setup({ reply: '안내', actions: [{ label: '이동', kind: 'external', target: 'https://chat.example' }] });
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true);
    const open = vi.spyOn(window, 'open').mockReturnValue(null);
    await send(fixture, el);
    actionButton(el).click();
    expect(confirm).toHaveBeenCalled();
    expect(open).toHaveBeenCalledWith('https://chat.example', '_blank', 'noopener,noreferrer');
  });

  it('http(s)가 아닌 외부 주소는 열지 않는다', async () => {
    const { fixture, el } = setup({ reply: '안내', actions: [{ label: '이동', kind: 'external', target: 'javascript:alert(1)' }] });
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true);
    const open = vi.spyOn(window, 'open').mockReturnValue(null);
    await send(fixture, el);
    actionButton(el).click();
    expect(confirm).not.toHaveBeenCalled();
    expect(open).not.toHaveBeenCalled();
  });

  it('응답을 기다리는 동안 중복으로 보내지 않는다', async () => {
    const pending = new Subject<GuideReply>();
    const guide = vi.fn(() => pending.asObservable());
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: CommunityApi, useValue: { guide } }],
    });
    const fixture = TestBed.createComponent(GuideChatComponent);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    await send(fixture, el);
    await send(fixture, el);
    expect(guide).toHaveBeenCalledTimes(1);
    pending.next({ reply: '끝', actions: [] });
  });
});

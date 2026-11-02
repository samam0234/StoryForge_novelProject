import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { CloneNetwork, Contributions, ShareGraph, WorkGraph } from '../../core/models/community';
import { CloneNetworkComponent } from './clone-network/clone-network.component';
import { ContributionHeatmapComponent } from './contribution-heatmap/contribution-heatmap.component';
import { ShareGraphComponent } from './share-graph/share-graph.component';
import { WorkGraphComponent } from './work-graph/work-graph.component';

const event = (id: number, day: string, actor: 'MANUAL' | 'AGENT', summary: string) => ({
  id,
  type: 'X',
  actor,
  summary,
  changedChars: 10,
  createdAt: `${day}T01:00:00Z`,
  day,
  workId: null,
  workTitle: null,
  postId: null,
  postTitle: null,
  projectId: null,
  projectTitle: null,
});

function renderGraph(graph: WorkGraph) {
  const fixture = TestBed.createComponent(WorkGraphComponent);
  fixture.componentRef.setInput('graph', graph);
  fixture.detectChanges();
  return fixture;
}

describe('WorkGraphComponent', () => {
  const graph: WorkGraph = {
    manual: { events: 3, chars: 25 },
    agent: { events: 1, chars: 75 },
    days: [
      { date: '2026-01-01', manual: 2, agent: 1, manualChars: 20, agentChars: 75 },
      { date: '2026-01-04', manual: 1, agent: 0, manualChars: 5, agentChars: 0 },
    ],
    events: [
      event(4, '2026-01-04', 'MANUAL', '다른 날 작업'),
      event(3, '2026-01-01', 'AGENT', '1장 AI 생성'),
      event(2, '2026-01-01', 'MANUAL', '1장 직접 수정'),
      event(1, '2026-01-01', 'MANUAL', '1장 승인'),
    ],
  };

  it('수동·에이전트 비율을 글자 수 기준으로 보여 준다', () => {
    const text = (renderGraph(graph).nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('수동 25%');
    expect(text).toContain('에이전트 75%');
  });

  it('쉬는 날도 칸을 두고, 작업 없는 칸은 누를 수 없다', () => {
    const el = renderGraph(graph).nativeElement as HTMLElement;
    const cols = [...el.querySelectorAll<HTMLButtonElement>('button.col')];
    const firstFour = cols.slice(0, 4);
    expect(firstFour.map((c) => c.disabled)).toEqual([false, true, true, false]);
  });

  it('막대를 누르면 그날 작업만 표에 나오고 다시 누르면 전체로 돌아간다', async () => {
    const fixture = renderGraph(graph);
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelectorAll('tbody tr').length).toBe(4);

    const first = el.querySelector<HTMLButtonElement>('button.col')!;
    first.click();
    await fixture.whenStable();
    const rows = [...el.querySelectorAll('tbody tr')].map((r) => r.textContent ?? '');
    expect(rows.length).toBe(3);
    expect(rows.join()).not.toContain('다른 날 작업');
    expect(first.getAttribute('aria-pressed')).toBe('true');

    first.click();
    await fixture.whenStable();
    expect(el.querySelectorAll('tbody tr').length).toBe(4);
  });

  it('주체를 색만이 아니라 글자로도 표시한다', () => {
    const body = (renderGraph(graph).nativeElement as HTMLElement).querySelector('tbody')?.textContent ?? '';
    expect(body).toContain('에이전트');
    expect(body).toContain('수동');
  });
});

describe('ContributionHeatmapComponent', () => {
  const data: Contributions = {
    from: '2026-01-04',
    to: '2026-01-14',
    total: 12,
    currentStreak: 1,
    longestStreak: 2,
    days: [
      { date: '2026-01-05', count: 1, level: 1 },
      { date: '2026-01-06', count: 11, level: 4 },
    ],
  };

  function render() {
    const fixture = TestBed.createComponent(ContributionHeatmapComponent);
    fixture.componentRef.setInput('data', data);
    fixture.detectChanges();
    return fixture;
  }

  it('활동이 많은 날일수록 높은 단계로 칠하고 건수를 레이블로 둔다', () => {
    const el = render().nativeElement as HTMLElement;
    const busy = el.querySelector('button[aria-label^="2026-01-06"]');
    expect(busy?.getAttribute('data-level')).toBe('4');
    expect(busy?.getAttribute('aria-label')).toContain('11건');
    expect(el.querySelector('button[aria-label^="2026-01-07"]')?.getAttribute('data-level')).toBe('0');
  });

  it('주 단위로 일요일부터 채우고 오늘 뒤는 누를 수 없다', () => {
    const el = render().nativeElement as HTMLElement;
    expect(el.querySelectorAll('.week').length).toBe(2);
    expect(el.querySelectorAll('button.cell').length).toBe(11);
    expect(el.querySelectorAll('.cell.none').length).toBe(3);
  });

  it('칸을 누르면 날짜를 내보낸다', () => {
    const fixture = render();
    const picked: string[] = [];
    fixture.componentInstance.pick.subscribe((d) => picked.push(d));
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button[aria-label^="2026-01-05"]')!.click();
    expect(picked).toEqual(['2026-01-05']);
  });
});

describe('ShareGraphComponent', () => {
  const user = (id: number, displayName: string) => ({ id, displayName, avatarColor: null });
  const share = (id: number, userId: number, name: string, channel: 'BLOG' | 'X', target: string | null) => ({
    id,
    user: user(userId, name),
    channel,
    target,
    visitCount: 2,
    createdAt: '2026-01-01T00:00:00Z',
  });

  function render(graph: ShareGraph) {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(ShareGraphComponent);
    fixture.componentRef.setInput('graph', graph);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('누가 어디에 공유했는지 그래프와 표로 보여 준다', () => {
    const el = render({ total: 3, visits: 6, items: [share(1, 1, '가', 'BLOG', '내 블로그'), share(2, 1, '가', 'BLOG', null), share(3, 2, '나', 'X', null)] });
    expect(el.querySelectorAll('svg path').length).toBe(2);
    expect(el.querySelector('table')?.textContent).toContain('내 블로그');
    expect(el.querySelector('svg')?.textContent).toContain('블로그 2');
  });

  it('공유가 없으면 안내만 보여 준다', () => {
    const el = render({ total: 0, visits: 0, items: [] });
    expect(el.querySelector('svg')).toBeNull();
    expect(el.textContent).toContain('아직 공유 기록이 없습니다');
  });

  it('입력값을 HTML로 넣지 않는다', () => {
    const el = render({ total: 1, visits: 0, items: [share(1, 1, '가', 'X', '<img src=x>')] });
    expect(el.querySelector('table img')).toBeNull();
    expect(el.querySelector('table')?.textContent).toContain('<img src=x>');
  });
});

describe('CloneNetworkComponent', () => {
  const node = (workId: number, parentId: number | null, current = false) => ({
    workId,
    parentId,
    title: `작품 ${workId}`,
    author: { id: 1, displayName: '가', avatarColor: null },
    revision: 1,
    likeCount: 0,
    cloneCount: 0,
    current,
  });

  it('원작에서 파생작으로 트리를 그리고 현재 작품을 표시한다', () => {
    const network: CloneNetwork = {
      rootId: 1,
      nodes: [node(1, null), node(2, 1, true), node(3, 1), node(4, 2)],
      world: 1,
      plot: 0,
      full: 2,
      clones: [],
    };
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(CloneNetworkComponent);
    fixture.componentRef.setInput('network', network);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelectorAll('rect').length).toBe(4);
    expect(el.querySelectorAll('svg path').length).toBe(3);
    expect(el.querySelectorAll('rect.current').length).toBe(1);
    expect(el.textContent).toContain('전체 원고 2');
  });
});

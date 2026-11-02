import { DatePipe } from '@angular/common';
import { Component, computed, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { WorkCommit } from '../../../core/models/community';

interface Row {
  commit: WorkCommit;
  lane: number;
  lanes: (number | null)[];
}

const TARGET: Record<WorkCommit['target'], string> = {
  BIBLE: 'Story Bible',
  OUTLINE: 'Outline',
  CHAPTER: '챕터',
};

/** 작업 커밋을 git 기록표처럼 보여 줍니다. 점을 누르면 메시지와 인용 구간이 열립니다. */
@Component({
  selector: 'sf-commit-graph',
  imports: [DatePipe, RouterLink],
  template: `
    @if (!rows().length) {
      <div class="sf-empty">아직 작업 커밋이 없습니다. 고친 구간을 적으면 참여자만 이 표에서 봅니다.</div>
    } @else {
      <ol class="log" aria-label="작업 커밋 기록표">
        @for (row of rows(); track row.commit.id) {
          <li>
            <button type="button" class="node" [class.selected]="selected() === row.commit.id" (click)="toggle(row.commit.id)">
              <span class="lanes" aria-hidden="true">
                @for (lane of row.lanes; track $index) {
                  <span class="lane">
                    @if ($index === row.lane) {
                      <i class="dot"></i>
                    } @else if (lane !== null) {
                      <i class="line"></i>
                    }
                  </span>
                }
              </span>
              <span class="msg">{{ row.commit.message }}</span>
              <span class="sf-muted meta">{{ row.commit.author.displayName }} · {{ where(row.commit) }} · {{ row.commit.createdAt | date: 'MM-dd HH:mm' }}</span>
            </button>
            @if (selected() === row.commit.id) {
              <article class="paragraph">
                <p>{{ row.commit.message }}</p>
                @if (row.commit.quote) {
                  <p class="quote">{{ parts(row.commit).before }}<mark>{{ parts(row.commit).mark }}</mark>{{ parts(row.commit).after }}</p>
                }
                @if (projectId()) {
                  <a [routerLink]="path(row.commit)" [queryParams]="{ commit: row.commit.id }">이 구간이 있는 화면으로</a>
                }
              </article>
            }
          </li>
        }
      </ol>
    }
  `,
  styles: `
    :host {
      display: block;
    }
    .log {
      list-style: none;
      margin: 0;
      padding: 0;
    }
    .node {
      display: flex;
      align-items: center;
      gap: 8px;
      width: 100%;
      padding: 6px 0;
      border: 0;
      background: transparent;
      color: var(--sf-text);
      font: inherit;
      text-align: left;
      cursor: pointer;
    }
    .node.selected .msg {
      font-weight: 650;
    }
    .lanes {
      display: flex;
      flex: none;
    }
    .lane {
      width: 14px;
      height: 18px;
      display: grid;
      place-items: center;
    }
    .dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      background: var(--sf-series-manual);
      box-shadow: 0 0 0 2px var(--sf-surface);
    }
    .line {
      width: 2px;
      height: 18px;
      background: var(--sf-border);
    }
    .msg {
      flex: 1;
      min-width: 0;
    }
    .meta {
      font-size: 12px;
      white-space: nowrap;
    }
    .paragraph {
      margin: 0 0 8px 14px;
      padding: 8px 10px;
      border: 1px solid var(--sf-border);
      border-radius: 6px;
      background: var(--sf-surface-2);
      font-size: 13px;
      line-height: 1.55;
    }
    .paragraph p {
      margin: 0 0 6px;
    }
    .quote {
      white-space: pre-wrap;
    }
    mark {
      background: color-mix(in srgb, var(--sf-series-manual) 45%, transparent);
      color: inherit;
    }
    a {
      color: var(--sf-emphasis);
    }
  `,
})
export class CommitGraphComponent {
  readonly commits = input.required<WorkCommit[]>();
  readonly projectId = input<number | null>(null);
  protected readonly selected = signal<number | null>(null);

  protected readonly rows = computed<Row[]>(() => layout(this.commits()));

  protected toggle(id: number): void {
    this.selected.update((current) => (current === id ? null : id));
  }

  protected where(commit: WorkCommit): string {
    return commit.target === 'CHAPTER' && commit.chapterNumber ? `${commit.chapterNumber}장` : TARGET[commit.target];
  }

  protected parts(commit: WorkCommit): { before: string; mark: string; after: string } {
    const quote = commit.quote ?? '';
    const start = commit.startOffset ?? 0;
    const end = commit.endOffset ?? quote.length;
    const s = Math.max(0, Math.min(start, quote.length));
    const e = Math.max(s, Math.min(end, quote.length));
    return { before: quote.slice(0, s), mark: quote.slice(s, e), after: quote.slice(e) };
  }

  protected path(commit: WorkCommit): (string | number)[] {
    const id = this.projectId();
    if (id == null) return ['/'];
    if (commit.target === 'BIBLE') return ['/projects', id, 'bible'];
    if (commit.target === 'OUTLINE') return ['/projects', id, 'outline'];
    return ['/projects', id, 'chapters'];
  }
}

/** 최신이 위인 커밋을 레인에 배치합니다. 부모가 같으면 줄이 갈라집니다. */
function layout(commits: WorkCommit[]): Row[] {
  const active: (number | null)[] = [];
  const rows: Row[] = [];
  for (const commit of commits) {
    let lane = active.indexOf(commit.id);
    if (lane < 0) {
      lane = active.indexOf(null);
      if (lane < 0) {
        lane = active.length;
        active.push(null);
      }
    }
    active[lane] = commit.parentId;
    if (commit.parentId != null) {
      for (let i = 0; i < active.length; i++) {
        if (i !== lane && active[i] === commit.parentId) active[i] = null;
      }
    }
    rows.push({ commit, lane, lanes: [...active] });
  }
  return rows;
}

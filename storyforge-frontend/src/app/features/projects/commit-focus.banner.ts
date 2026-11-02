import { Component, effect, inject, input, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { filter, map, startWith } from 'rxjs';
import { CommunityApi } from '../../core/api/community-api.service';
import { WorkCommit } from '../../core/models/community';

/** 주소의 commit 쿼리가 가리키는 인용 구간을 현재 화면 위에 표시합니다. */
@Component({
  selector: 'sf-commit-focus',
  template: `
    @if (commit(); as c) {
      <aside class="focus" aria-label="선택한 작업 구간">
        <strong>{{ c.author.displayName }}</strong>
        <span>{{ c.message }}</span>
        @if (c.quote) {
          <p class="quote">{{ before() }}<mark>{{ mark() }}</mark>{{ after() }}</p>
        }
      </aside>
    }
  `,
  styles: `
    .focus {
      display: flex;
      flex-direction: column;
      gap: 4px;
      margin: 0 24px 8px;
      padding: 8px 10px;
      border: 1px solid var(--sf-border);
      border-radius: 6px;
      background: var(--sf-surface-2);
      font-size: 13px;
    }
    .quote {
      margin: 0;
      white-space: pre-wrap;
      line-height: 1.55;
    }
    mark {
      background: color-mix(in srgb, var(--sf-series-manual) 45%, transparent);
      color: inherit;
    }
  `,
})
export class CommitFocusBanner {
  readonly projectId = input.required<number>();

  private readonly api = inject(CommunityApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly commit = signal<WorkCommit | null>(null);
  private readonly commitId = toSignal(
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      startWith(null),
      map(() => this.leafCommitId()),
    ),
    { initialValue: this.leafCommitId() },
  );

  constructor() {
    effect(() => {
      const projectId = this.projectId();
      const id = Number(this.commitId());
      if (!id) {
        this.commit.set(null);
        return;
      }
      this.api.commit(projectId, id).subscribe({
        next: (c) => this.commit.set(c),
        error: () => this.commit.set(null),
      });
    });
  }

  protected before(): string {
    return slice(this.commit()).before;
  }
  protected mark(): string {
    return slice(this.commit()).mark;
  }
  protected after(): string {
    return slice(this.commit()).after;
  }

  private leafCommitId(): string | null {
    let current: ActivatedRoute | null = this.route;
    while (current?.firstChild) current = current.firstChild;
    return current?.snapshot.queryParamMap.get('commit') ?? null;
  }
}

function slice(commit: WorkCommit | null): { before: string; mark: string; after: string } {
  const quote = commit?.quote ?? '';
  const start = commit?.startOffset ?? 0;
  const end = commit?.endOffset ?? quote.length;
  const s = Math.max(0, Math.min(start, quote.length));
  const e = Math.max(s, Math.min(end, quote.length));
  return { before: quote.slice(0, s), mark: quote.slice(s, e), after: quote.slice(e) };
}

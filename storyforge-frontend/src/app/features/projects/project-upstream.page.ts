import { Component, computed, effect, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { diffWordsWithSpace } from 'diff';
import { CommunityApi } from '../../core/api/community-api.service';
import { errorMessage } from '../../core/api/storyforge-api.service';
import { CLONE_MODES, UpstreamChapter, UpstreamCompare, labelOf } from '../../core/models/community';
import { ProjectContext } from './project-context';

const STATE_LABEL: Record<UpstreamChapter['state'], string> = {
  SAME: '같음',
  CHANGED: '달라짐',
  ONLY_UPSTREAM: '원본에만 있음',
  ONLY_LOCAL: '내 프로젝트에만 있음',
};

/** Clone한 프로젝트와 원본 최신 게시본 비교 */
@Component({
  selector: 'sf-project-upstream-page',
  imports: [RouterLink, MatButtonModule, MatIconModule, MatProgressBarModule],
  template: `
    <div class="sf-page">
      @if (error()) {
        <div class="sf-empty" role="alert">{{ error() }}</div>
      } @else if (!compare()) {
        <mat-progress-bar mode="indeterminate" />
      } @else if (compare(); as c) {
        <header class="head">
          <h2>원본: {{ c.title }}</h2>
          <p class="sf-muted">
            {{ modeLabel() }} 범위로 r{{ c.clonedRevision }}에서 Clone했습니다.
            @if (c.available) {
              원본 최신은 r{{ c.currentRevision }}입니다.
              @if (c.currentRevision > c.clonedRevision) {
                <strong class="behind">원본이 {{ c.currentRevision - c.clonedRevision }}개 리비전 앞서 있습니다.</strong>
              }
            }
          </p>
          <div class="sf-row">
            @if (c.workId) {
              <a mat-stroked-button [routerLink]="['/works', c.workId]"><mat-icon>open_in_new</mat-icon> 원본 보기</a>
            }
            @if (c.available && c.currentRevision > c.clonedRevision) {
              <button mat-button [disabled]="busy()" (click)="acknowledge()">r{{ c.currentRevision }}까지 확인함</button>
            }
          </div>
        </header>
        @if (!c.available) {
          <div class="sf-empty">원본이 삭제되었거나 비공개로 바뀌어 비교할 수 없습니다. 내 프로젝트는 그대로 쓸 수 있습니다.</div>
        } @else {
          <div class="layout">
            <ul class="rows" aria-label="챕터별 차이">
              @for (r of c.chapters; track r.number) {
                <li>
                  <button type="button" [class.on]="selected() === r.number" (click)="selected.set(r.number)">
                    <span>{{ r.number }}장. {{ r.localTitle || r.upstreamTitle }}</span>
                    <span class="sf-badge" [attr.data-state]="r.state">{{ stateLabel[r.state] }}</span>
                  </button>
                </li>
              }
            </ul>
            <div class="diff" aria-label="선택한 챕터 비교">
              @if (current(); as cur) {
                <div class="sf-row diff-head">
                  <p class="sf-muted legend"><span class="added">내가 추가</span> · <span class="removed">원본에 있던 내용</span></p>
                  <span class="sf-spacer"></span>
                  @if (canPull(cur)) {
                    <button mat-stroked-button [disabled]="busy()" (click)="pull(cur)"><mat-icon>download</mat-icon> 원본으로 덮어쓰기</button>
                  }
                </div>
                <p class="body">
                  @for (p of parts(); track $index) {
                    <span [class.added]="p.added" [class.removed]="p.removed">{{ p.value }}</span>
                  }
                </p>
              } @else {
                <div class="sf-empty">챕터를 고르면 차이가 나옵니다.</div>
              }
            </div>
          </div>
        }
      }
    </div>
  `,
  styles: `
    .head {
      margin-bottom: 16px;
    }
    h2 {
      margin: 0 0 4px;
      font-size: 18px;
    }
    .behind {
      color: var(--sf-severity-medium);
    }
    .layout {
      display: grid;
      grid-template-columns: 280px minmax(0, 1fr);
      gap: 12px;
    }
    @media (max-width: 800px) {
      .layout {
        grid-template-columns: 1fr;
      }
    }
    .rows {
      list-style: none;
      margin: 0;
      padding: 0;
    }
    .rows button {
      width: 100%;
      display: flex;
      justify-content: space-between;
      gap: 8px;
      align-items: center;
      padding: 8px;
      text-align: left;
      font: inherit;
      font-size: 14px;
      color: var(--sf-text);
      border: 0;
      border-radius: 6px;
      background: none;
      cursor: pointer;
    }
    .rows button.on {
      background: color-mix(in srgb, var(--sf-secondary) 18%, transparent);
    }
    .sf-badge[data-state='SAME'] {
      background: var(--sf-surface-2);
      color: var(--sf-text-muted);
    }
    .diff {
      padding: 12px;
      border: 1px solid var(--sf-border);
      border-radius: var(--sf-radius);
      background: var(--sf-surface);
      min-height: 200px;
    }
    .body {
      white-space: pre-wrap;
      overflow-wrap: anywhere;
      margin: 0;
      line-height: 1.75;
    }
    .diff-head {
      margin-bottom: 8px;
    }
    .legend {
      margin: 0;
      font-size: 12px;
    }
    .added {
      background: color-mix(in srgb, var(--sf-series-manual) 30%, transparent);
      text-decoration: underline;
    }
    .removed {
      background: color-mix(in srgb, var(--sf-series-agent) 30%, transparent);
      text-decoration: line-through;
    }
  `,
})
export class ProjectUpstreamPage {
  private readonly ctx = inject(ProjectContext);
  private readonly api = inject(CommunityApi);
  private readonly snack = inject(MatSnackBar);

  protected readonly stateLabel = STATE_LABEL;
  protected readonly compare = signal<UpstreamCompare | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly selected = signal<number | null>(null);
  protected readonly busy = signal(false);
  protected readonly current = computed(() => this.compare()?.chapters.find((c) => c.number === this.selected()) ?? null);
  protected readonly parts = computed(() => {
    const c = this.current();
    return c ? diffWordsWithSpace(c.upstreamContent ?? '', c.localContent ?? '') : [];
  });
  protected readonly modeLabel = computed(() => labelOf(CLONE_MODES, this.compare()?.mode));

  /** 전체 원고 Clone이 검토 단계일 때, 아직 승인하지 않았고 달라진 챕터만 가져올 수 있습니다. 서버가 다시 확인합니다. */
  protected canPull(r: UpstreamChapter): boolean {
    const confirmed = this.ctx.statusInfo()?.confirmedChapters ?? 0;
    return this.compare()?.mode === 'FULL' && this.ctx.is('CHAPTER_REVIEW') && r.upstreamContent !== null && r.state !== 'SAME' && r.number > confirmed;
  }

  protected pull(r: UpstreamChapter): void {
    if (!confirm(`${r.number}장을 원본 내용으로 덮어쓸까요? 지금 내용은 챕터 이력에 남습니다.`)) return;
    this.run(this.api.pullUpstreamChapter(this.ctx.projectId() as number, r.number), `${r.number}장을 가져왔습니다. 승인할 때 다시 검증합니다.`);
  }

  protected acknowledge(): void {
    this.run(this.api.acknowledgeUpstream(this.ctx.projectId() as number), '확인으로 표시했습니다.');
  }

  private run(request: Observable<UpstreamCompare>, message: string): void {
    this.busy.set(true);
    request.subscribe({
      next: (c) => {
        this.busy.set(false);
        this.compare.set(c);
        this.snack.open(message, undefined, { duration: 3000 });
      },
      error: (e) => {
        this.busy.set(false);
        this.snack.open(errorMessage(e), '닫기', { duration: 4000 });
      },
    });
  }

  constructor() {
    effect(() => {
      const id = this.ctx.projectId();
      if (id == null) return;
      this.api.upstream(id).subscribe({
        next: (c) => {
          this.compare.set(c);
          this.selected.set(c.chapters.find((r) => r.state !== 'SAME')?.number ?? c.chapters[0]?.number ?? null);
        },
        error: (e) => this.error.set(errorMessage(e)),
      });
    });
  }
}

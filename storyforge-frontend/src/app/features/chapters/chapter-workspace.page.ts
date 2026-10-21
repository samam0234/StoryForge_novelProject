import { Component, DestroyRef, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subscription, forkJoin } from 'rxjs';
import { ChapterStream } from '../../core/api/chapter-stream.service';
import { StoryforgeApi, errorMessage } from '../../core/api/storyforge-api.service';
import { Chapter, OutlineChapter, StoryBible } from '../../core/models/models';
import { ProjectContext } from '../projects/project-context';

/**
 * 챕터 워크스페이스 (기획서 §9.6 4번)
 * 좌: Outline + Bible 컨텍스트 / 중앙: 에디터(스트리밍) / 우: Continuity 리포트, 캐릭터 상태 / 하단: 승인·재생성
 * TODO(editor): 기획서 §9.2의 TipTap 에디터로 교체
 */
@Component({
  selector: 'sf-chapter-workspace-page',
  imports: [
    FormsModule,
    MatButtonModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatTooltipModule,
  ],
  template: `
    @if (!ready()) {
      <div class="sf-page">
        @if (ctx.status() && !ctx.is('OUTLINE_CONFIRMED', 'CHAPTER_GENERATING', 'CHAPTER_REVIEW', 'COMPLETED')) {
          <div class="sf-empty">Outline을 확정하면 챕터를 생성할 수 있습니다.</div>
        } @else {
          <mat-progress-bar mode="indeterminate" />
        }
      </div>
    } @else {
      <div class="workspace">
        <!-- 좌측: Outline + Bible 컨텍스트 -->
        <aside class="col left">
          <h3>챕터</h3>
          <ol class="chapter-nav">
            @for (o of outline(); track o.order) {
              <li>
                <button [class.selected]="o.order === selected()" (click)="select(o.order)" [disabled]="streaming()">
                  <mat-icon class="state">{{ stateIcon(o.order) }}</mat-icon>
                  <span>{{ o.order }}. {{ o.title }}</span>
                </button>
              </li>
            }
          </ol>

          @if (outlineChapter(); as oc) {
            <h3>Outline</h3>
            <div class="block">
              @if (oc.goals) {
                <p><strong>목표</strong> {{ oc.goals }}</p>
              }
              <ul>
                @for (b of oc.beats; track $index) {
                  <li>{{ b }}</li>
                }
              </ul>
              @if (oc.characters.length) {
                <p class="sf-muted">등장: {{ oc.characters.join(', ') }}</p>
              }
              @if (oc.continuityPoints.length) {
                <p class="sf-muted">연속성: {{ oc.continuityPoints.join(' / ') }}</p>
              }
            </div>
          }

          <mat-accordion displayMode="flat">
            <mat-expansion-panel class="ctx-panel">
              <mat-expansion-panel-header><mat-panel-title>Bible 컨텍스트</mat-panel-title></mat-expansion-panel-header>
              <p class="pre">{{ bible()?.worldSetting }}</p>
              <p class="sf-muted"><strong>테마</strong> {{ bible()?.theme }}</p>
              <p class="sf-muted"><strong>톤</strong> {{ bible()?.tone }}</p>
            </mat-expansion-panel>
          </mat-accordion>
        </aside>

        <!-- 중앙: 에디터 -->
        <section class="col center">
          <div class="sf-row center-head">
            <h2>{{ selected() }}장. {{ current()?.title || outlineChapter()?.title }}</h2>
            <span class="sf-spacer"></span>
            @if (current(); as c) {
              <span class="chip" [class.ok]="c.status === 'CONFIRMED'">{{ c.status === 'CONFIRMED' ? '확정' : '검토 중' }}</span>
            }
          </div>

          @if (streaming()) {
            <div class="writing"><span class="pulse"></span> AI writing...</div>
            <article class="stream">{{ streamText() }}</article>
          } @else if (current(); as c) {
            @if (editable()) {
              <mat-form-field appearance="outline" class="sf-full" subscriptSizing="dynamic">
                <mat-label>제목</mat-label>
                <input matInput [(ngModel)]="editTitle" (ngModelChange)="dirty.set(true)" />
              </mat-form-field>
              <textarea class="editor" [(ngModel)]="editContent" (ngModelChange)="dirty.set(true)"></textarea>
            } @else {
              <article class="stream">{{ c.content }}</article>
            }
          } @else {
            <div class="sf-empty">
              @if (selected() === nextNumber()) {
                아직 생성되지 않은 챕터입니다. 아래에서 생성하세요.
              } @else {
                앞 챕터를 먼저 확정해야 이 챕터를 생성할 수 있습니다.
              }
            </div>
          }

          <footer class="sf-row actions">
            @if (ctx.is('CHAPTER_GENERATING') && !streaming()) {
              <span class="sf-muted">다른 곳에서 생성 중입니다. 잠시 후 새로고침하세요.</span>
            }
            <span class="sf-spacer"></span>
            @if (editable() && dirty()) {
              <button mat-stroked-button (click)="saveEdit()" [disabled]="busy()"><mat-icon>save</mat-icon> 수정 저장</button>
            }
            @if (canGenerate()) {
              <button mat-stroked-button (click)="generate()">
                <mat-icon>{{ current() ? 'refresh' : 'auto_awesome' }}</mat-icon>
                {{ current() ? '재생성' : '챕터 생성' }}
              </button>
            }
            @if (streaming()) {
              <button mat-stroked-button (click)="stop()"><mat-icon>stop</mat-icon> 중단</button>
            }
            @if (canConfirm()) {
              <button mat-flat-button (click)="approve()" [disabled]="busy() || dirty()">
                <mat-icon>check_circle</mat-icon> 승인
              </button>
            }
          </footer>
        </section>

        <!-- 우측: Continuity + 캐릭터 상태 -->
        <aside class="col right">
          <h3>Continuity Check</h3>
          @if (current()?.continuityReport; as r) {
            <div class="score" [class.low]="r.score < 70">
              <span class="value">{{ r.score }}</span><span class="sf-muted">/ 100</span>
            </div>
            @if (!r.issues.length) {
              <p class="sf-muted">발견된 이슈가 없습니다.</p>
            }
            <ul class="issues">
              @for (i of r.issues; track $index) {
                <li [attr.data-severity]="i.severity">
                  <span class="sev">{{ i.severity }}</span>
                  <span class="type">{{ i.type }}</span>
                  <p>{{ i.description }}</p>
                </li>
              }
            </ul>
          } @else {
            <p class="sf-muted">챕터를 생성하면 검증 결과가 여기에 표시됩니다.</p>
          }

          <h3>캐릭터</h3>
          <ul class="chars">
            @for (c of bible()?.characters ?? []; track c.name) {
              <li [class.in-chapter]="outlineChapter()?.characters?.includes(c.name)">
                <strong>{{ c.name }}</strong>
                <span class="sf-muted">{{ c.personality }}</span>
              </li>
            }
          </ul>
        </aside>
      </div>
    }
  `,
  styles: `
    .workspace {
      display: grid;
      grid-template-columns: 280px minmax(0, 1fr) 300px;
      height: calc(100vh - 64px - 150px);
      min-height: 520px;
    }
    .col {
      overflow-y: auto;
      padding: 16px;
      box-sizing: border-box;
    }
    .left,
    .right {
      background: var(--sf-surface);
      font-size: 14px;
    }
    .left {
      border-right: 1px solid var(--sf-border);
    }
    .right {
      border-left: 1px solid var(--sf-border);
    }
    h3 {
      font-size: 12px;
      text-transform: uppercase;
      letter-spacing: 0.06em;
      color: var(--sf-text-muted);
      margin: 16px 0 8px;
    }
    h3:first-child {
      margin-top: 0;
    }
    .chapter-nav {
      list-style: none;
      margin: 0;
      padding: 0;
    }
    .chapter-nav button {
      all: unset;
      display: flex;
      align-items: center;
      gap: 6px;
      width: 100%;
      box-sizing: border-box;
      padding: 6px 8px;
      border-radius: 6px;
      cursor: pointer;
    }
    .chapter-nav button:hover {
      background: var(--sf-surface-2);
    }
    .chapter-nav button.selected {
      background: color-mix(in srgb, var(--sf-secondary) 22%, transparent);
      font-weight: 600;
    }
    .state {
      font-size: 18px;
      width: 18px;
      height: 18px;
      color: var(--sf-text-muted);
    }
    .block ul {
      padding-left: 18px;
      margin: 4px 0;
    }
    .ctx-panel {
      background: var(--sf-surface-2);
      margin-top: 12px;
    }
    .pre {
      white-space: pre-wrap;
    }
    .center {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .center-head h2 {
      margin: 0;
      font-size: 20px;
    }
    .chip {
      font-size: 12px;
      padding: 2px 10px;
      border-radius: 999px;
      background: color-mix(in srgb, var(--sf-accent) 20%, transparent);
    }
    .chip.ok {
      background: color-mix(in srgb, var(--sf-secondary) 25%, transparent);
    }
    .writing {
      display: flex;
      align-items: center;
      gap: 8px;
      color: var(--sf-accent);
      font-size: 13px;
    }
    .pulse {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: var(--sf-accent);
      animation: pulse 1s infinite alternate;
    }
    @keyframes pulse {
      to {
        opacity: 0.2;
      }
    }
    .stream,
    .editor {
      flex: 1;
      white-space: pre-wrap;
      line-height: 1.85;
      font-size: 16px;
      padding: 20px 24px;
      border-radius: var(--sf-radius);
      background: var(--sf-surface);
      border: 1px solid var(--sf-border);
      color: var(--sf-text);
      overflow-y: auto;
    }
    .editor {
      font-family: inherit;
      resize: none;
      outline: none;
    }
    .editor:focus {
      border-color: var(--sf-secondary);
    }
    .actions {
      padding-top: 4px;
    }
    .score {
      display: flex;
      align-items: baseline;
      gap: 4px;
    }
    .score .value {
      font-size: 36px;
      font-weight: 700;
      color: var(--sf-emphasis);
    }
    .score.low .value {
      color: var(--sf-severity-high);
    }
    .issues,
    .chars {
      list-style: none;
      padding: 0;
      margin: 0;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .issues li {
      padding: 8px 10px;
      border-radius: 6px;
      background: var(--sf-surface-2);
      border-left: 3px solid var(--sf-severity-low);
    }
    .issues li[data-severity='high'] {
      border-left-color: var(--sf-severity-high);
    }
    .issues li[data-severity='medium'] {
      border-left-color: var(--sf-severity-medium);
    }
    .issues p {
      margin: 4px 0 0;
    }
    .sev {
      font-size: 11px;
      text-transform: uppercase;
      font-weight: 700;
      margin-right: 6px;
    }
    .type {
      font-size: 12px;
      color: var(--sf-text-muted);
    }
    .chars li {
      display: flex;
      flex-direction: column;
      padding: 6px 8px;
      border-radius: 6px;
    }
    .chars li.in-chapter {
      background: color-mix(in srgb, var(--sf-secondary) 14%, transparent);
    }
  `,
})
export class ChapterWorkspacePage {
  protected readonly ctx = inject(ProjectContext);
  private readonly api = inject(StoryforgeApi);
  private readonly stream = inject(ChapterStream);
  private readonly snack = inject(MatSnackBar);

  protected readonly outline = signal<OutlineChapter[]>([]);
  protected readonly bible = signal<StoryBible | null>(null);
  protected readonly chapters = signal<Chapter[]>([]);
  protected readonly ready = signal(false);
  protected readonly selected = signal(1);
  protected readonly streaming = signal(false);
  protected readonly streamText = signal('');
  protected readonly busy = signal(false);
  protected readonly dirty = signal(false);
  protected editTitle = '';
  protected editContent = '';
  private streamSub?: Subscription;

  protected readonly nextNumber = computed(() => this.ctx.statusInfo()?.nextChapterNumber ?? null);
  protected readonly current = computed(() => this.chapters().find((c) => c.number === this.selected()) ?? null);
  protected readonly outlineChapter = computed(() => this.outline().find((o) => o.order === this.selected()) ?? null);
  protected readonly editable = computed(
    () => this.ctx.is('CHAPTER_REVIEW') && this.current()?.status === 'DRAFT' && !this.streaming(),
  );
  protected readonly canGenerate = computed(
    () => this.ctx.is('OUTLINE_CONFIRMED', 'CHAPTER_REVIEW') && this.selected() === this.nextNumber() && !this.streaming(),
  );
  protected readonly canConfirm = computed(
    () => this.ctx.is('CHAPTER_REVIEW') && this.current()?.status === 'DRAFT' && !this.streaming(),
  );

  constructor() {
    inject(DestroyRef).onDestroy(() => this.streamSub?.unsubscribe());

    effect(() => {
      const id = this.ctx.projectId();
      const ok = this.ctx.is('OUTLINE_CONFIRMED', 'CHAPTER_GENERATING', 'CHAPTER_REVIEW', 'COMPLETED');
      if (id == null || !ok || untracked(this.ready)) return;
      forkJoin([this.api.getOutline(id), this.api.getBible(id), this.api.listChapters(id)]).subscribe({
        next: ([o, b, list]) => {
          this.outline.set(o?.chapters ?? []);
          this.bible.set(b);
          this.chapters.set(list);
          this.select(this.nextNumber() ?? list.at(-1)?.number ?? 1);
          this.ready.set(true);
        },
        error: (e) => this.toast(e),
      });
    });
  }

  protected stateIcon(n: number): string {
    const c = this.chapters().find((x) => x.number === n);
    if (c?.status === 'CONFIRMED') return 'check_circle';
    if (c) return 'edit_note';
    return n === this.nextNumber() ? 'radio_button_checked' : 'radio_button_unchecked';
  }

  protected select(n: number): void {
    if (this.dirty() && !confirm('저장하지 않은 수정이 있습니다. 이동할까요?')) return;
    this.selected.set(n);
    this.resetEdit();
  }

  protected generate(): void {
    const id = this.ctx.projectId();
    const n = this.selected();
    if (id == null) return;
    if (this.current() && !confirm('현재 초안을 버리고 다시 생성할까요?')) return;

    this.streaming.set(true);
    this.streamText.set('');
    this.streamSub = this.stream.generate(id, n).subscribe({
      next: (ev) => {
        if (ev.type === 'token') {
          this.streamText.update((t) => t + ev.text);
        } else if (ev.type === 'done') {
          this.chapters.update((l) => [...l.filter((c) => c.number !== ev.chapter.number), ev.chapter]);
          this.resetEdit();
        } else {
          this.toast(ev.message);
        }
      },
      complete: () => this.endStream(),
      error: () => this.endStream(),
    });
    // 상태가 CHAPTER_GENERATING으로 바뀐 것을 반영
    setTimeout(() => this.ctx.refresh(), 300);
  }

  protected stop(): void {
    this.streamSub?.unsubscribe();
    this.endStream();
  }

  protected saveEdit(): void {
    const c = this.current();
    if (!c) return;
    this.busy.set(true);
    this.api.updateChapter(c.id, { title: this.editTitle, content: this.editContent }).subscribe({
      next: (updated) => {
        this.busy.set(false);
        this.replace(updated);
        this.snack.open('저장했습니다.', undefined, { duration: 2000 });
      },
      error: (e) => {
        this.busy.set(false);
        this.toast(e);
      },
    });
  }

  /** 사용자 승인. Continuity 경고가 있으면 한 번 더 확인합니다 (검증 결과를 무시하고 통과시키지 않음). */
  protected approve(): void {
    const c = this.current();
    if (!c) return;
    const high = c.continuityReport?.issues.filter((i) => i.severity === 'high').length ?? 0;
    if (high && !confirm(`심각한 연속성 이슈가 ${high}건 있습니다. 그래도 승인할까요?`)) return;
    this.busy.set(true);
    this.api.confirmChapter(c.id).subscribe({
      next: (confirmed) => {
        this.busy.set(false);
        this.replace(confirmed);
        this.ctx.refresh();
        if (confirmed.number < this.outline().length) this.select(confirmed.number + 1);
        this.snack.open(`${confirmed.number}장을 승인했습니다.`, undefined, { duration: 2500 });
      },
      error: (e) => {
        this.busy.set(false);
        this.toast(e);
      },
    });
  }

  private endStream(): void {
    this.streaming.set(false);
    this.streamSub = undefined;
    this.ctx.refresh();
  }

  private replace(c: Chapter): void {
    this.chapters.update((l) => l.map((x) => (x.id === c.id ? c : x)));
    this.resetEdit();
  }

  private resetEdit(): void {
    const c = this.current();
    this.editTitle = c?.title ?? '';
    this.editContent = c?.content ?? '';
    this.dirty.set(false);
  }

  private toast(e: unknown): void {
    this.snack.open(typeof e === 'string' ? e : errorMessage(e), '닫기', { duration: 4000 });
  }
}

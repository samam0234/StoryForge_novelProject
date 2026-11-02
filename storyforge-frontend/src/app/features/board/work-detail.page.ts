import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink } from '@angular/router';
import { CommunityApi, safeFilename, saveBlob } from '../../core/api/community-api.service';
import { errorMessage } from '../../core/api/storyforge-api.service';
import { AuthService } from '../../core/auth/auth.service';
import {
  CLONE_POLICY_OPTIONS,
  CloneNetwork,
  ITEM_ROLES,
  ItemRole,
  KIND_LABEL,
  PACKAGE_TYPES,
  ShareGraph,
  WorkChapter,
  WorkDetail,
  WorkGraph,
  labelOf,
} from '../../core/models/community';
import { EXPORT_FORMATS, ExportFormat } from '../../core/models/models';
import { CloneNetworkComponent } from '../../shared/components/clone-network/clone-network.component';
import { PackageMapComponent } from '../../shared/components/package-map/package-map.component';
import { RichTextEditorComponent } from '../../shared/components/rich-text-editor/rich-text-editor.component';
import { ShareGraphComponent } from '../../shared/components/share-graph/share-graph.component';
import { UserChipComponent } from '../../shared/components/user-chip/user-chip.component';
import { WorkCardComponent } from '../../shared/components/work-card/work-card.component';
import { WorkGraphComponent } from '../../shared/components/work-graph/work-graph.component';
import { CloneDialog, CloneDialogData } from './clone.dialog';
import { CommentThreadComponent } from './comment-thread.component';
import { ShareDialog, ShareDialogData } from './share.dialog';

const SHARE_TAB = 3;

/** 작품 상세: 읽기, 세계관, 작업 그래프, 공유 그래프, Clone 네트워크, 변경 이력, 댓글. 패키지는 구성과 연결 지도를 보여 줍니다. */
@Component({
  selector: 'sf-work-detail-page',
  imports: [
    DatePipe,
    DecimalPipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatProgressBarModule,
    MatTabsModule,
    MatTooltipModule,
    CloneNetworkComponent,
    CommentThreadComponent,
    PackageMapComponent,
    RichTextEditorComponent,
    ShareGraphComponent,
    UserChipComponent,
    WorkCardComponent,
    WorkGraphComponent,
  ],
  template: `
    @if (error()) {
      <div class="sf-page"><div class="sf-empty" role="alert">{{ error() }}</div></div>
    } @else if (!detail()) {
      <mat-progress-bar mode="indeterminate" />
    } @else if (detail(); as d) {
      <div class="sf-page">
        <header class="head">
          <div class="sf-row meta">
            <span class="sf-badge">{{ kindLabel() }}</span>
            @if (d.card.genre) {
              <span class="sf-muted">{{ d.card.genre }}</span>
            }
            @if (d.card.visibility !== 'PUBLIC') {
              <span class="sf-muted"><mat-icon inline>{{ d.card.visibility === 'PRIVATE' ? 'lock' : 'link' }}</mat-icon> {{ d.card.visibility === 'PRIVATE' ? '비공개' : '링크 공개' }}</span>
            }
            <span class="sf-muted">r{{ d.card.revision }} · {{ d.card.updatedAt | date: 'yyyy-MM-dd' }} 업데이트</span>
          </div>
          <h1>{{ d.card.title }}</h1>
          <div class="sf-row">
            <sf-user-chip [user]="d.card.author" />
            @if (d.card.parentTitle) {
              <span class="sf-muted lineage">
                <mat-icon inline>call_split</mat-icon>
                원작:
                @if (d.card.parentWorkId) {
                  <a [routerLink]="['/works', d.card.parentWorkId]">{{ d.card.parentTitle }}</a>
                } @else {
                  {{ d.card.parentTitle }} (삭제됨)
                }
              </span>
            }
          </div>
          @if (d.card.synopsis) {
            <p class="synopsis">{{ d.card.synopsis }}</p>
          }
          @if (d.card.tags.length) {
            <div class="tags">
              @for (t of d.card.tags; track t) {
                <span>#{{ t }}</span>
              }
            </div>
          }
          <div class="sf-row stats">
            <span>조회 {{ d.card.viewCount | number }}</span>
            <span>추천 {{ d.card.likeCount | number }}</span>
            <span>댓글 {{ d.card.commentCount | number }}</span>
            <span>Clone {{ d.card.cloneCount | number }}</span>
            <span>공유 {{ d.card.shareCount | number }}</span>
          </div>
          <div class="sf-row actions">
            @if (!d.owned) {
              <button mat-flat-button (click)="toggleLike()" [attr.aria-pressed]="d.liked">
                <mat-icon>{{ d.liked ? 'favorite' : 'favorite_border' }}</mat-icon> 추천 {{ d.card.likeCount | number }}
              </button>
            }
            @if (d.card.kind !== 'PACKAGE') {
              <span [matTooltip]="d.cloneModes.length ? '' : cloneBlockedReason()">
                <button mat-stroked-button [disabled]="!d.cloneModes.length" (click)="openClone()"><mat-icon>call_split</mat-icon> Clone</button>
              </span>
            }
            @if (d.card.visibility !== 'PRIVATE') {
              <button mat-stroked-button (click)="openShare()"><mat-icon>share</mat-icon> 공유</button>
            }
            <span [matTooltip]="canDownload() ? '' : '작가가 내려받기를 허용하지 않았습니다'">
              <button mat-stroked-button [matMenuTriggerFor]="exportMenu" [disabled]="!canDownload() || exporting()">
                <mat-icon>download</mat-icon> 내보내기
              </button>
            </span>
            <mat-menu #exportMenu="matMenu">
              @for (f of formats; track f.value) {
                <button mat-menu-item (click)="export(f.value)">{{ f.label }}</button>
              }
            </mat-menu>
            @if (d.owned) {
              <span class="sf-spacer"></span>
              @if (d.card.kind === 'PACKAGE') {
                <a mat-stroked-button [routerLink]="['/package-forge', d.card.id]"><mat-icon>hub</mat-icon> 패키지 편집</a>
              } @else {
                <a mat-stroked-button [routerLink]="['/me/works', d.card.id, 'edit']"><mat-icon>edit</mat-icon> 작품 수정</a>
              }
              @if (d.projectId) {
                <a mat-button [routerLink]="['/projects', d.projectId]">프로젝트 열기</a>
              }
            }
          </div>
          @if (d.inPackages.length) {
            <p class="sf-muted in-packages">
              이 작품이 들어 있는 패키지:
              @for (p of d.inPackages; track p.id) {
                <a [routerLink]="['/works', p.id]">{{ p.title }}</a>{{ $last ? '' : ', ' }}
              }
            </p>
          }
        </header>

        <mat-tab-group [selectedIndex]="tabIndex()" (selectedIndexChange)="tabIndex.set($event)" [preserveContent]="true">
          @if (d.packageView; as pkg) {
            <mat-tab label="구성 작품">
              <div class="tab">
                @if (pkg.worldNote) {
                  <section class="note">
                    <h3>공용 세계관 노트</h3>
                    <p class="pre">{{ pkg.worldNote }}</p>
                  </section>
                }
                <h3>독서 순서</h3>
                <ol class="members">
                  @for (i of pkg.items; track i.id) {
                    <li>
                      <div class="sf-row">
                        <span class="sf-badge">{{ roleLabel(i.role) }}</span>
                        @if (i.status === 'PENDING') {
                          <span class="sf-muted"><mat-icon inline>hourglass_top</mat-icon> 작가 승인 대기 (독자에게 보이지 않음)</span>
                        }
                      </div>
                      <sf-work-card [work]="i.work" />
                      @if (i.note) {
                        <p class="sf-muted item-note">{{ i.note }}</p>
                      }
                    </li>
                  }
                </ol>
              </div>
            </mat-tab>
            <mat-tab label="연결 지도">
              <div class="tab"><sf-package-map [items]="pkg.items" [links]="pkg.links" /></div>
            </mat-tab>
            <mat-tab label="통합 인물 사전">
              <div class="tab">
                @if (!pkg.characters.length) {
                  <div class="sf-empty">공개된 인물이 없습니다. 작가가 Clone을 막은 작품의 설정집은 공개되지 않습니다.</div>
                }
                <ul class="characters">
                  @for (c of pkg.characters; track c.name) {
                    <li>
                      <div class="sf-row">
                        <strong>{{ c.name }}</strong>
                        @if (c.crossover) {
                          <span class="sf-badge">교차 등장</span>
                        }
                      </div>
                      @for (a of c.appearances; track a.workId) {
                        <p class="sf-muted appearance">
                          <a [routerLink]="['/works', a.workId]">{{ a.workTitle }}</a>{{ a.description ? ': ' + a.description : '' }}
                        </p>
                      }
                    </li>
                  }
                </ul>
              </div>
            </mat-tab>
          } @else {
            <mat-tab label="읽기">
              <div class="tab reader">
                <nav class="toc" aria-label="챕터 목록">
                  <ol>
                    @for (c of d.chapters; track c.number) {
                      <li>
                        <button type="button" [class.on]="chapter()?.number === c.number" (click)="openChapter(c.number)">
                          {{ c.number }}. {{ c.title || '제목 없음' }}
                        </button>
                      </li>
                    }
                  </ol>
                </nav>
                <article class="page">
                  @if (chapter(); as c) {
                    <h2>{{ c.number }}장. {{ c.title }}</h2>
                    <sf-rich-text-editor [value]="c.content ?? ''" [editable]="false" />
                    <div class="sf-row">
                      @if (c.previous) {
                        <button mat-button (click)="openChapter(c.previous)"><mat-icon>chevron_left</mat-icon> 이전 장</button>
                      }
                      <span class="sf-spacer"></span>
                      @if (c.next) {
                        <button mat-button (click)="openChapter(c.next)">다음 장 <mat-icon iconPositionEnd>chevron_right</mat-icon></button>
                      }
                    </div>
                  }
                </article>
              </div>
            </mat-tab>
            <mat-tab label="세계관">
              <div class="tab">
                @if (d.bible; as b) {
                  <section class="note">
                    <h3>세계관</h3>
                    <p class="pre">{{ b.worldSetting }}</p>
                    <h3>테마</h3>
                    <p class="pre">{{ b.theme }}</p>
                    <h3>톤</h3>
                    <p class="pre">{{ b.tone }}</p>
                  </section>
                  <h3>인물</h3>
                  <ul class="characters">
                    @for (c of b.characters; track c.name) {
                      <li>
                        <strong>{{ c.name }}</strong>
                        <p class="sf-muted appearance">{{ c.description }}</p>
                        <p class="sf-muted appearance">{{ c.personality }}</p>
                      </li>
                    }
                  </ul>
                  @if (d.outline; as outline) {
                    <h3>Outline</h3>
                    <ol class="outline">
                      @for (o of outline; track o.order) {
                        <li>
                          <strong>{{ o.title }}</strong>
                          @if (o.goals) {
                            <span class="sf-muted"> — {{ o.goals }}</span>
                          }
                        </li>
                      }
                    </ol>
                  }
                } @else {
                  <div class="sf-empty">작가가 설정집을 공개하지 않았습니다.</div>
                }
              </div>
            </mat-tab>
          }
          @if (d.card.kind !== 'PACKAGE') {
            <mat-tab label="작업 그래프">
              <div class="tab">
                <p class="sf-muted hint">수동과 에이전트 작업을 날짜별로 나눠 보여 줍니다. 막대를 누르면 그날 한 작업이 표에 나옵니다. 본문은 공개되지 않습니다.</p>
                @if (graph(); as g) {
                  <sf-work-graph [graph]="g" />
                }
              </div>
            </mat-tab>
          }
          <mat-tab label="공유 그래프">
            <div class="tab">
              @if (shares(); as s) {
                <sf-share-graph [graph]="s" />
              }
            </div>
          </mat-tab>
          @if (d.card.kind !== 'PACKAGE') {
            <mat-tab label="Clone 네트워크">
              <div class="tab">
                @if (network(); as n) {
                  <sf-clone-network [network]="n" />
                }
              </div>
            </mat-tab>
          }
          <mat-tab label="변경 이력">
            <div class="tab">
              @if (!d.revisions.length) {
                <div class="sf-empty">변경 이력이 없습니다.</div>
              } @else {
                <table class="sf-table">
                  <thead>
                    <tr>
                      <th scope="col">리비전</th>
                      <th scope="col">일시</th>
                      <th scope="col">내용</th>
                      <th scope="col">분량</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (r of d.revisions; track r.revision) {
                      <tr>
                        <td>r{{ r.revision }}</td>
                        <td>{{ r.createdAt | date: 'yyyy-MM-dd HH:mm' }}</td>
                        <td>{{ r.message || '-' }}</td>
                        <td>{{ r.chapterCount }}장 · {{ r.charCount | number }}자</td>
                      </tr>
                    }
                  </tbody>
                </table>
              }
            </div>
          </mat-tab>
          <mat-tab [label]="'댓글 ' + d.card.commentCount">
            <div class="tab"><sf-comment-thread [workId]="d.card.id" (changed)="commentCount($event)" /></div>
          </mat-tab>
        </mat-tab-group>
      </div>
    }
  `,
  styles: `
    .head {
      display: flex;
      flex-direction: column;
      gap: 8px;
      margin-bottom: 16px;
    }
    h1 {
      margin: 0;
      font-size: 26px;
      overflow-wrap: anywhere;
    }
    .meta,
    .stats {
      font-size: 13px;
    }
    .stats {
      gap: 12px;
      color: var(--sf-text-muted);
    }
    .synopsis {
      margin: 0;
      white-space: pre-wrap;
    }
    .tags {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      font-size: 13px;
      color: var(--sf-text-muted);
    }
    .lineage,
    .in-packages {
      font-size: 13px;
      margin: 0;
    }
    .tab {
      padding: 16px 0;
    }
    .hint {
      font-size: 13px;
      margin: 0 0 12px;
    }
    .reader {
      display: grid;
      grid-template-columns: 220px minmax(0, 1fr);
      gap: 16px;
    }
    @media (max-width: 800px) {
      .reader {
        grid-template-columns: 1fr;
      }
    }
    .toc ol {
      list-style: none;
      margin: 0;
      padding: 0;
    }
    .toc button {
      width: 100%;
      text-align: left;
      padding: 8px;
      border: 0;
      border-radius: 6px;
      background: none;
      color: var(--sf-text);
      font: inherit;
      font-size: 14px;
      cursor: pointer;
    }
    .toc button:hover {
      background: var(--sf-surface-2);
    }
    .toc button.on {
      background: color-mix(in srgb, var(--sf-secondary) 18%, transparent);
      font-weight: 600;
    }
    .page {
      display: flex;
      flex-direction: column;
      gap: 12px;
      min-width: 0;
    }
    .page h2 {
      margin: 0;
      font-size: 20px;
    }
    .page sf-rich-text-editor {
      min-height: 320px;
      flex: none;
    }
    .note h3,
    h3 {
      font-size: 15px;
      margin: 16px 0 6px;
    }
    .pre {
      white-space: pre-wrap;
      margin: 0;
      overflow-wrap: anywhere;
    }
    .members {
      list-style: none;
      counter-reset: item;
      margin: 0;
      padding: 0;
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 12px;
    }
    .members > li {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }
    .item-note {
      margin: 0;
      font-size: 13px;
    }
    .characters {
      list-style: none;
      margin: 0;
      padding: 0;
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
      gap: 8px;
    }
    .characters > li {
      padding: 10px;
      border: 1px solid var(--sf-border);
      border-radius: 8px;
    }
    .appearance {
      margin: 4px 0 0;
      font-size: 13px;
    }
  `,
})
export class WorkDetailPage {
  private readonly api = inject(CommunityApi);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  readonly workId = input.required<string>();
  /** 공유 링크의 추적 토큰 */
  readonly s = input<string>();

  protected readonly formats = EXPORT_FORMATS;
  protected readonly detail = signal<WorkDetail | null>(null);
  protected readonly chapter = signal<WorkChapter | null>(null);
  protected readonly graph = signal<WorkGraph | null>(null);
  protected readonly shares = signal<ShareGraph | null>(null);
  protected readonly network = signal<CloneNetwork | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly exporting = signal(false);
  protected readonly tabIndex = signal(0);

  protected readonly canDownload = computed(() => {
    const d = this.detail();
    return !!d && (d.downloadable || d.owned);
  });

  protected readonly kindLabel = computed(() => {
    const c = this.detail()?.card;
    if (!c) return '';
    return c.kind === 'PACKAGE' ? `패키지포지 · ${labelOf(PACKAGE_TYPES, c.packageType)}` : KIND_LABEL[c.kind];
  });

  constructor() {
    effect(() => {
      const id = Number(this.workId());
      this.detail.set(null);
      this.chapter.set(null);
      this.error.set(null);
      this.tabIndex.set(0);
      this.load(id);
      this.countShareVisit(this.s());
    });
  }

  private load(id: number): void {
    this.api.work(id).subscribe({
      next: (d) => {
        this.detail.set(d);
        if (d.chapters.length) this.openChapter(d.chapters[0].number);
        if (d.card.kind !== 'PACKAGE') {
          this.api.workGraph(id).subscribe((g) => this.graph.set(g));
          this.api.network(id).subscribe((n) => this.network.set(n));
        }
        this.loadShares(id);
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  private loadShares(id: number): void {
    this.api.shares(id).subscribe((s) => this.shares.set(s));
  }

  /** 같은 브라우저 세션에서는 한 번만 셉니다. */
  private countShareVisit(token: string | undefined): void {
    if (!token) return;
    const key = `storyforge.share.${token}`;
    try {
      if (sessionStorage.getItem(key)) return;
      sessionStorage.setItem(key, '1');
    } catch {
      /* 저장소를 쓸 수 없으면 그대로 셉니다 */
    }
    this.api.visitShare(token).subscribe({ error: () => undefined });
  }

  protected openChapter(number: number): void {
    const d = this.detail();
    if (!d) return;
    this.api.workChapter(d.card.id, number).subscribe({
      next: (c) => this.chapter.set(c),
      error: (e) => this.snack.open(errorMessage(e), '닫기', { duration: 4000 }),
    });
  }

  protected roleLabel(role: ItemRole): string {
    return labelOf(ITEM_ROLES, role);
  }

  protected cloneBlockedReason(): string {
    const policy = this.detail()?.card.clonePolicy;
    return policy === 'CLOSED' ? '작가가 Clone을 허용하지 않았습니다' : CLONE_POLICY_OPTIONS.find((o) => o.value === policy)?.hint ?? '';
  }

  private requireLogin(): boolean {
    if (this.auth.isLoggedIn()) return true;
    this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
    return false;
  }

  protected toggleLike(): void {
    const d = this.detail();
    if (!d || !this.requireLogin()) return;
    this.api.like(d.card.id, !d.liked).subscribe({
      next: (r) => this.detail.set({ ...d, liked: r.liked, card: { ...d.card, likeCount: r.likeCount } }),
      error: (e) => this.snack.open(errorMessage(e), '닫기', { duration: 4000 }),
    });
  }

  protected commentCount(count: number): void {
    const d = this.detail();
    if (d) this.detail.set({ ...d, card: { ...d.card, commentCount: count } });
  }

  protected openClone(): void {
    const d = this.detail();
    if (!d || !this.requireLogin()) return;
    const data: CloneDialogData = { title: d.card.title, revision: d.card.revision, allowed: d.cloneModes };
    this.dialog
      .open(CloneDialog, { data, width: '680px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe((body) => {
        if (!body) return;
        this.api.cloneWork(d.card.id, body).subscribe({
          next: (r) => this.router.navigate(['/projects', r.projectId]),
          error: (e) => this.snack.open(errorMessage(e), '닫기', { duration: 4000 }),
        });
      });
  }

  protected openShare(): void {
    const d = this.detail();
    if (!d || !this.requireLogin()) return;
    const data: ShareDialogData = { workId: d.card.id, title: d.card.title };
    this.dialog
      .open(ShareDialog, { data, maxWidth: '95vw' })
      .afterClosed()
      .subscribe((created) => {
        if (!created) return;
        this.loadShares(d.card.id);
        this.detail.update((cur) => (cur ? { ...cur, card: { ...cur.card, shareCount: cur.card.shareCount + 1 } } : cur));
        // 공유하면 공유 그래프를 바로 보여 줍니다. 일반 작품과 패키지 모두 네 번째 탭입니다.
        this.tabIndex.set(SHARE_TAB);
      });
  }

  protected export(format: ExportFormat): void {
    const d = this.detail();
    if (!d || !this.requireLogin()) return;
    this.exporting.set(true);
    this.api.exportWork(d.card.id, format).subscribe({
      next: (blob) => {
        this.exporting.set(false);
        saveBlob(blob, safeFilename(d.card.title, format));
      },
      error: (e: HttpErrorResponse) => {
        this.exporting.set(false);
        // 응답 형식이 Blob이라 ProblemDetail을 읽지 않고 상태 코드로 안내합니다.
        const message = e.status === 409 ? '내보낼 수 없는 작품입니다.' : '내보내기에 실패했습니다.';
        this.snack.open(message, '닫기', { duration: 4000 });
      },
    });
  }
}

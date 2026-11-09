import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { StoryforgeApi, errorMessage } from '../../core/api/storyforge-api.service';
import { CLONE_MODES, CloneMode, labelOf } from '../../core/models/community';
import { PIPELINE_STAGES, Project, stageIndex, stageLabel } from '../../core/models/models';
import { ProjectCreateDialog } from '../projects/project-create.dialog';

type Filter = 'ALL' | 'ACTIVE' | 'COMPLETED' | 'PUBLISHED' | 'CLONED';
type Sort = 'recent' | 'title' | 'progress';

/**
 * 내 프로젝트 (기획서 §9.6 1번). 게시판이 "읽을 작품"을 찾는 곳이라면 여기는 "쓰고 있는 작업실"입니다.
 * 다음에 할 일, 파이프라인 진행, 수동·에이전트 비율, 게시 여부, Clone 출처를 보여 줍니다.
 */
@Component({
  selector: 'sf-dashboard-page',
  imports: [
    DatePipe,
    DecimalPipe,
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatChipsModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatSelectModule,
  ],
  template: `
    <div class="sf-page">
      <header class="sf-row head">
        <div>
          <h1>내 프로젝트</h1>
          <p class="sf-muted">쓰고 있는 원고와 다음 할 일을 모아 봅니다. 프로젝트는 게시 전까지 나만 볼 수 있습니다.</p>
        </div>
        <span class="sf-spacer"></span>
        <button mat-flat-button class="cta" (click)="openCreate()">
          <mat-icon>add</mat-icon>
          새 프로젝트
        </button>
      </header>

      @if (loading()) {
        <mat-progress-bar mode="indeterminate" />
      } @else if (error()) {
        <div class="sf-empty">{{ error() }}</div>
      } @else if (projects().length === 0) {
        <div class="sf-empty">
          <p>아직 프로젝트가 없습니다.</p>
          <button mat-stroked-button (click)="openCreate()">첫 프로젝트 만들기</button>
          <a mat-button routerLink="/board/long">다른 작품 Clone하러 가기</a>
        </div>
      } @else {
        <section class="summary" aria-label="작업 요약">
          <div><strong>{{ counts().active }}</strong><span>진행 중</span></div>
          <div><strong>{{ counts().completed }}</strong><span>완료</span></div>
          <div><strong>{{ counts().published }}</strong><span>게시됨</span></div>
          <div><strong>{{ counts().cloned }}</strong><span>Clone</span></div>
          <div class="share">
            <span>전체 변경 글자</span>
            <div class="bar" role="img" [attr.aria-label]="'수동 ' + manualShare() + '%, 에이전트 ' + (100 - manualShare()) + '%'">
              @if (totals().manual) {
                <i class="manual" [style.flex-grow]="totals().manual"></i>
              }
              @if (totals().agent) {
                <i class="agent" [style.flex-grow]="totals().agent"></i>
              }
            </div>
            <small class="sf-muted">수동 {{ totals().manual | number }}자 · 에이전트 {{ totals().agent | number }}자</small>
          </div>
        </section>

        <div class="sf-row filters">
          <mat-chip-listbox aria-label="상태 필터" [value]="filter()" (change)="filter.set($event.value ?? 'ALL')">
            @for (f of filters; track f.value) {
              <mat-chip-option [value]="f.value">{{ f.label }}</mat-chip-option>
            }
          </mat-chip-listbox>
          <span class="sf-spacer"></span>
          <mat-form-field appearance="outline" subscriptSizing="dynamic" class="search">
            <mat-label>제목 검색</mat-label>
            <input matInput [ngModel]="query()" (ngModelChange)="query.set($event)" />
          </mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic" class="sort">
            <mat-label>정렬</mat-label>
            <mat-select [ngModel]="sort()" (ngModelChange)="sort.set($event)">
              <mat-option value="recent">최근 수정</mat-option>
              <mat-option value="title">제목</mat-option>
              <mat-option value="progress">진행률</mat-option>
            </mat-select>
          </mat-form-field>
        </div>

        @if (!visible().length) {
          <div class="sf-empty">조건에 맞는 프로젝트가 없습니다.</div>
        }
        <ul class="list">
          @for (p of visible(); track p.id) {
            <li class="row">
              <div class="main">
                <div class="sf-row badges">
                  <span class="sf-badge">{{ p.form === 'SHORT' ? '단편' : '장편' }}</span>
                  <span class="stage">{{ label(p) }}</span>
                  @if (p.workId) {
                    <a class="sf-badge published" [routerLink]="['/works', p.workId]"><mat-icon inline>public</mat-icon> 게시됨</a>
                  }
                  @if (p.role === 'COLLABORATOR') {
                    <span class="sf-badge">함께 쓰는 중</span>
                  }
                  @if (p.clonedFromTitle) {
                    <span class="sf-muted origin"><mat-icon inline>call_split</mat-icon> {{ p.clonedFromTitle }} ({{ modeLabel(p.cloneMode) }})</span>
                  }
                </div>
                <h3><a [routerLink]="['/projects', p.id]">{{ p.title }}</a></h3>
                @if (p.coverUrl) {
                  <img class="cover" [src]="p.coverUrl" alt="" />
                }
                <p class="sf-muted sub">{{ p.genre || '장르 미정' }} · 최근 수정 {{ p.updatedAt | date: 'yyyy-MM-dd HH:mm' }}</p>
                <div class="progress">
                  <mat-progress-bar mode="determinate" [value]="progress(p)" />
                  @if (p.totalChapters > 0) {
                    <span class="sf-muted">{{ p.confirmedChapters }} / {{ p.totalChapters }}장</span>
                  }
                </div>
              </div>
              <div class="side">
                <div class="mini" role="img" [attr.aria-label]="ratioLabel(p)" [title]="ratioLabel(p)">
                  @if (p.manualChars) {
                    <i class="manual" [style.flex-grow]="p.manualChars"></i>
                  }
                  @if (p.agentChars) {
                    <i class="agent" [style.flex-grow]="p.agentChars"></i>
                  }
                </div>
                <a mat-stroked-button [routerLink]="next(p).link">{{ next(p).label }} <mat-icon iconPositionEnd>arrow_forward</mat-icon></a>
              </div>
            </li>
          }
        </ul>
      }
    </div>
  `,
  styles: `
    h1 {
      margin: 0 0 4px;
      font-size: 24px;
    }
    .head {
      margin-bottom: 16px;
    }
    .cta {
      --mat-button-filled-container-color: var(--sf-accent);
      --mat-button-filled-label-text-color: #2b1d00;
    }
    .summary {
      display: grid;
      grid-template-columns: repeat(4, minmax(70px, 110px)) minmax(200px, 1fr);
      gap: 8px;
      margin-bottom: 16px;
    }
    @media (max-width: 800px) {
      .summary {
        grid-template-columns: repeat(2, 1fr);
      }
      .share {
        grid-column: 1 / -1;
      }
    }
    .summary > div {
      display: flex;
      flex-direction: column;
      gap: 2px;
      padding: 10px 12px;
      border: 1px solid var(--sf-border);
      border-radius: 8px;
      background: var(--sf-surface);
      font-size: 12px;
    }
    .summary strong {
      font-size: 20px;
    }
    .bar,
    .mini {
      display: flex;
      gap: 2px;
      height: 8px;
      border-radius: 4px;
      overflow: hidden;
      background: var(--sf-surface-2);
    }
    .bar i,
    .mini i {
      display: block;
      flex-basis: 0;
      min-width: 2px;
    }
    .manual {
      background: var(--sf-series-manual);
    }
    .agent {
      background: var(--sf-series-agent);
    }
    .filters {
      margin-bottom: 12px;
    }
    .search {
      width: 180px;
    }
    .sort {
      width: 130px;
    }
    .list {
      list-style: none;
      margin: 0;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .row {
      display: flex;
      gap: 16px;
      align-items: center;
      flex-wrap: wrap;
      padding: 14px 16px;
      border: 1px solid var(--sf-border);
      border-radius: var(--sf-radius);
      background: var(--sf-surface);
    }
    .cover {
      width: 96px;
      height: 64px;
      object-fit: cover;
      border-radius: 6px;
    }
    .row:hover {
      border-color: var(--sf-secondary);
    }
    .main {
      flex: 1 1 320px;
      min-width: 0;
    }
    .badges {
      font-size: 12px;
    }
    .stage {
      font-weight: 600;
      color: var(--sf-emphasis);
    }
    .published {
      text-decoration: none;
    }
    h3 {
      margin: 6px 0 2px;
      font-size: 16px;
      overflow-wrap: anywhere;
    }
    h3 a {
      text-decoration: none;
    }
    h3 a:hover {
      text-decoration: underline;
    }
    .sub {
      margin: 0 0 8px;
      font-size: 12px;
    }
    .progress {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 12px;
      white-space: nowrap;
    }
    .side {
      display: flex;
      flex-direction: column;
      gap: 8px;
      flex: 0 0 200px;
    }
  `,
})
export class DashboardPage {
  private readonly api = inject(StoryforgeApi);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);

  protected readonly filters: { value: Filter; label: string }[] = [
    { value: 'ALL', label: '전체' },
    { value: 'ACTIVE', label: '진행 중' },
    { value: 'COMPLETED', label: '완료' },
    { value: 'PUBLISHED', label: '게시됨' },
    { value: 'CLONED', label: 'Clone' },
  ];

  protected readonly projects = signal<Project[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly filter = signal<Filter>('ALL');
  protected readonly query = signal('');
  protected readonly sort = signal<Sort>('recent');

  protected readonly counts = computed(() => {
    const list = this.projects();
    return {
      active: list.filter((p) => p.status !== 'COMPLETED').length,
      completed: list.filter((p) => p.status === 'COMPLETED').length,
      published: list.filter((p) => p.workId).length,
      cloned: list.filter((p) => p.clonedFromTitle).length,
    };
  });

  protected readonly totals = computed(() => ({
    manual: this.projects().reduce((sum, p) => sum + p.manualChars, 0),
    agent: this.projects().reduce((sum, p) => sum + p.agentChars, 0),
  }));

  protected readonly manualShare = computed(() => {
    const { manual, agent } = this.totals();
    return manual + agent ? Math.round((manual / (manual + agent)) * 100) : 0;
  });

  protected readonly visible = computed(() => {
    const q = this.query().trim().toLowerCase();
    const filtered = this.projects().filter((p) => {
      if (q && !p.title.toLowerCase().includes(q)) return false;
      switch (this.filter()) {
        case 'ACTIVE':
          return p.status !== 'COMPLETED';
        case 'COMPLETED':
          return p.status === 'COMPLETED';
        case 'PUBLISHED':
          return !!p.workId;
        case 'CLONED':
          return !!p.clonedFromTitle;
        default:
          return true;
      }
    });
    switch (this.sort()) {
      case 'title':
        return [...filtered].sort((a, b) => a.title.localeCompare(b.title, 'ko'));
      case 'progress':
        return [...filtered].sort((a, b) => this.progress(b) - this.progress(a));
      default:
        return filtered;
    }
  });

  constructor() {
    this.load();
  }

  protected label(p: Project): string {
    return stageLabel(p.status);
  }

  protected modeLabel(mode: string | null): string {
    return labelOf(CLONE_MODES, mode as CloneMode | null);
  }

  protected ratioLabel(p: Project): string {
    const total = p.manualChars + p.agentChars;
    if (!total) return '아직 변경 글자가 없습니다';
    const manual = Math.round((p.manualChars / total) * 100);
    return `수동 ${manual}% · 에이전트 ${100 - manual}%`;
  }

  /** 지금 이어서 할 일. 판단은 백엔드가 준 상태만 보고 합니다. */
  protected next(p: Project): { label: string; link: unknown[] } {
    const base = ['/projects', p.id];
    switch (p.status) {
      case 'BIBLE_DRAFT':
        return { label: 'Bible 작성', link: [...base, 'bible'] };
      case 'BIBLE_CONFIRMED':
      case 'OUTLINE_DRAFT':
        return { label: 'Outline 작성', link: [...base, 'outline'] };
      case 'OUTLINE_CONFIRMED':
        return { label: '첫 챕터 생성', link: [...base, 'chapters'] };
      case 'CHAPTER_GENERATING':
        return { label: '생성 확인', link: [...base, 'chapters'] };
      case 'CHAPTER_REVIEW':
        return { label: `${p.confirmedChapters + 1}장 검토`, link: [...base, 'chapters'] };
      default:
        return p.workId ? { label: '작품 보기', link: ['/works', p.workId] } : { label: '게시하기', link: [...base, 'publish'] };
    }
  }

  /** 챕터 단계 전에는 파이프라인 단계, 이후에는 확정 챕터 비율로 진행률을 보여줍니다. */
  protected progress(p: Project): number {
    if (p.status === 'COMPLETED') return 100;
    if (p.totalChapters > 0 && stageIndex(p.status) >= stageIndex('OUTLINE_CONFIRMED')) {
      return 40 + (60 * p.confirmedChapters) / p.totalChapters;
    }
    return (40 * stageIndex(p.status)) / (PIPELINE_STAGES.length - 3);
  }

  protected openCreate(): void {
    this.dialog
      .open(ProjectCreateDialog, { width: '480px' })
      .afterClosed()
      .subscribe((body) => {
        if (!body) return;
        this.api.createProject(body).subscribe({
          next: (p) => this.router.navigate(['/projects', p.id]),
          error: (e) => this.snack.open(errorMessage(e), '닫기', { duration: 4000 }),
        });
      });
  }

  private load(): void {
    this.api.listProjects().subscribe({
      next: (list) => {
        this.projects.set(list);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(errorMessage(e));
        this.loading.set(false);
      },
    });
  }
}

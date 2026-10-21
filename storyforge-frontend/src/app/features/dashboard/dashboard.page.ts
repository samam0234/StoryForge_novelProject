import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { StoryforgeApi, errorMessage } from '../../core/api/storyforge-api.service';
import { PIPELINE_STAGES, Project, stageIndex, stageLabel } from '../../core/models/models';
import { ProjectCreateDialog } from '../projects/project-create.dialog';

/** 프로젝트 카드 그리드 + 신규 프로젝트 CTA (기획서 §9.6 1번) */
@Component({
  selector: 'sf-dashboard-page',
  imports: [RouterLink, DatePipe, MatCardModule, MatButtonModule, MatIconModule, MatProgressBarModule],
  template: `
    <div class="sf-page">
      <header class="sf-row head">
        <div>
          <h1>내 프로젝트</h1>
          <p class="sf-muted">Story Bible → Outline → 챕터 순서로 장편을 완성합니다.</p>
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
        </div>
      } @else {
        <div class="grid">
          @for (p of projects(); track p.id) {
            <mat-card appearance="outlined" class="card">
              <a [routerLink]="['/projects', p.id]" class="card-link">
                <mat-card-header>
                  <mat-card-title>{{ p.title }}</mat-card-title>
                  <mat-card-subtitle>{{ p.genre || '장르 미정' }}</mat-card-subtitle>
                </mat-card-header>
                <mat-card-content>
                  <div class="sf-row status">
                    <span class="badge">{{ label(p) }}</span>
                    @if (p.totalChapters > 0) {
                      <span class="sf-muted">{{ p.confirmedChapters }} / {{ p.totalChapters }}장</span>
                    }
                  </div>
                  <mat-progress-bar mode="determinate" [value]="progress(p)" />
                  <p class="sf-muted updated">최근 수정 {{ p.updatedAt | date: 'yyyy-MM-dd HH:mm' }}</p>
                </mat-card-content>
              </a>
            </mat-card>
          }
        </div>
      }
    </div>
  `,
  styles: `
    h1 {
      margin: 0 0 4px;
      font-size: 24px;
    }
    .head {
      margin-bottom: 24px;
    }
    .cta {
      --mat-button-filled-container-color: var(--sf-accent);
      --mat-button-filled-label-text-color: #2b1d00;
    }
    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 16px;
    }
    .card {
      background: var(--sf-surface);
      transition: border-color 0.15s;
    }
    .card:hover {
      border-color: var(--sf-secondary);
    }
    .card-link {
      text-decoration: none;
      display: block;
      padding-bottom: 8px;
    }
    .status {
      margin: 12px 0 8px;
      font-size: 13px;
    }
    .badge {
      padding: 2px 10px;
      border-radius: 999px;
      background: color-mix(in srgb, var(--sf-secondary) 20%, transparent);
      color: var(--sf-emphasis);
      font-weight: 600;
    }
    .updated {
      font-size: 12px;
      margin: 8px 0 0;
    }
  `,
})
export class DashboardPage {
  private readonly api = inject(StoryforgeApi);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);

  protected readonly projects = signal<Project[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.load();
  }

  protected label(p: Project): string {
    return stageLabel(p.status);
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

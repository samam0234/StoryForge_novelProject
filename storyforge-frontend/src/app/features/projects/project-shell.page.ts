import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, input, numberAttribute, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTabsModule } from '@angular/material/tabs';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { safeFilename, saveBlob } from '../../core/api/community-api.service';
import { StoryforgeApi } from '../../core/api/storyforge-api.service';
import { EXPORT_FORMATS, ExportFormat } from '../../core/models/models';
import { PipelineStepperComponent } from '../../shared/components/pipeline-stepper/pipeline-stepper.component';
import { CommitFocusBanner } from './commit-focus.banner';
import { ProjectContext } from './project-context';

/** 프로젝트 상단(제목, 파이프라인 단계) + Bible / Outline / 챕터 탭 */
@Component({
  selector: 'sf-project-shell-page',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatTabsModule, MatIconModule, MatButtonModule, MatMenuModule, MatTooltipModule, PipelineStepperComponent, CommitFocusBanner],
  providers: [ProjectContext],
  template: `
    @if (ctx.project(); as p) {
      <div class="header">
        <div class="sf-row">
          <a mat-icon-button routerLink="/" aria-label="대시보드로"><mat-icon>arrow_back</mat-icon></a>
          <div>
            <h1>{{ p.title }}</h1>
            <span class="sf-muted">{{ p.genre || '장르 미정' }}</span>
            @if (p.clonedFromTitle) {
              <span class="sf-muted origin"> · Clone 원본: {{ p.clonedFromTitle }}</span>
            }
            @if (p.role === 'COLLABORATOR') {
              <span class="sf-badge">함께 쓰는 중</span>
            }
          </div>
          <span class="sf-spacer"></span>
          <span [matTooltip]="canExport() ? '' : '승인된 챕터가 있어야 내보낼 수 있습니다'">
            <button mat-stroked-button [matMenuTriggerFor]="exportMenu" [disabled]="!canExport() || exporting()">
              <mat-icon>download</mat-icon> 내보내기
            </button>
          </span>
          <mat-menu #exportMenu="matMenu">
            @for (f of formats; track f.value) {
              <button mat-menu-item (click)="export(f.value)">{{ f.label }}</button>
            }
          </mat-menu>
        </div>
        @if (ctx.status(); as s) {
          <sf-pipeline-stepper [status]="s" />
        }
        <nav mat-tab-nav-bar [tabPanel]="panel">
          @for (tab of tabs(); track tab.path) {
            <a mat-tab-link [routerLink]="tab.path" routerLinkActive #rla="routerLinkActive" [active]="rla.isActive">
              {{ tab.label }}
            </a>
          }
      </nav>
      <sf-commit-focus [projectId]="projectId()" />
    </div>
      <mat-tab-nav-panel #panel>
        <router-outlet />
      </mat-tab-nav-panel>
    }
  `,
  styles: `
    :host {
      display: flex;
      flex-direction: column;
      height: 100%;
    }
    mat-tab-nav-panel {
      flex: 1;
      min-height: 0;
      overflow-y: auto;
    }
    .header {
      padding: 16px 24px 0;
      background: var(--sf-surface);
      border-bottom: 1px solid var(--sf-border);
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    h1 {
      margin: 0;
      font-size: 20px;
    }
  `,
})
export class ProjectShellPage {
  protected readonly ctx = inject(ProjectContext);
  private readonly api = inject(StoryforgeApi);
  private readonly snack = inject(MatSnackBar);
  readonly projectId = input.required({ transform: numberAttribute });

  protected readonly tabs = computed(() => [
    { path: 'bible', label: 'Story Bible' },
    { path: 'outline', label: 'Outline' },
    { path: 'chapters', label: '챕터' },
    { path: 'activity', label: '작업 기록' },
    { path: 'publish', label: '게시' },
    ...(this.ctx.project()?.clonedFromTitle ? [{ path: 'upstream', label: '원본 비교' }] : []),
    { path: 'settings', label: '설정' },
  ]);
  protected readonly formats = EXPORT_FORMATS;
  protected readonly exporting = signal(false);

  constructor() {
    effect(() => this.ctx.load(this.projectId()));
  }

  protected canExport(): boolean {
    return (this.ctx.statusInfo()?.confirmedChapters ?? 0) > 0;
  }

  protected export(format: ExportFormat): void {
    const p = this.ctx.project();
    if (!p) return;
    this.exporting.set(true);
    this.api.exportProject(p.id, format).subscribe({
      next: (blob) => {
        this.exporting.set(false);
        saveBlob(blob, safeFilename(p.title, format));
      },
      error: (e: HttpErrorResponse) => {
        this.exporting.set(false);
        // 응답 형식이 Blob이라 ProblemDetail을 읽지 않고 상태 코드로 안내합니다.
        const message = e.status === 409 ? '승인된 챕터가 있어야 내보낼 수 있습니다.' : '내보내기에 실패했습니다.';
        this.snack.open(message, '닫기', { duration: 4000 });
      },
    });
  }
}

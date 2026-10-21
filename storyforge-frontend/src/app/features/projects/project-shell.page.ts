import { Component, effect, inject, input, numberAttribute } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { PipelineStepperComponent } from '../../shared/components/pipeline-stepper/pipeline-stepper.component';
import { ProjectContext } from './project-context';

/** 프로젝트 상단(제목, 파이프라인 단계) + Bible / Outline / 챕터 탭 */
@Component({
  selector: 'sf-project-shell-page',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatTabsModule, MatIconModule, MatButtonModule, PipelineStepperComponent],
  providers: [ProjectContext],
  template: `
    @if (ctx.project(); as p) {
      <div class="header">
        <div class="sf-row">
          <a mat-icon-button routerLink="/" aria-label="대시보드로"><mat-icon>arrow_back</mat-icon></a>
          <div>
            <h1>{{ p.title }}</h1>
            <span class="sf-muted">{{ p.genre || '장르 미정' }}</span>
          </div>
        </div>
        @if (ctx.status(); as s) {
          <sf-pipeline-stepper [status]="s" />
        }
        <nav mat-tab-nav-bar [tabPanel]="panel">
          @for (tab of tabs; track tab.path) {
            <a mat-tab-link [routerLink]="tab.path" routerLinkActive #rla="routerLinkActive" [active]="rla.isActive">
              {{ tab.label }}
            </a>
          }
        </nav>
      </div>
      <mat-tab-nav-panel #panel>
        <router-outlet />
      </mat-tab-nav-panel>
    }
  `,
  styles: `
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
  readonly projectId = input.required({ transform: numberAttribute });

  protected readonly tabs = [
    { path: 'bible', label: 'Story Bible' },
    { path: 'outline', label: 'Outline' },
    { path: 'chapters', label: '챕터' },
  ];

  constructor() {
    effect(() => this.ctx.load(this.projectId()));
  }
}

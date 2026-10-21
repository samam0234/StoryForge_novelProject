import { Component, computed, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { PIPELINE_STAGES, ProjectStatus, stageIndex } from '../../../core/models/models';

/** 현재 파이프라인 단계를 항상 보여줍니다 (기획서 §9.1 단계적 진행 가시화). */
@Component({
  selector: 'sf-pipeline-stepper',
  imports: [MatIconModule],
  template: `
    <ol class="steps">
      @for (stage of stages; track stage.status; let i = $index) {
        <li [class.done]="i < current()" [class.current]="i === current()">
          <span class="dot">
            @if (i < current() || (i === current() && status() === 'COMPLETED')) {
              <mat-icon>check</mat-icon>
            } @else {
              {{ i + 1 }}
            }
          </span>
          <span class="label">{{ stage.label }}</span>
        </li>
      }
    </ol>
  `,
  styles: `
    .steps {
      display: flex;
      gap: 4px;
      list-style: none;
      margin: 0;
      padding: 0;
      overflow-x: auto;
    }
    li {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 6px 10px;
      border-radius: 999px;
      color: var(--sf-text-muted);
      font-size: 13px;
      white-space: nowrap;
    }
    .dot {
      display: inline-grid;
      place-items: center;
      width: 22px;
      height: 22px;
      border-radius: 50%;
      border: 1px solid var(--sf-border);
      font-size: 12px;
    }
    .dot mat-icon {
      font-size: 16px;
      width: 16px;
      height: 16px;
    }
    .done .dot {
      background: var(--sf-secondary);
      border-color: var(--sf-secondary);
      color: #fff;
    }
    .current {
      background: color-mix(in srgb, var(--sf-accent) 18%, transparent);
      color: var(--sf-text);
      font-weight: 600;
    }
    .current .dot {
      border-color: var(--sf-accent);
      color: var(--sf-accent);
    }
  `,
})
export class PipelineStepperComponent {
  readonly status = input.required<ProjectStatus>();
  protected readonly stages = PIPELINE_STAGES;
  protected readonly current = computed(() => stageIndex(this.status()));
}

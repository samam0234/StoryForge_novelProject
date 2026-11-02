import { Component, effect, inject, signal } from '@angular/core';
import { CommunityApi } from '../../core/api/community-api.service';
import { WorkGraph } from '../../core/models/community';
import { WorkGraphComponent } from '../../shared/components/work-graph/work-graph.component';
import { ProjectContext } from './project-context';

/** 프로젝트 작업 기록: 수동·에이전트 작업 그래프 */
@Component({
  selector: 'sf-project-activity-page',
  imports: [WorkGraphComponent],
  template: `
    <div class="sf-page">
      <p class="sf-muted hint">막대를 누르면 그날 한 작업이 표에 나옵니다. 게시하면 같은 기록이 작품의 작업 그래프로 공개됩니다.</p>
      @if (graph(); as g) {
        <sf-work-graph [graph]="g" />
      }
    </div>
  `,
  styles: `
    .hint {
      margin: 0 0 12px;
      font-size: 13px;
    }
  `,
})
export class ProjectActivityPage {
  private readonly ctx = inject(ProjectContext);
  private readonly api = inject(CommunityApi);
  protected readonly graph = signal<WorkGraph | null>(null);

  constructor() {
    effect(() => {
      const id = this.ctx.projectId();
      if (id != null) this.api.projectGraph(id).subscribe((g) => this.graph.set(g));
    });
  }
}

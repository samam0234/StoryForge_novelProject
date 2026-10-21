import { Injectable, computed, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { StoryforgeApi } from '../../core/api/storyforge-api.service';
import { Project, ProjectStatus, ProjectStatusInfo } from '../../core/models/models';

/**
 * 프로젝트 화면(ProjectShellPage)과 하위 화면이 공유하는 상태.
 * 상태 전이 판단은 백엔드가 하고, 여기서는 받은 상태를 보여주기만 합니다.
 */
@Injectable()
export class ProjectContext {
  private readonly api = inject(StoryforgeApi);

  readonly project = signal<Project | null>(null);
  readonly statusInfo = signal<ProjectStatusInfo | null>(null);
  readonly status = computed<ProjectStatus | null>(() => this.statusInfo()?.status ?? null);
  readonly projectId = computed(() => this.project()?.id ?? null);

  load(id: number): void {
    forkJoin([this.api.getProject(id), this.api.getStatus(id)]).subscribe(([project, status]) => {
      this.project.set(project);
      this.statusInfo.set(status);
    });
  }

  refresh(): void {
    const id = this.projectId();
    if (id != null) this.load(id);
  }

  is(...statuses: ProjectStatus[]): boolean {
    const s = this.status();
    return s != null && statuses.includes(s);
  }
}

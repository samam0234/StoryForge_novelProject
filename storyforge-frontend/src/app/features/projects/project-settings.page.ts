import { Component, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { StoryforgeApi, errorMessage } from '../../core/api/storyforge-api.service';
import { EXPORT_FORMATS, ExportFormat, MODEL_OPTIONS } from '../../core/models/models';
import { ProjectContext } from './project-context';

/** 프로젝트 설정 (기획서 §9.6 5번: 모델 선택, 기본 톤, 내보내기 옵션) */
@Component({
  selector: 'sf-project-settings-page',
  imports: [FormsModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatIconModule, MatInputModule, MatSelectModule],
  template: `
    <div class="sf-page narrow">
      <mat-card appearance="outlined" class="card">
        <mat-card-header><mat-card-title>프로젝트 설정</mat-card-title></mat-card-header>
        <mat-card-content class="sf-form body">
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>제목</mat-label>
            <input matInput name="title" [(ngModel)]="title" maxlength="200" required />
          </mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>장르</mat-label>
            <input matInput name="genre" [(ngModel)]="genre" maxlength="100" />
          </mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>생성 모델</mat-label>
            <mat-select name="model" [(ngModel)]="model">
              <mat-option [value]="null">서비스 기본값</mat-option>
              @for (m of models; track m.value) {
                <mat-option [value]="m.value">{{ m.label }}</mat-option>
              }
            </mat-select>
            <mat-hint>mock 모드에서는 적용되지 않습니다.</mat-hint>
          </mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>기본 톤</mat-label>
            <textarea matInput name="defaultTone" rows="3" [(ngModel)]="defaultTone" maxlength="2000"></textarea>
            <mat-hint>Story Bible을 생성할 때 반영합니다.</mat-hint>
          </mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>기본 내보내기 형식</mat-label>
            <mat-select name="exportFormat" [(ngModel)]="exportFormat">
              @for (f of formats; track f.value) {
                <mat-option [value]="f.value">{{ f.label }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
        </mat-card-content>
        <mat-card-actions align="end">
          <button mat-flat-button [disabled]="busy() || !title.trim()" (click)="save()"><mat-icon>save</mat-icon> 저장</button>
        </mat-card-actions>
      </mat-card>

      <mat-card appearance="outlined" class="card danger">
        <mat-card-header>
          <mat-card-title>프로젝트 삭제</mat-card-title>
          <mat-card-subtitle>Story Bible, Outline, 챕터, 이력이 모두 삭제되며 되돌릴 수 없습니다.</mat-card-subtitle>
        </mat-card-header>
        <mat-card-actions align="end">
          <button mat-stroked-button class="delete" [disabled]="busy()" (click)="remove()"><mat-icon>delete</mat-icon> 삭제</button>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: `
    .narrow {
      max-width: 720px;
    }
    .card {
      background: var(--sf-surface);
      margin-bottom: 16px;
    }
    .body {
      margin-top: 16px;
      gap: 20px;
    }
    .delete {
      color: var(--sf-severity-high);
    }
  `,
})
export class ProjectSettingsPage {
  protected readonly ctx = inject(ProjectContext);
  private readonly api = inject(StoryforgeApi);
  private readonly snack = inject(MatSnackBar);
  private readonly router = inject(Router);

  protected readonly models = MODEL_OPTIONS;
  protected readonly formats = EXPORT_FORMATS;
  protected readonly busy = signal(false);
  protected title = '';
  protected genre = '';
  protected model: string | null = null;
  protected defaultTone = '';
  protected exportFormat: ExportFormat = 'md';
  private loadedId: number | null = null;

  constructor() {
    effect(() => {
      const p = this.ctx.project();
      // 처음 한 번만 채웁니다. 상태 갱신이 입력 중인 값을 덮어쓰지 않도록 합니다.
      if (!p || untracked(() => this.loadedId) === p.id) return;
      this.loadedId = p.id;
      this.title = p.title;
      this.genre = p.genre ?? '';
      this.model = p.model;
      this.defaultTone = p.defaultTone ?? '';
      this.exportFormat = p.exportFormat;
    });
  }

  protected save(): void {
    const id = this.ctx.projectId();
    if (id == null) return;
    this.busy.set(true);
    this.api
      .updateProject(id, {
        title: this.title.trim(),
        genre: this.genre.trim() || null,
        model: this.model,
        defaultTone: this.defaultTone.trim() || null,
        exportFormat: this.exportFormat,
      })
      .subscribe({
        next: () => {
          this.busy.set(false);
          this.ctx.refresh();
          this.snack.open('저장했습니다.', undefined, { duration: 2000 });
        },
        error: (e) => {
          this.busy.set(false);
          this.snack.open(errorMessage(e), '닫기', { duration: 4000 });
        },
      });
  }

  protected remove(): void {
    const p = this.ctx.project();
    if (!p || !confirm(`"${p.title}" 프로젝트를 삭제할까요? 되돌릴 수 없습니다.`)) return;
    this.busy.set(true);
    this.api.deleteProject(p.id).subscribe({
      next: () => this.router.navigate(['/']),
      error: (e) => {
        this.busy.set(false);
        this.snack.open(errorMessage(e), '닫기', { duration: 4000 });
      },
    });
  }
}

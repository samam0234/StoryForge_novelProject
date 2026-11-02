import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { CommunityApi } from '../../core/api/community-api.service';
import { errorMessage } from '../../core/api/storyforge-api.service';
import { CLONE_POLICY_OPTIONS, ClonePolicy, GENRES, VISIBILITY_OPTIONS, Visibility, WorkKind } from '../../core/models/community';
import { ProjectContext } from './project-context';

/** 프로젝트 게시: 승인된 챕터를 작품으로 올립니다. 게시해도 파이프라인 상태는 바뀌지 않습니다. */
@Component({
  selector: 'sf-project-publish-page',
  imports: [FormsModule, RouterLink, MatButtonModule, MatCheckboxModule, MatFormFieldModule, MatIconModule, MatInputModule, MatSelectModule],
  template: `
    <div class="sf-page narrow">
      @if (ctx.project(); as p) {
        @if (p.workId) {
          <div class="panel">
            <h2>이미 게시한 프로젝트입니다</h2>
            <p class="sf-muted">새 챕터를 승인했다면 작품 수정 화면에서 게시본을 업데이트하세요.</p>
            <div class="sf-row">
              <a mat-flat-button [routerLink]="['/works', p.workId]"><mat-icon>visibility</mat-icon> 작품 보기</a>
              <a mat-stroked-button [routerLink]="['/me/works', p.workId, 'edit']"><mat-icon>edit</mat-icon> 작품 수정·업데이트</a>
            </div>
          </div>
        } @else if (!approved()) {
          <div class="sf-empty">승인된 챕터가 있어야 게시할 수 있습니다. 챕터 탭에서 검토하고 승인하세요.</div>
        } @else {
          <div class="panel sf-form">
            <h2>작품 게시</h2>
            <p class="sf-muted">승인된 챕터 {{ approved() }}장을 게시합니다. 검토 중인 초안은 들어가지 않습니다. 게시 후에 나머지 챕터를 승인하면 업데이트할 수 있습니다.</p>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>게시판</mat-label>
              <mat-select name="kind" [(ngModel)]="kind">
                <mat-option value="LONG">롱품 (장편)</mat-option>
                <mat-option value="SHORT">숏품 (단편)</mat-option>
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>제목</mat-label>
              <input matInput name="title" [(ngModel)]="title" maxlength="200" required />
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>줄거리</mat-label>
              <textarea matInput name="synopsis" rows="3" [(ngModel)]="synopsis" maxlength="2000"></textarea>
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>장르</mat-label>
              <input matInput name="genre" [(ngModel)]="genre" maxlength="100" list="sf-publish-genres" />
              <datalist id="sf-publish-genres">
                @for (g of genres; track g) {
                  <option [value]="g"></option>
                }
              </datalist>
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>태그 (쉼표로 구분)</mat-label>
              <input matInput name="tags" [(ngModel)]="tags" maxlength="300" />
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>공개 범위</mat-label>
              <mat-select name="visibility" [(ngModel)]="visibility">
                @for (v of visibilities; track v.value) {
                  <mat-option [value]="v.value">{{ v.label }} — {{ v.hint }}</mat-option>
                }
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>Clone 정책</mat-label>
              <mat-select name="clonePolicy" [(ngModel)]="clonePolicy">
                @for (o of policies; track o.value) {
                  <mat-option [value]="o.value">{{ o.label }} — {{ o.hint }}</mat-option>
                }
              </mat-select>
            </mat-form-field>
            <mat-checkbox name="downloadable" [(ngModel)]="downloadable">독자의 내려받기 허용</mat-checkbox>
            <p class="sf-muted notice">
              <mat-icon inline>info</mat-icon>
              공개하면 작업 기록(날짜, 수동·에이전트 구분, "3장 직접 수정" 같은 요약)도 함께 공개됩니다. 본문 변경 내용은 공개되지 않습니다.
              @if (p.clonedFromTitle) {
                이 프로젝트는 「{{ p.clonedFromTitle }}」의 Clone이라 원작으로 표시됩니다.
              }
            </p>
            <div class="sf-row">
              <span class="sf-spacer"></span>
              <button mat-flat-button [disabled]="busy() || !title.trim()" (click)="publish()"><mat-icon>publish</mat-icon> 게시</button>
            </div>
          </div>
        }
      }
    </div>
  `,
  styles: `
    .narrow {
      max-width: 720px;
    }
    .panel {
      padding: 16px;
      border: 1px solid var(--sf-border);
      border-radius: var(--sf-radius);
      background: var(--sf-surface);
    }
    .sf-form {
      gap: 12px;
    }
    h2 {
      margin: 0;
      font-size: 18px;
    }
    .notice {
      font-size: 13px;
      margin: 0;
    }
  `,
})
export class ProjectPublishPage {
  protected readonly ctx = inject(ProjectContext);
  private readonly api = inject(CommunityApi);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);

  protected readonly genres = GENRES;
  protected readonly visibilities = VISIBILITY_OPTIONS;
  protected readonly policies = CLONE_POLICY_OPTIONS;
  protected readonly busy = signal(false);
  protected readonly approved = computed(() => this.ctx.statusInfo()?.confirmedChapters ?? 0);

  protected kind: WorkKind = 'LONG';
  protected title = '';
  protected synopsis = '';
  protected genre = '';
  protected tags = '';
  protected visibility: Visibility = 'PUBLIC';
  protected clonePolicy: ClonePolicy = 'OPEN';
  protected downloadable = true;
  private filled: number | null = null;

  constructor() {
    effect(() => {
      const p = this.ctx.project();
      if (!p || untracked(() => this.filled) === p.id) return;
      this.filled = p.id;
      this.title = p.title;
      this.genre = p.genre ?? '';
      this.kind = p.form === 'SHORT' ? 'SHORT' : 'LONG';
    });
  }

  protected publish(): void {
    const id = this.ctx.projectId();
    if (id == null) return;
    this.busy.set(true);
    this.api
      .publish(id, {
        kind: this.kind,
        title: this.title.trim(),
        synopsis: this.synopsis.trim() || null,
        genre: this.genre.trim() || null,
        tags: this.tags.trim() || null,
        visibility: this.visibility,
        clonePolicy: this.clonePolicy,
        downloadable: this.downloadable,
      })
      .subscribe({
        next: (w) => this.router.navigate(['/works', w.id]),
        error: (e) => {
          this.busy.set(false);
          this.snack.open(errorMessage(e), '닫기', { duration: 4000 });
        },
      });
  }
}

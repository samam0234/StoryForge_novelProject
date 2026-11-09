import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { CommunityApi } from '../../core/api/community-api.service';
import { errorMessage } from '../../core/api/storyforge-api.service';
import { CLONE_POLICY_OPTIONS, ClonePolicy, GENRES, VISIBILITY_OPTIONS, Visibility, WorkDetail } from '../../core/models/community';
import { RichTextEditorComponent } from '../../shared/components/rich-text-editor/rich-text-editor.component';
import { ImageDropComponent } from '../../shared/components/image-drop/image-drop.component';

/** 게시 작품 수정: 정보, 본문 직접 수정, 프로젝트에서 업데이트 */
@Component({
  selector: 'sf-work-edit-page',
  imports: [
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    RichTextEditorComponent,
    ImageDropComponent,
  ],
  template: `
    <div class="sf-page">
      @if (detail(); as d) {
        <div class="sf-row">
          <a mat-button routerLink="/me/works"><mat-icon>arrow_back</mat-icon> 내 작품 관리</a>
          <span class="sf-spacer"></span>
          <a mat-button [routerLink]="['/works', d.card.id]">작품 보기 <mat-icon iconPositionEnd>open_in_new</mat-icon></a>
          <button mat-button class="danger" (click)="remove()">삭제</button>
        </div>
        <h1>{{ d.card.title }} <span class="sf-muted rev">r{{ d.card.revision }}</span></h1>

        <section class="panel sf-form">
          <h2>작품 정보</h2>
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>제목</mat-label>
            <input matInput name="title" [(ngModel)]="title" maxlength="200" required />
          </mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>줄거리</mat-label>
            <textarea matInput name="synopsis" rows="3" [(ngModel)]="synopsis" maxlength="2000"></textarea>
          </mat-form-field>
          <div class="grid">
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>장르</mat-label>
              <input matInput name="genre" [(ngModel)]="genre" maxlength="100" list="sf-genres" />
              <datalist id="sf-genres">
                @for (g of genres; track g) {
                  <option [value]="g"></option>
                }
              </datalist>
            </mat-form-field>
            <sf-image-drop [url]="coverUrl || null" (uploaded)="coverUrl = $event" />
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>표지 주소 (올린 파일 또는 https)</mat-label>
              <input matInput name="coverUrl" [(ngModel)]="coverUrl" maxlength="500" />
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>공개 범위</mat-label>
              <mat-select name="visibility" [(ngModel)]="visibility">
                @for (v of visibilities; track v.value) {
                  <mat-option [value]="v.value">{{ v.label }}</mat-option>
                }
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>Clone 정책</mat-label>
              <mat-select name="clonePolicy" [(ngModel)]="clonePolicy">
                @for (p of policies; track p.value) {
                  <mat-option [value]="p.value">{{ p.label }}</mat-option>
                }
              </mat-select>
            </mat-form-field>
          </div>
          <mat-checkbox name="downloadable" [(ngModel)]="downloadable">독자의 내려받기 허용</mat-checkbox>
          <div class="sf-row">
            <span class="sf-spacer"></span>
            <button mat-flat-button [disabled]="busy() || !title.trim()" (click)="saveMeta()"><mat-icon>save</mat-icon> 정보 저장</button>
          </div>
        </section>

        @if (d.projectId) {
          <section class="panel sf-form">
            <h2>프로젝트에서 업데이트</h2>
            <p class="sf-muted">프로젝트의 승인된 챕터, Story Bible, Outline을 다시 가져옵니다. 아래에서 게시본만 고친 내용은 덮어씁니다.</p>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>변경 내용 (선택)</mat-label>
              <input matInput name="syncMessage" [(ngModel)]="syncMessage" maxlength="300" />
            </mat-form-field>
            <div class="sf-row">
              <span class="sf-spacer"></span>
              <button mat-stroked-button [disabled]="busy()" (click)="sync()"><mat-icon>sync</mat-icon> 업데이트</button>
            </div>
          </section>
        }

        <section class="panel">
          <h2>본문 직접 수정</h2>
          <p class="sf-muted">게시본만 고칩니다. 프로젝트 원고는 바뀌지 않으며, 저장하면 리비전이 올라가고 작업 그래프에 수동 작업으로 남습니다.</p>
          <div class="editor-wrap">
            <nav class="toc" aria-label="챕터 목록">
              @for (c of d.chapters; track c.number) {
                <button type="button" [class.on]="number() === c.number" (click)="open(c.number)">{{ c.number }}. {{ c.title || '제목 없음' }}</button>
              }
            </nav>
            <div class="editor">
              @if (number() !== null) {
                <mat-form-field appearance="outline" subscriptSizing="dynamic" class="sf-full">
                  <mat-label>챕터 제목</mat-label>
                  <input matInput name="chapterTitle" [(ngModel)]="chapterTitle" maxlength="200" />
                </mat-form-field>
                <sf-rich-text-editor [value]="loadedContent()" (valueChange)="chapterContent = $event" />
                <div class="sf-row">
                  <button mat-stroked-button type="button" [disabled]="busy()" (click)="complete()"><mat-icon>auto_awesome</mat-icon> 이어 쓰기</button>
                  <span class="sf-spacer"></span>
                  <mat-form-field appearance="outline" subscriptSizing="dynamic" class="message">
                    <mat-label>변경 내용 (선택)</mat-label>
                    <input matInput name="chapterMessage" [(ngModel)]="chapterMessage" maxlength="300" />
                  </mat-form-field>
                  <button mat-flat-button [disabled]="busy()" (click)="saveChapter()"><mat-icon>save</mat-icon> 본문 저장</button>
                </div>
              }
            </div>
          </div>
        </section>
      }
    </div>
  `,
  styles: `
    h1 {
      font-size: 22px;
      margin: 8px 0;
    }
    .rev {
      font-size: 14px;
      font-weight: 400;
    }
    h2 {
      font-size: 16px;
      margin: 0 0 8px;
    }
    .panel {
      padding: 16px;
      margin-top: 16px;
      border: 1px solid var(--sf-border);
      border-radius: var(--sf-radius);
      background: var(--sf-surface);
    }
    .sf-form {
      gap: 12px;
    }
    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 12px;
    }
    .editor-wrap {
      display: grid;
      grid-template-columns: 200px minmax(0, 1fr);
      gap: 12px;
    }
    @media (max-width: 800px) {
      .editor-wrap {
        grid-template-columns: 1fr;
      }
    }
    .toc {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }
    .toc button {
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
    .toc button.on {
      background: color-mix(in srgb, var(--sf-secondary) 18%, transparent);
      font-weight: 600;
    }
    .editor {
      display: flex;
      flex-direction: column;
      gap: 8px;
      min-width: 0;
    }
    .editor sf-rich-text-editor {
      min-height: 320px;
      flex: none;
    }
    .danger {
      color: #b42318;
    }
    .message {
      flex: 1;
    }
  `,
})
export class WorkEditPage {
  private readonly api = inject(CommunityApi);
  private readonly snack = inject(MatSnackBar);
  private readonly router = inject(Router);

  readonly workId = input.required<string>();

  protected readonly genres = GENRES;
  protected readonly visibilities = VISIBILITY_OPTIONS;
  protected readonly policies = CLONE_POLICY_OPTIONS;
  protected readonly detail = signal<WorkDetail | null>(null);
  protected readonly busy = signal(false);
  protected readonly number = signal<number | null>(null);
  protected readonly loadedContent = signal('');
  protected readonly id = computed(() => Number(this.workId()));

  protected title = '';
  protected synopsis = '';
  protected genre = '';
  protected tags = '';
  protected visibility: Visibility = 'PUBLIC';
  protected clonePolicy: ClonePolicy = 'OPEN';
  protected downloadable = true;
  protected coverUrl = '';
  protected syncMessage = '';
  protected chapterTitle = '';
  protected chapterContent = '';
  protected chapterMessage = '';

  constructor() {
    effect(() => this.reload(this.id(), true));
  }

  private reload(id: number, fill = false): void {
    this.api.work(id).subscribe({
      next: (d) => {
        this.detail.set(d);
        if (fill) {
          this.title = d.card.title;
          this.synopsis = d.card.synopsis ?? '';
          this.genre = d.card.genre ?? '';
          this.tags = d.card.tags.join(', ');
          this.visibility = d.card.visibility;
          this.clonePolicy = d.card.clonePolicy;
          this.downloadable = d.downloadable;
          this.coverUrl = d.card.coverUrl ?? '';
          if (d.chapters.length) this.open(this.number() ?? d.chapters[0].number);
        }
      },
      error: (e) => this.snack.open(errorMessage(e), '닫기', { duration: 4000 }),
    });
  }

  protected open(number: number): void {
    this.api.workChapter(this.id(), number).subscribe({
      next: (c) => {
        this.number.set(c.number);
        this.chapterTitle = c.title ?? '';
        this.chapterContent = c.content ?? '';
        this.loadedContent.set(c.content ?? '');
        this.chapterMessage = '';
      },
      error: (e) => this.snack.open(errorMessage(e), '닫기', { duration: 4000 }),
    });
  }

  protected saveMeta(): void {
    const d = this.detail();
    if (!d) return;
    this.run(
      this.api.updateWork(d.card.id, {
        title: this.title.trim(),
        synopsis: this.synopsis.trim() || null,
        genre: this.genre.trim() || null,
        tags: this.tags.trim() || null,
        visibility: this.visibility,
        clonePolicy: this.clonePolicy,
        downloadable: this.downloadable,
        coverUrl: this.coverUrl.trim(),
      }),
      '저장했습니다.',
      () => this.reload(d.card.id),
    );
  }

  protected sync(): void {
    const d = this.detail();
    if (!d || !confirm('프로젝트 내용으로 게시본을 덮어쓸까요?')) return;
    this.run(this.api.syncWork(d.card.id, this.syncMessage.trim() || null), '게시본을 업데이트했습니다.', () => {
      this.syncMessage = '';
      this.reload(d.card.id, true);
    });
  }

  protected complete(): void {
    const number = this.number();
    if (number === null) return;
    this.busy.set(true);
    const text = this.chapterContent;
    const cut = Math.max(0, text.length - 4000);
    this.api.completeChapter(this.id(), number, text.slice(cut), '').subscribe({
      next: (r) => {
        this.busy.set(false);
        const suggestion = r.suggestion?.trim();
        if (!suggestion) return;
        this.chapterContent = `${text.trimEnd()}\n\n${suggestion}`;
        this.loadedContent.set(this.chapterContent);
      },
      error: (e) => {
        this.busy.set(false);
        this.snack.open(errorMessage(e), '닫기', { duration: 4000 });
      },
    });
  }

  protected remove(): void {
    const d = this.detail();
    if (!d || !confirm(`"${d.card.title}" 게시를 삭제할까요? 프로젝트 원고는 남습니다.`)) return;
    this.api.deleteWork(d.card.id).subscribe({
      next: () => this.router.navigate(['/me/works']),
      error: (e) => this.snack.open(errorMessage(e), '닫기', { duration: 4000 }),
    });
  }

  protected saveChapter(): void {
    const number = this.number();
    if (number === null) return;
    this.run(
      this.api.editWorkChapter(this.id(), number, {
        title: this.chapterTitle.trim() || null,
        content: this.chapterContent,
        message: this.chapterMessage.trim() || null,
      }),
      '본문을 저장했습니다.',
      () => {
        this.chapterMessage = '';
        this.reload(this.id());
      },
    );
  }

  private run<T>(request: Observable<T>, message: string, done: () => void): void {
    this.busy.set(true);
    request.subscribe({
      next: () => {
        this.busy.set(false);
        this.snack.open(message, undefined, { duration: 2000 });
        done();
      },
      error: (e) => {
        this.busy.set(false);
        this.snack.open(errorMessage(e), '닫기', { duration: 4000 });
      },
    });
  }
}

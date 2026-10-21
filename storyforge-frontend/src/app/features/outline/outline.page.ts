import { CdkDrag, CdkDragDrop, CdkDragHandle, CdkDropList, moveItemInArray } from '@angular/cdk/drag-drop';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { StoryforgeApi, errorMessage } from '../../core/api/storyforge-api.service';
import { Outline, OutlineChapter } from '../../core/models/models';
import { ProjectContext } from '../projects/project-context';

/** 편집용 뷰 모델: 목록 필드는 줄/쉼표 단위 텍스트로 다룹니다. */
interface ChapterDraft {
  title: string;
  goals: string;
  beats: string;
  characters: string;
  continuityPoints: string;
}

const lines = (s: string) => s.split('\n').map((x) => x.trim()).filter(Boolean);

/** Outline 관리 (기획서 §6.2, §9.6 3번) */
@Component({
  selector: 'sf-outline-page',
  imports: [
    FormsModule,
    CdkDropList,
    CdkDrag,
    CdkDragHandle,
    MatButtonModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
  ],
  template: `
    <div class="sf-page">
      @if (busy()) {
        <mat-progress-bar mode="indeterminate" class="busy" />
      }

      @if (ctx.is('BIBLE_DRAFT')) {
        <div class="sf-empty">Story Bible을 먼저 확정해야 Outline을 만들 수 있습니다.</div>
      } @else {
        <div class="sf-row toolbar">
          @if (canGenerate()) {
            <mat-form-field appearance="outline" class="count" subscriptSizing="dynamic">
              <mat-label>챕터 수</mat-label>
              <input matInput type="number" min="1" max="50" [(ngModel)]="chapterCount" />
            </mat-form-field>
            <button mat-stroked-button [disabled]="busy()" (click)="generate()">
              <mat-icon>auto_awesome</mat-icon> {{ chapters().length ? 'AI로 다시 생성' : 'AI로 Outline 생성' }}
            </button>
          }
          <span class="sf-spacer"></span>
          @if (editable()) {
            @if (dirty()) {
              <span class="unsaved">저장되지 않은 변경</span>
            }
            <button mat-stroked-button (click)="addChapter()"><mat-icon>add</mat-icon> 챕터 추가</button>
            <button mat-stroked-button [disabled]="busy() || !dirty()" (click)="save()"><mat-icon>save</mat-icon> 저장</button>
            <button mat-flat-button [disabled]="busy() || dirty() || !chapters().length" (click)="confirm()">
              <mat-icon>check_circle</mat-icon> Outline 확정
            </button>
          } @else if (chapters().length) {
            <span class="locked"><mat-icon>lock</mat-icon> 확정된 Outline입니다</span>
            <button mat-flat-button (click)="goChapters()">챕터 작성 <mat-icon iconPositionEnd>arrow_forward</mat-icon></button>
          }
        </div>

        @if (loaded() && !chapters().length) {
          <div class="sf-empty">아직 Outline이 없습니다. 챕터 수를 정하고 AI로 생성하세요.</div>
        }

        <mat-accordion cdkDropList (cdkDropListDropped)="drop($event)" [cdkDropListDisabled]="!editable()" multi>
          @for (c of chapters(); track c; let i = $index) {
            <mat-expansion-panel cdkDrag class="chapter">
              <mat-expansion-panel-header>
                <mat-panel-title>
                  @if (editable()) {
                    <mat-icon cdkDragHandle class="handle" (click)="$event.stopPropagation()">drag_indicator</mat-icon>
                  }
                  <span class="num">{{ i + 1 }}장</span>
                  {{ c.title || '(제목 없음)' }}
                </mat-panel-title>
                <mat-panel-description>{{ lines(c.beats).length }} beats</mat-panel-description>
              </mat-expansion-panel-header>
              <div class="sf-form">
                <mat-form-field appearance="outline">
                  <mat-label>제목</mat-label>
                  <input matInput [(ngModel)]="c.title" (ngModelChange)="touch()" [readonly]="!editable()" />
                </mat-form-field>
                <mat-form-field appearance="outline">
                  <mat-label>주요 Beats (한 줄에 하나)</mat-label>
                  <textarea matInput rows="4" [(ngModel)]="c.beats" (ngModelChange)="touch()" [readonly]="!editable()"></textarea>
                </mat-form-field>
                <div class="two">
                  <mat-form-field appearance="outline">
                    <mat-label>등장 캐릭터 (쉼표로 구분)</mat-label>
                    <input matInput [(ngModel)]="c.characters" (ngModelChange)="touch()" [readonly]="!editable()" />
                  </mat-form-field>
                  <mat-form-field appearance="outline">
                    <mat-label>긴장/감정 목표</mat-label>
                    <input matInput [(ngModel)]="c.goals" (ngModelChange)="touch()" [readonly]="!editable()" />
                  </mat-form-field>
                </div>
                <mat-form-field appearance="outline">
                  <mat-label>연속성 포인트 (한 줄에 하나)</mat-label>
                  <textarea matInput rows="2" [(ngModel)]="c.continuityPoints" (ngModelChange)="touch()" [readonly]="!editable()"></textarea>
                </mat-form-field>
                @if (editable()) {
                  <div><button mat-button (click)="removeChapter(i)"><mat-icon>delete</mat-icon> 이 챕터 삭제</button></div>
                }
              </div>
            </mat-expansion-panel>
          }
        </mat-accordion>
      }
    </div>
  `,
  styles: `
    .busy {
      margin-bottom: 12px;
    }
    .toolbar {
      margin-bottom: 16px;
    }
    .count {
      width: 110px;
    }
    .unsaved {
      color: var(--sf-accent);
      font-size: 13px;
    }
    .locked {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      color: var(--sf-text-muted);
    }
    .chapter {
      background: var(--sf-surface);
    }
    .num {
      color: var(--sf-emphasis);
      font-weight: 700;
      margin-right: 8px;
    }
    .handle {
      cursor: grab;
      margin-right: 6px;
      color: var(--sf-text-muted);
    }
    .two {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
    }
    .cdk-drag-preview {
      box-shadow: 0 8px 24px rgb(0 0 0 / 0.35);
    }
    .cdk-drag-placeholder {
      opacity: 0.3;
    }
  `,
})
export class OutlinePage {
  protected readonly ctx = inject(ProjectContext);
  private readonly api = inject(StoryforgeApi);
  private readonly snack = inject(MatSnackBar);
  private readonly router = inject(Router);

  protected readonly chapters = signal<ChapterDraft[]>([]);
  protected readonly loaded = signal(false);
  protected readonly busy = signal(false);
  protected readonly dirty = signal(false);
  protected readonly editable = computed(() => this.ctx.is('OUTLINE_DRAFT'));
  protected readonly canGenerate = computed(() => this.ctx.is('BIBLE_CONFIRMED', 'OUTLINE_DRAFT'));
  protected readonly lines = lines;
  protected chapterCount = 10;

  constructor() {
    effect(() => {
      const id = this.ctx.projectId();
      if (id == null) return;
      this.api.getOutline(id).subscribe({
        next: (o) => {
          this.apply(o);
          this.loaded.set(true);
        },
        error: (e) => this.toast(e),
      });
    });
  }

  protected touch(): void {
    this.dirty.set(true);
  }

  protected drop(e: CdkDragDrop<ChapterDraft[]>): void {
    const list = [...this.chapters()];
    moveItemInArray(list, e.previousIndex, e.currentIndex);
    this.chapters.set(list);
    this.touch();
  }

  protected addChapter(): void {
    this.chapters.update((l) => [...l, { title: '', goals: '', beats: '', characters: '', continuityPoints: '' }]);
    this.touch();
  }

  protected removeChapter(i: number): void {
    this.chapters.update((l) => l.filter((_, idx) => idx !== i));
    this.touch();
  }

  protected generate(): void {
    const id = this.ctx.projectId();
    if (id == null) return;
    if (this.chapters().length && !confirm('현재 Outline을 덮어쓰고 다시 생성할까요?')) return;
    this.run(this.api.generateOutline(id, this.chapterCount), (o) => {
      this.apply(o);
      this.ctx.refresh();
    });
  }

  protected save(): void {
    const id = this.ctx.projectId();
    if (id == null) return;
    this.run(this.api.updateOutline(id, this.toModel()), (o) => {
      this.apply(o);
      this.snack.open('저장했습니다.', undefined, { duration: 2000 });
    });
  }

  protected confirm(): void {
    const id = this.ctx.projectId();
    if (id == null) return;
    this.run(this.api.confirmOutline(id), () => {
      this.ctx.refresh();
      this.snack.open('Outline을 확정했습니다.', undefined, { duration: 2500 });
    });
  }

  protected goChapters(): void {
    this.router.navigate(['/projects', this.ctx.projectId(), 'chapters']);
  }

  private apply(o: Outline | null): void {
    this.dirty.set(false);
    this.chapters.set(
      (o?.chapters ?? []).map((c) => ({
        title: c.title ?? '',
        goals: c.goals ?? '',
        beats: (c.beats ?? []).join('\n'),
        characters: (c.characters ?? []).join(', '),
        continuityPoints: (c.continuityPoints ?? []).join('\n'),
      })),
    );
  }

  private toModel(): OutlineChapter[] {
    return this.chapters().map((c, i) => ({
      order: i + 1,
      title: c.title.trim(),
      goals: c.goals.trim() || null,
      beats: lines(c.beats),
      characters: c.characters.split(',').map((x) => x.trim()).filter(Boolean),
      continuityPoints: lines(c.continuityPoints),
    }));
  }

  private run<T>(obs: Observable<T>, onOk: (v: T) => void): void {
    this.busy.set(true);
    obs.subscribe({
      next: (v) => {
        this.busy.set(false);
        onOk(v);
      },
      error: (e) => {
        this.busy.set(false);
        this.toast(e);
      },
    });
  }

  private toast(e: unknown): void {
    this.snack.open(errorMessage(e), '닫기', { duration: 4000 });
  }
}

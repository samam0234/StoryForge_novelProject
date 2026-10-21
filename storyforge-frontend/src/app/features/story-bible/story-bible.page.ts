import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { StoryforgeApi, errorMessage } from '../../core/api/storyforge-api.service';
import { Character, StoryBible } from '../../core/models/models';
import { ProjectContext } from '../projects/project-context';

interface BibleDraft {
  worldSetting: string;
  theme: string;
  tone: string;
  characters: Character[];
}

/** Story Bible 에디터 (기획서 §6.1, §9.6 2번) */
@Component({
  selector: 'sf-story-bible-page',
  imports: [
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatTabsModule,
  ],
  template: `
    <div class="sf-page">
      @if (busy()) {
        <mat-progress-bar mode="indeterminate" class="busy" />
      }

      @if (loaded() && !bible()) {
        <mat-card appearance="outlined" class="intro">
          <mat-card-header>
            <mat-card-title>어떤 이야기를 쓰고 싶나요?</mat-card-title>
            <mat-card-subtitle>아이디어를 적으면 AI가 세계관, 캐릭터, 테마, 톤 초안을 만듭니다.</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content class="sf-form">
            <mat-form-field appearance="outline">
              <mat-label>아이디어</mat-label>
              <textarea matInput rows="5" [(ngModel)]="prompt" maxlength="4000"></textarea>
            </mat-form-field>
          </mat-card-content>
          <mat-card-actions align="end">
            <button mat-flat-button [disabled]="!prompt.trim() || busy() || !editable()" (click)="generate()">
              <mat-icon>auto_awesome</mat-icon> AI로 생성
            </button>
          </mat-card-actions>
        </mat-card>
      }

      @if (draft(); as d) {
        <div class="sf-row toolbar">
          <span class="sf-muted">버전 {{ bible()?.version }}</span>
          @if (dirty()) {
            <span class="unsaved">저장되지 않은 변경</span>
          } @else {
            <span class="sf-muted">저장됨</span>
          }
          <span class="sf-spacer"></span>
          @if (editable()) {
            <button mat-stroked-button [disabled]="busy() || !dirty()" (click)="save()">
              <mat-icon>save</mat-icon> 저장
            </button>
            <button mat-flat-button [disabled]="busy() || dirty()" (click)="confirm()">
              <mat-icon>check_circle</mat-icon> Bible 확정
            </button>
          } @else {
            <span class="locked"><mat-icon>lock</mat-icon> 확정된 Bible입니다</span>
          }
        </div>

        <mat-tab-group animationDuration="0ms">
          <mat-tab label="Overview">
            <div class="tab sf-form">
              <mat-form-field appearance="outline">
                <mat-label>톤</mat-label>
                <textarea matInput rows="3" [(ngModel)]="d.tone" (ngModelChange)="touch()" [readonly]="!editable()"></textarea>
              </mat-form-field>
              @if (editable()) {
                <div class="regen">
                  <mat-form-field appearance="outline">
                    <mat-label>다시 생성할 아이디어</mat-label>
                    <textarea matInput rows="3" [(ngModel)]="prompt" maxlength="4000"></textarea>
                    <mat-hint>다시 생성하면 현재 내용을 덮어쓰고 버전이 올라갑니다.</mat-hint>
                  </mat-form-field>
                  <button mat-stroked-button [disabled]="!prompt.trim() || busy()" (click)="generate()">
                    <mat-icon>auto_awesome</mat-icon> AI로 다시 생성
                  </button>
                </div>
              }
            </div>
          </mat-tab>

          <mat-tab label="Characters ({{ d.characters.length }})">
            <div class="tab">
              <div class="chars">
                @for (c of d.characters; track $index; let i = $index) {
                  <mat-card appearance="outlined" class="char">
                    <mat-card-content class="sf-form">
                      <div class="sf-row">
                        <mat-form-field appearance="outline" class="name">
                          <mat-label>이름</mat-label>
                          <input matInput [(ngModel)]="c.name" (ngModelChange)="touch()" [readonly]="!editable()" />
                        </mat-form-field>
                        @if (editable()) {
                          <button mat-icon-button (click)="removeCharacter(i)" aria-label="캐릭터 삭제">
                            <mat-icon>delete</mat-icon>
                          </button>
                        }
                      </div>
                      <mat-form-field appearance="outline">
                        <mat-label>설명 (외모, 배경)</mat-label>
                        <textarea matInput rows="2" [(ngModel)]="c.description" (ngModelChange)="touch()" [readonly]="!editable()"></textarea>
                      </mat-form-field>
                      <mat-form-field appearance="outline">
                        <mat-label>성격</mat-label>
                        <textarea matInput rows="2" [(ngModel)]="c.personality" (ngModelChange)="touch()" [readonly]="!editable()"></textarea>
                      </mat-form-field>
                      <mat-form-field appearance="outline">
                        <mat-label>관계</mat-label>
                        <textarea matInput rows="2" [(ngModel)]="c.relationships" (ngModelChange)="touch()" [readonly]="!editable()"></textarea>
                      </mat-form-field>
                    </mat-card-content>
                  </mat-card>
                }
              </div>
              @if (editable()) {
                <button mat-stroked-button (click)="addCharacter()"><mat-icon>person_add</mat-icon> 캐릭터 추가</button>
              }
            </div>
          </mat-tab>

          <mat-tab label="World">
            <div class="tab sf-form">
              <mat-form-field appearance="outline">
                <mat-label>세계관</mat-label>
                <textarea matInput rows="14" [(ngModel)]="d.worldSetting" (ngModelChange)="touch()" [readonly]="!editable()"></textarea>
              </mat-form-field>
            </div>
          </mat-tab>

          <mat-tab label="Themes">
            <div class="tab sf-form">
              <mat-form-field appearance="outline">
                <mat-label>테마</mat-label>
                <textarea matInput rows="8" [(ngModel)]="d.theme" (ngModelChange)="touch()" [readonly]="!editable()"></textarea>
              </mat-form-field>
            </div>
          </mat-tab>
        </mat-tab-group>

        @if (!editable() && ctx.is('BIBLE_CONFIRMED')) {
          <div class="next">
            <span>다음 단계: Outline을 만드세요.</span>
            <button mat-flat-button (click)="goOutline()">Outline으로 <mat-icon iconPositionEnd>arrow_forward</mat-icon></button>
          </div>
        }
      }
    </div>
  `,
  styles: `
    .busy {
      margin-bottom: 12px;
    }
    .intro {
      max-width: 720px;
      margin: 24px auto;
      background: var(--sf-surface);
    }
    .toolbar {
      margin-bottom: 8px;
      font-size: 13px;
    }
    .unsaved {
      color: var(--sf-accent);
    }
    .locked {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      color: var(--sf-text-muted);
    }
    .tab {
      padding: 16px 0;
    }
    .regen {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 8px;
      margin-top: 8px;
    }
    .regen mat-form-field {
      width: 100%;
    }
    .chars {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 12px;
      margin-bottom: 12px;
    }
    .char {
      background: var(--sf-surface);
    }
    .name {
      flex: 1;
    }
    .next {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      margin-top: 16px;
      padding: 12px 16px;
      border-radius: var(--sf-radius);
      background: color-mix(in srgb, var(--sf-secondary) 14%, transparent);
    }
  `,
})
export class StoryBiblePage {
  protected readonly ctx = inject(ProjectContext);
  private readonly api = inject(StoryforgeApi);
  private readonly snack = inject(MatSnackBar);
  private readonly router = inject(Router);

  protected readonly bible = signal<StoryBible | null>(null);
  protected readonly draft = signal<BibleDraft | null>(null);
  protected readonly loaded = signal(false);
  protected readonly busy = signal(false);
  protected readonly dirty = signal(false);
  protected readonly editable = computed(() => this.ctx.is('BIBLE_DRAFT'));
  protected prompt = '';

  constructor() {
    effect(() => {
      const id = this.ctx.projectId();
      if (id == null) return;
      this.api.getBible(id).subscribe({
        next: (b) => {
          this.apply(b);
          this.loaded.set(true);
        },
        error: (e) => this.toast(e),
      });
    });
  }

  protected touch(): void {
    this.dirty.set(true);
  }

  protected addCharacter(): void {
    this.draft()?.characters.push({ name: '새 캐릭터', description: '', personality: '', relationships: '' });
    this.touch();
  }

  protected removeCharacter(i: number): void {
    this.draft()?.characters.splice(i, 1);
    this.touch();
  }

  protected generate(): void {
    const id = this.ctx.projectId();
    if (id == null) return;
    if (this.dirty() && !confirm('저장하지 않은 변경이 있습니다. 덮어쓰고 다시 생성할까요?')) return;
    this.run(this.api.generateBible(id, this.prompt.trim()), (b) => {
      this.apply(b);
      this.ctx.refresh();
    });
  }

  protected save(): void {
    const id = this.ctx.projectId();
    const d = this.draft();
    if (id == null || !d) return;
    this.run(this.api.updateBible(id, d), (b) => {
      this.apply(b);
      this.snack.open('저장했습니다.', undefined, { duration: 2000 });
    });
  }

  protected confirm(): void {
    const id = this.ctx.projectId();
    if (id == null) return;
    this.run(this.api.confirmBible(id), () => {
      this.ctx.refresh();
      this.snack.open('Story Bible을 확정했습니다.', undefined, { duration: 2500 });
    });
  }

  protected goOutline(): void {
    this.router.navigate(['/projects', this.ctx.projectId(), 'outline']);
  }

  private apply(b: StoryBible | null): void {
    this.bible.set(b);
    this.dirty.set(false);
    this.draft.set(
      b && {
        worldSetting: b.worldSetting ?? '',
        theme: b.theme ?? '',
        tone: b.tone ?? '',
        characters: b.characters.map((c) => ({ ...c })),
      },
    );
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

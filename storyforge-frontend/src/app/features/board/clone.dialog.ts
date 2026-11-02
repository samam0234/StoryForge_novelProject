import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { CLONE_MODES, CloneMode } from '../../core/models/community';

export interface CloneDialogData {
  title: string;
  revision: number;
  allowed: CloneMode[];
}

/** Clone 범위 선택. 결과 {mode, title}을 닫을 때 돌려줍니다. */
@Component({
  selector: 'sf-clone-dialog',
  imports: [FormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatIconModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>「{{ data.title }}」 Clone</h2>
    <mat-dialog-content>
      <p class="sf-muted">
        r{{ data.revision }} 기준으로 내 프로젝트를 만듭니다. 공유와 달리 창작 자료를 가져와 이어 쓰는 기능이며, 원본은 바뀌지 않습니다.
      </p>
      <div class="modes" role="radiogroup" aria-label="Clone 범위">
        @for (m of modes; track m.value) {
          <button
            type="button"
            class="mode"
            role="radio"
            [attr.aria-checked]="mode() === m.value"
            [class.on]="mode() === m.value"
            [disabled]="!data.allowed.includes(m.value)"
            (click)="mode.set(m.value)"
          >
            <mat-icon>{{ m.icon }}</mat-icon>
            <strong>{{ m.label }}</strong>
            <span>{{ m.hint }}</span>
            <small class="sf-muted">{{ data.allowed.includes(m.value) ? m.start : '작가가 허용하지 않은 범위' }}</small>
          </button>
        }
      </div>
      <mat-form-field appearance="outline" class="sf-full" subscriptSizing="dynamic">
        <mat-label>새 프로젝트 제목 (비우면 자동)</mat-label>
        <input matInput [(ngModel)]="title" maxlength="200" />
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>취소</button>
      <button mat-flat-button (click)="submit()"><mat-icon>call_split</mat-icon> Clone</button>
    </mat-dialog-actions>
  `,
  styles: `
    .modes {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
      gap: 8px;
      margin: 12px 0 16px;
    }
    .mode {
      display: flex;
      flex-direction: column;
      gap: 4px;
      align-items: flex-start;
      text-align: left;
      padding: 12px;
      font: inherit;
      color: var(--sf-text);
      border: 1px solid var(--sf-border);
      border-radius: var(--sf-radius);
      background: var(--sf-surface);
      cursor: pointer;
    }
    .mode.on {
      border-color: var(--sf-emphasis);
      background: color-mix(in srgb, var(--sf-secondary) 12%, transparent);
    }
    .mode:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  `,
})
export class CloneDialog {
  protected readonly data = inject<CloneDialogData>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<CloneDialog>);
  protected readonly modes = CLONE_MODES;
  protected readonly mode = signal<CloneMode>(this.data.allowed[0]);
  protected title = '';

  protected submit(): void {
    this.ref.close({ mode: this.mode(), title: this.title.trim() || null });
  }
}

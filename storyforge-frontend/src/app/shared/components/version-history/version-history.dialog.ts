import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { diffWordsWithSpace } from 'diff';

export interface VersionEntry<T = unknown> {
  version: number;
  /** 예: "AI 생성", "직접 수정" */
  label?: string;
  createdAt: string;
  text: string;
  payload: T;
}

export interface VersionHistoryData<T = unknown> {
  title: string;
  /** 최신순 */
  versions: VersionEntry<T>[];
  /** true면 선택한 버전을 편집기로 불러올 수 있습니다. */
  canRestore: boolean;
}

/**
 * 버전 비교 (기획서 §6.1 버전 관리, §6.4 생성 히스토리 및 Diff).
 * 불러오기는 편집기에 채우기만 합니다. 저장은 사용자가 합니다. 닫을 때 선택한 항목의 payload를 돌려줍니다.
 */
@Component({
  selector: 'sf-version-history-dialog',
  imports: [DatePipe, FormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatSelectModule],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      @if (data.versions.length === 0) {
        <p class="sf-muted">아직 이력이 없습니다.</p>
      } @else {
        <div class="selects">
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>이전 버전</mat-label>
            <mat-select [ngModel]="fromVersion()" (ngModelChange)="fromVersion.set($event)">
              @for (v of data.versions; track v.version) {
                <mat-option [value]="v.version">{{ label(v) }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>비교 버전</mat-label>
            <mat-select [ngModel]="toVersion()" (ngModelChange)="toVersion.set($event)">
              @for (v of data.versions; track v.version) {
                <mat-option [value]="v.version">{{ label(v) }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
        </div>
        <p class="sf-muted when">
          v{{ to()?.version }} 저장 시각: {{ to()?.createdAt | date: 'yyyy-MM-dd HH:mm:ss' }}
          @if (!hasChanges()) {
            · 두 버전이 같습니다
          }
        </p>
        <pre class="diff">@for (part of diff(); track $index) {<span [class.added]="part.added" [class.removed]="part.removed">{{ part.value }}</span>}</pre>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>닫기</button>
      @if (data.canRestore && to(); as selected) {
        <button mat-flat-button [mat-dialog-close]="selected.payload">v{{ selected.version }} 불러오기</button>
      }
    </mat-dialog-actions>
  `,
  styles: `
    .selects {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
      margin-top: 8px;
    }
    .when {
      font-size: 13px;
      margin: 12px 0 8px;
    }
    .diff {
      margin: 0;
      padding: 16px;
      max-height: 50vh;
      overflow-y: auto;
      white-space: pre-wrap;
      word-break: break-word;
      font-family: var(--sf-font);
      line-height: 1.7;
      border: 1px solid var(--sf-border);
      border-radius: var(--sf-radius);
      background: var(--sf-surface-2);
    }
    .added {
      background: color-mix(in srgb, #2e9e5b 30%, transparent);
    }
    .removed {
      background: color-mix(in srgb, #d9534f 30%, transparent);
      text-decoration: line-through;
    }
  `,
})
export class VersionHistoryDialog {
  protected readonly data = inject<VersionHistoryData>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<VersionHistoryDialog>);

  /** 기본값: 가장 최근 변경 (직전 버전 → 최신 버전) */
  protected readonly toVersion = signal(this.data.versions[0]?.version ?? 0);
  protected readonly fromVersion = signal(this.data.versions[1]?.version ?? this.data.versions[0]?.version ?? 0);

  private readonly from = computed(() => this.find(this.fromVersion()));
  protected readonly to = computed(() => this.find(this.toVersion()));
  protected readonly diff = computed(() => diffWordsWithSpace(this.from()?.text ?? '', this.to()?.text ?? ''));
  protected readonly hasChanges = computed(() => this.diff().some((p) => p.added || p.removed));

  protected label(v: VersionEntry): string {
    return v.label ? `v${v.version} · ${v.label}` : `v${v.version}`;
  }

  private find(version: number): VersionEntry | undefined {
    return this.data.versions.find((v) => v.version === version);
  }
}

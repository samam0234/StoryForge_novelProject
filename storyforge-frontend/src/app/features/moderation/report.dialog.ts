import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CommunityApi } from '../../core/api/community-api.service';
import { errorMessage } from '../../core/api/storyforge-api.service';
import { REPORT_REASONS, ReportReason, ReportTargetType } from '../../core/models/community';

export interface ReportDialogData {
  targetType: ReportTargetType;
  targetId: number;
  /** 무엇을 신고하는지 보여 줄 이름 */
  label: string;
}

/** 신고. 신고자는 작성자에게 알려지지 않습니다. */
@Component({
  selector: 'sf-report-dialog',
  imports: [FormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  template: `
    <h2 mat-dialog-title>신고: {{ data.label }}</h2>
    <mat-dialog-content class="sf-form body">
      <p class="sf-muted note">관리자가 검토합니다. 누가 신고했는지는 작성자에게 알리지 않습니다.</p>
      <mat-form-field appearance="outline" subscriptSizing="dynamic">
        <mat-label>사유</mat-label>
        <mat-select [(ngModel)]="reason" name="reason">
          @for (r of reasons; track r.value) {
            <mat-option [value]="r.value">{{ r.label }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
      <mat-form-field appearance="outline" subscriptSizing="dynamic">
        <mat-label>자세한 내용 (선택)</mat-label>
        <textarea matInput [(ngModel)]="detail" name="detail" rows="3" maxlength="500"></textarea>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>취소</button>
      <button mat-flat-button [disabled]="busy()" (click)="submit()">신고</button>
    </mat-dialog-actions>
  `,
  styles: `
    .body {
      gap: 12px;
      padding-top: 8px;
      min-width: min(420px, 80vw);
    }
    .note {
      margin: 0;
      font-size: 13px;
    }
  `,
})
export class ReportDialog {
  protected readonly data = inject<ReportDialogData>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<ReportDialog>);
  private readonly api = inject(CommunityApi);
  private readonly snack = inject(MatSnackBar);

  protected readonly reasons = REPORT_REASONS;
  protected readonly busy = signal(false);
  protected reason: ReportReason = 'SPAM';
  protected detail = '';

  protected submit(): void {
    this.busy.set(true);
    this.api
      .report({ targetType: this.data.targetType, targetId: this.data.targetId, reason: this.reason, detail: this.detail.trim() || null })
      .subscribe({
        next: () => {
          this.snack.open('신고를 접수했습니다.', undefined, { duration: 2500 });
          this.ref.close(true);
        },
        error: (e) => {
          this.busy.set(false);
          this.snack.open(errorMessage(e), '닫기', { duration: 4000 });
        },
      });
  }
}

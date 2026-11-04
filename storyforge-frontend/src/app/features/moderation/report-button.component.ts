import { Component, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { ModerationApi, ReportReason, ReportTarget } from '../../core/api/moderation-api.service';
import { errorMessage } from '../../core/api/storyforge-api.service';
import { AuthService } from '../../core/auth/auth.service';

/** 로그인한 사람이 글·작품·댓글·사용자를 신고합니다. */
@Component({
  selector: 'sf-report-button',
  imports: [FormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, MatSelectModule],
  template: `
    @if (auth.isLoggedIn()) {
      <button mat-button type="button" (click)="open.set(!open())">
        <mat-icon>flag</mat-icon> 신고
      </button>
      @if (open()) {
        <form class="form" (ngSubmit)="submit()">
          <mat-form-field appearance="outline">
            <mat-label>이유</mat-label>
            <mat-select name="reason" [(ngModel)]="reason" required>
              <mat-option value="SPAM">도배·광고</mat-option>
              <mat-option value="ABUSE">괴롭힘</mat-option>
              <mat-option value="ILLEGAL">불법</mat-option>
              <mat-option value="COPYRIGHT">권리 침해</mat-option>
              <mat-option value="OTHER">기타</mat-option>
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>설명 (선택)</mat-label>
            <textarea matInput name="detail" [(ngModel)]="detail" rows="2" maxlength="1000"></textarea>
          </mat-form-field>
          <button mat-flat-button type="submit" [disabled]="busy()">보내기</button>
        </form>
      }
    }
  `,
  styles: `
    :host {
      display: inline-flex;
      flex-direction: column;
      align-items: flex-start;
    }
    .form {
      display: flex;
      flex-direction: column;
      width: min(360px, 100%);
      margin-top: 8px;
    }
  `,
})
export class ReportButtonComponent {
  private readonly api = inject(ModerationApi);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);
  protected readonly auth = inject(AuthService);

  readonly targetType = input.required<ReportTarget>();
  readonly targetId = input.required<number>();

  protected readonly open = signal(false);
  protected readonly busy = signal(false);
  protected reason: ReportReason = 'SPAM';
  protected detail = '';

  protected submit(): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.api
      .report({
        targetType: this.targetType(),
        targetId: this.targetId(),
        reason: this.reason,
        detail: this.detail.trim() || undefined,
      })
      .subscribe({
        next: () => {
          this.busy.set(false);
          this.open.set(false);
          this.detail = '';
          this.snack.open('신고를 접수했습니다.', '닫기', { duration: 3000 });
        },
        error: (e) => {
          this.busy.set(false);
          if (e?.status === 401) {
            this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
            return;
          }
          this.snack.open(errorMessage(e), '닫기', { duration: 4000 });
        },
      });
  }
}

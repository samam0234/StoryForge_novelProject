import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ModerationApi, ReportView } from '../../core/api/moderation-api.service';
import { errorMessage } from '../../core/api/storyforge-api.service';

const REASON: Record<ReportView['reason'], string> = {
  SPAM: '도배·광고',
  ABUSE: '괴롭힘',
  ILLEGAL: '불법',
  COPYRIGHT: '권리 침해',
  OTHER: '기타',
};

const TARGET: Record<ReportView['targetType'], string> = {
  POST: '게시글',
  WORK: '작품',
  COMMENT: '댓글',
  USER: '사용자',
};

/** 관리자만 연 신고를 기각하거나 공개 목록에서 숨깁니다. */
@Component({
  selector: 'sf-admin-reports-page',
  imports: [DatePipe, MatButtonModule, MatIconModule, MatProgressBarModule],
  template: `
    <div class="sf-page">
      <h1>신고 처리</h1>
      <p class="sf-muted">숨기면 게시글·작품·댓글이 공개 목록에서 빠집니다. 사용자 신고는 계정을 지우지 않습니다.</p>
      @if (error()) {
        <div class="sf-empty" role="alert">{{ error() }}</div>
      } @else if (!reports()) {
        <mat-progress-bar mode="indeterminate" />
      } @else if (!reports()!.length) {
        <div class="sf-empty">열린 신고가 없습니다.</div>
      } @else {
        <ul>
          @for (r of reports(); track r.id) {
            <li>
              <div>
                <strong>{{ targetLabel(r) }} #{{ r.targetId }}</strong>
                <span class="sf-muted">{{ reasonLabel(r) }} · {{ r.reporterName }} · {{ r.createdAt | date: 'yyyy-MM-dd HH:mm' }}</span>
                @if (r.detail) {
                  <p>{{ r.detail }}</p>
                }
              </div>
              <div class="sf-row">
                <button mat-button (click)="dismiss(r)" [disabled]="busy()">기각</button>
                <button mat-stroked-button (click)="hide(r)" [disabled]="busy() || r.targetType === 'USER'">숨기기</button>
              </div>
            </li>
          }
        </ul>
      }
    </div>
  `,
  styles: `
    h1 {
      margin: 0 0 8px;
      font-size: 22px;
    }
    ul {
      list-style: none;
      margin: 16px 0 0;
      padding: 0;
    }
    li {
      display: flex;
      justify-content: space-between;
      gap: 12px;
      padding: 12px 0;
      border-bottom: 1px solid var(--sf-border);
    }
    p {
      margin: 4px 0 0;
      white-space: pre-wrap;
    }
    .sf-muted {
      display: block;
      margin-top: 4px;
    }
  `,
})
export class AdminReportsPage {
  private readonly api = inject(ModerationApi);
  private readonly snack = inject(MatSnackBar);

  protected readonly reports = signal<ReportView[] | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly busy = signal(false);

  constructor() {
    this.load();
  }

  protected targetLabel(r: ReportView): string {
    return TARGET[r.targetType];
  }

  protected reasonLabel(r: ReportView): string {
    return REASON[r.reason];
  }

  protected dismiss(r: ReportView): void {
    this.act(this.api.dismiss(r.id));
  }

  protected hide(r: ReportView): void {
    this.act(this.api.hide(r.id));
  }

  private load(): void {
    this.api.openReports().subscribe({
      next: (rows) => this.reports.set(rows),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  private act(request: ReturnType<ModerationApi['dismiss']>): void {
    this.busy.set(true);
    request.subscribe({
      next: () => {
        this.busy.set(false);
        this.load();
      },
      error: (e) => {
        this.busy.set(false);
        this.snack.open(errorMessage(e), '닫기', { duration: 4000 });
      },
    });
  }
}

import { DatePipe } from '@angular/common';
import { Component, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { CommunityApi } from '../../core/api/community-api.service';
import { errorMessage } from '../../core/api/storyforge-api.service';
import { REPORT_REASONS, ReportAction, ReportReason, ReportStatus, ReportView, labelOf } from '../../core/models/community';

const TARGET_LABEL = { WORK: '작품', POST: '게시글', COMMENT: '댓글', USER: '사용자' } as const;

/** 관리자 신고 처리. 숨김은 복원할 수 있고, 처리 결과는 작성자와 신고자에게 알립니다. */
@Component({
  selector: 'sf-admin-reports-page',
  imports: [DatePipe, FormsModule, RouterLink, MatButtonModule, MatButtonToggleModule, MatFormFieldModule, MatIconModule, MatInputModule, MatPaginatorModule],
  template: `
    <div class="sf-page">
      <h1>신고 관리</h1>
      <mat-button-toggle-group hideSingleSelectionIndicator [value]="status()" (change)="setStatus($event.value)" aria-label="처리 상태">
        <mat-button-toggle value="OPEN">대기</mat-button-toggle>
        <mat-button-toggle value="RESOLVED">처리됨</mat-button-toggle>
        <mat-button-toggle value="DISMISSED">기각</mat-button-toggle>
      </mat-button-toggle-group>

      @if (error()) {
        <div class="sf-empty" role="alert">{{ error() }}</div>
      } @else if (!items().length) {
        <div class="sf-empty">해당하는 신고가 없습니다.</div>
      }
      <ul class="list">
        @for (r of items(); track r.id) {
          <li class="card">
            <div class="sf-row meta">
              <span class="sf-badge">{{ targetLabel[r.targetType] }}</span>
              <span class="sf-badge reason">{{ reasonLabel(r.reason) }}</span>
              @if (r.openReports > 1) {
                <span class="sf-muted">같은 대상 신고 {{ r.openReports }}건</span>
              }
              @if (r.target.hidden) {
                <span class="state"><mat-icon inline>visibility_off</mat-icon> 숨김</span>
              }
              @if (r.target.authorSuspended) {
                <span class="state"><mat-icon inline>block</mat-icon> 작성자 정지</span>
              }
              <span class="sf-spacer"></span>
              <span class="sf-muted when">{{ r.createdAt | date: 'yyyy-MM-dd HH:mm' }}</span>
            </div>
            @if (r.target.exists) {
              <h2>
                @if (r.target.link) {
                  <a [routerLink]="r.target.link">{{ r.target.title }}</a>
                } @else {
                  {{ r.target.title }}
                }
              </h2>
              @if (r.target.excerpt) {
                <p class="excerpt">{{ r.target.excerpt }}</p>
              }
              @if (r.target.author; as a) {
                <p class="sf-muted by">작성자: <a [routerLink]="['/users', a.id]">{{ a.displayName }}</a></p>
              }
            } @else {
              <p class="sf-muted">이미 삭제된 대상입니다.</p>
            }
            <p class="by sf-muted">
              신고자: <a [routerLink]="['/users', r.reporter.id]">{{ r.reporter.displayName }}</a>
              @if (r.detail) {
                — {{ r.detail }}
              }
            </p>
            @if (r.resolutionNote) {
              <p class="by sf-muted">처리 메모: {{ r.resolutionNote }}</p>
            }
            <div class="actions">
              <mat-form-field appearance="outline" subscriptSizing="dynamic" class="note">
                <mat-label>처리 메모 (작성자에게 사유로 전달)</mat-label>
                <input matInput [ngModel]="notes[r.id] ?? ''" (ngModelChange)="notes[r.id] = $event" [name]="'note' + r.id" maxlength="300" />
              </mat-form-field>
              @if (r.targetType !== 'USER' && r.target.exists) {
                @if (r.target.hidden) {
                  <button mat-stroked-button [disabled]="busy()" (click)="decide(r, 'RESTORE')">복원</button>
                } @else {
                  <button mat-flat-button [disabled]="busy()" (click)="decide(r, 'HIDE')">숨김</button>
                }
              }
              @if (r.status === 'OPEN') {
                <button mat-stroked-button [disabled]="busy()" (click)="decide(r, 'DISMISS')">기각</button>
              }
              @if (r.target.author) {
                @if (r.target.authorSuspended) {
                  <button mat-stroked-button [disabled]="busy()" (click)="decide(r, 'UNSUSPEND')">정지 해제</button>
                } @else {
                  <button mat-stroked-button class="danger" [disabled]="busy()" (click)="decide(r, 'SUSPEND')">작성자 정지</button>
                }
              }
            </div>
          </li>
        }
      </ul>
      @if (total() > 20) {
        <mat-paginator [length]="total()" [pageIndex]="page()" [pageSize]="20" [hidePageSize]="true" (page)="onPage($event)" aria-label="신고 페이지" />
      }
    </div>
  `,
  styles: `
    h1 {
      margin: 0 0 12px;
      font-size: 24px;
    }
    h2 {
      margin: 8px 0 4px;
      font-size: 16px;
    }
    .list {
      list-style: none;
      margin: 16px 0 0;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .card {
      padding: 14px 16px;
      border: 1px solid var(--sf-border);
      border-radius: var(--sf-radius);
      background: var(--sf-surface);
    }
    .meta {
      font-size: 13px;
    }
    .reason {
      background: color-mix(in srgb, var(--sf-accent) 22%, transparent);
    }
    .state {
      color: var(--sf-severity-high);
    }
    .when {
      font-size: 12px;
    }
    .excerpt {
      margin: 0 0 8px;
      white-space: pre-wrap;
      overflow-wrap: anywhere;
      font-size: 14px;
    }
    .by {
      margin: 0 0 4px;
      font-size: 13px;
    }
    .actions {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      align-items: center;
      margin-top: 10px;
    }
    .note {
      flex: 1 1 260px;
    }
    .danger {
      color: var(--sf-severity-high);
    }
    mat-paginator {
      background: transparent;
    }
  `,
})
export class AdminReportsPage {
  private readonly api = inject(CommunityApi);
  private readonly snack = inject(MatSnackBar);

  protected readonly targetLabel = TARGET_LABEL;
  protected readonly status = signal<ReportStatus>('OPEN');
  protected readonly page = signal(0);
  protected readonly items = signal<ReportView[]>([]);
  protected readonly total = signal(0);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  private readonly reload = signal(0);
  protected notes: Record<number, string | undefined> = {};

  constructor() {
    effect(() => {
      this.reload();
      this.api.adminReports(this.status(), this.page()).subscribe({
        next: (r) => {
          this.items.set(r.items);
          this.total.set(r.total);
          this.error.set(null);
        },
        error: (e) => this.error.set(errorMessage(e)),
      });
    });
  }

  protected reasonLabel(reason: ReportReason): string {
    return labelOf(REPORT_REASONS, reason);
  }

  protected setStatus(status: ReportStatus): void {
    this.page.set(0);
    this.status.set(status);
  }

  protected onPage(e: PageEvent): void {
    this.page.set(e.pageIndex);
  }

  protected decide(r: ReportView, action: ReportAction): void {
    if (action === 'SUSPEND' && !confirm(`${r.target.author?.displayName}님의 계정을 정지할까요? 글쓰기와 로그인이 막힙니다.`)) return;
    this.busy.set(true);
    this.api.decideReport(r.id, { action, note: this.notes[r.id]?.trim() || null }).subscribe({
      next: () => {
        this.busy.set(false);
        this.snack.open('처리했습니다.', undefined, { duration: 2000 });
        this.reload.update((n) => n + 1);
      },
      error: (e) => {
        this.busy.set(false);
        this.snack.open(errorMessage(e), '닫기', { duration: 4000 });
      },
    });
  }
}

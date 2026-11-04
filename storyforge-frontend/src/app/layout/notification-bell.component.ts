import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatBadgeModule } from '@angular/material/badge';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router } from '@angular/router';
import { interval, startWith } from 'rxjs';
import { CommunityApi } from '../core/api/community-api.service';
import { NotificationView } from '../core/models/community';

const POLL_MS = 60_000;

/** 상단 알림 종. 안 읽은 수는 1분마다 확인하고, 목록은 열 때 읽습니다. */
@Component({
  selector: 'sf-notification-bell',
  imports: [DatePipe, MatBadgeModule, MatButtonModule, MatIconModule, MatMenuModule, MatTooltipModule],
  template: `
    <button
      mat-icon-button
      [matMenuTriggerFor]="menu"
      (menuOpened)="load()"
      matTooltip="알림"
      [attr.aria-label]="unread() ? '알림 ' + unread() + '건 안 읽음' : '알림'"
    >
      <mat-icon [matBadge]="unread()" [matBadgeHidden]="!unread()" matBadgeSize="small" matBadgeColor="warn" aria-hidden="false">notifications</mat-icon>
    </button>
    <mat-menu #menu="matMenu" class="sf-notification-menu">
      <div class="head" (click)="$event.stopPropagation()" (keydown.enter)="$event.stopPropagation()" tabindex="-1">
        <strong>알림</strong>
        <button mat-button [disabled]="!unread()" (click)="readAll()">모두 읽음</button>
      </div>
      @for (n of items(); track n.id) {
        <button mat-menu-item class="row" [class.unread]="!n.read" (click)="open(n)">
          <span class="message">{{ n.message }}</span>
          <small class="when">{{ n.createdAt | date: 'yyyy-MM-dd HH:mm' }}</small>
        </button>
      } @empty {
        <p class="empty">새 알림이 없습니다.</p>
      }
    </mat-menu>
  `,
  styles: `
    .head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
      padding: 4px 8px 4px 16px;
    }
    .empty {
      margin: 0;
      padding: 16px;
      color: var(--sf-text-muted);
      font-size: 14px;
    }
    .row {
      height: auto;
      min-height: 48px;
      line-height: 1.4;
    }
    .row.unread {
      background: color-mix(in srgb, var(--sf-secondary) 14%, transparent);
    }
    .message {
      display: block;
      white-space: normal;
      font-size: 14px;
    }
    .when {
      display: block;
      color: var(--sf-text-muted);
    }
  `,
})
export class NotificationBellComponent {
  private readonly api = inject(CommunityApi);
  private readonly router = inject(Router);

  protected readonly unread = signal(0);
  protected readonly items = signal<NotificationView[]>([]);

  constructor() {
    interval(POLL_MS)
      .pipe(startWith(0), takeUntilDestroyed(inject(DestroyRef)))
      .subscribe(() => this.refreshCount());
  }

  private refreshCount(): void {
    this.api.unreadNotifications().subscribe({ next: (r) => this.unread.set(r.count), error: () => undefined });
  }

  protected load(): void {
    this.api.notifications(0).subscribe((r) => this.items.set(r.items));
    this.refreshCount();
  }

  protected open(n: NotificationView): void {
    if (!n.read) {
      this.api.readNotification(n.id).subscribe(() => this.refreshCount());
    }
    // 서버가 만든 앱 안 경로만 따라갑니다.
    if (n.link?.startsWith('/')) this.router.navigateByUrl(n.link);
  }

  protected readAll(): void {
    this.api.readAllNotifications().subscribe(() => {
      this.items.update((list) => list.map((n) => ({ ...n, read: true })));
      this.unread.set(0);
    });
  }
}

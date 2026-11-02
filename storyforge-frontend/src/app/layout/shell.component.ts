import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
import { ThemeService } from './theme.service';

/** 헤더 + 사이드 내비게이션 + 본문 (기획서 §9.5 대시보드 목업) */
@Component({
  selector: 'sf-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
  ],
  template: `
    <mat-toolbar class="topbar">
      <a routerLink="/" class="brand">
        <mat-icon>auto_stories</mat-icon>
        <span>StoryForge</span>
      </a>
      <span class="sf-spacer"></span>
      @if (auth.user(); as u) {
        <span class="user" [matTooltip]="u.email">{{ u.displayName }}</span>
      }
      <button
        mat-icon-button
        (click)="theme.toggle()"
        [matTooltip]="theme.mode() === 'dark' ? '라이트 모드' : '다크 모드'"
        aria-label="테마 전환"
      >
        <mat-icon>{{ theme.mode() === 'dark' ? 'light_mode' : 'dark_mode' }}</mat-icon>
      </button>
      @if (auth.isLoggedIn()) {
        <button mat-icon-button (click)="auth.logout()" matTooltip="로그아웃" aria-label="로그아웃">
          <mat-icon>logout</mat-icon>
        </button>
      }
    </mat-toolbar>

    <mat-sidenav-container class="container">
      <mat-sidenav mode="side" [opened]="auth.isLoggedIn()" class="sidenav">
        <mat-nav-list>
          <a mat-list-item routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">
            <mat-icon matListItemIcon>dashboard</mat-icon>
            <span matListItemTitle>대시보드</span>
          </a>
        </mat-nav-list>
      </mat-sidenav>
      <mat-sidenav-content>
        <router-outlet />
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: `
    :host {
      display: flex;
      flex-direction: column;
      height: 100vh;
    }
    .topbar {
      border-bottom: 1px solid var(--sf-border);
      background: var(--sf-surface);
    }
    .brand {
      display: flex;
      align-items: center;
      gap: 8px;
      text-decoration: none;
      font-weight: 700;
      color: var(--sf-emphasis);
    }
    .user {
      font-size: 14px;
      margin-right: 8px;
      color: var(--sf-text-muted);
    }
    .container {
      flex: 1;
      min-height: 0;
      background: var(--sf-bg);
    }
    .sidenav {
      width: 220px;
      border-right: 1px solid var(--sf-border);
      background: var(--sf-surface);
    }
    .active {
      background: color-mix(in srgb, var(--sf-secondary) 18%, transparent);
    }
  `,
})
export class ShellComponent {
  protected readonly theme = inject(ThemeService);
  protected readonly auth = inject(AuthService);
}

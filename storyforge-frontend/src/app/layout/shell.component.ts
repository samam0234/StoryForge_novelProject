import { BreakpointObserver } from '@angular/cdk/layout';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from '../core/auth/auth.service';
import { GuideChatComponent } from '../features/guide/guide-chat.component';
import { ThemeService } from './theme.service';

/** 헤더 + 사이드 내비게이션 + 본문 (기획서 §9.5 대시보드 목업). 게시판은 로그인 없이 볼 수 있습니다. */
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
    GuideChatComponent,
  ],
  template: `
    <mat-toolbar class="topbar">
      @if (narrow()) {
        <button mat-icon-button aria-label="메뉴" (click)="navOpen.set(!navOpen())"><mat-icon>menu</mat-icon></button>
      }
      <a routerLink="/issues" class="brand">
        <mat-icon>auto_stories</mat-icon>
        <span>StoryForge</span>
      </a>
      <span class="sf-spacer"></span>
      @if (auth.user(); as u) {
        <a class="user" [routerLink]="['/users', u.id]" [matTooltip]="u.email">{{ u.displayName }}</a>
        <button
          mat-icon-button
          (click)="guideOpen.set(!guideOpen())"
          matTooltip="AI 가이드"
          aria-label="AI 가이드 열기"
          [attr.aria-pressed]="guideOpen()"
        >
          <mat-icon>support_agent</mat-icon>
        </button>
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
      } @else {
        <a mat-stroked-button routerLink="/login">로그인</a>
      }
    </mat-toolbar>

    <div class="body">
    <mat-sidenav-container class="container">
      <mat-sidenav [mode]="narrow() ? 'over' : 'side'" [opened]="navVisible()" (closedStart)="navOpen.set(false)" class="sidenav">
        <mat-nav-list>
          <h2 class="nav-title">게시판</h2>
          @for (item of boards; track item.link) {
            <a mat-list-item [routerLink]="item.link" routerLinkActive="active" (click)="closeIfNarrow()">
              <mat-icon matListItemIcon>{{ item.icon }}</mat-icon>
              <span matListItemTitle>{{ item.label }}</span>
            </a>
          }
          @if (auth.user(); as u) {
            <h2 class="nav-title">내 공간</h2>
            <a mat-list-item routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }" (click)="closeIfNarrow()">
              <mat-icon matListItemIcon>dashboard</mat-icon>
              <span matListItemTitle>내 프로젝트</span>
            </a>
            <a mat-list-item routerLink="/me/works" routerLinkActive="active" (click)="closeIfNarrow()">
              <mat-icon matListItemIcon>library_books</mat-icon>
              <span matListItemTitle>내 작품 관리</span>
            </a>
            <a mat-list-item [routerLink]="['/users', u.id]" routerLinkActive="active" (click)="closeIfNarrow()">
              <mat-icon matListItemIcon>person</mat-icon>
              <span matListItemTitle>프로필</span>
            </a>
            <a mat-list-item routerLink="/me/settings" routerLinkActive="active" (click)="closeIfNarrow()">
              <mat-icon matListItemIcon>settings</mat-icon>
              <span matListItemTitle>계정 설정</span>
            </a>
          }
        </mat-nav-list>
      </mat-sidenav>
      <mat-sidenav-content>
        <router-outlet />
      </mat-sidenav-content>
    </mat-sidenav-container>
    @if (guideOpen() && auth.isLoggedIn()) {
      <!-- 화면을 가리지 않고 옆에 둔 채 이동하며 쓰도록 오버레이가 아닌 패널로 둡니다. -->
      <aside class="guide" aria-label="AI 가이드" (keydown.escape)="guideOpen.set(false)">
        <sf-guide-chat (closed)="guideOpen.set(false)" />
      </aside>
    }
    </div>
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
      text-decoration: none;
    }
    .user:hover {
      text-decoration: underline;
    }
    .body {
      position: relative;
      display: flex;
      flex: 1;
      min-height: 0;
    }
    .container {
      flex: 1;
      min-width: 0;
      background: var(--sf-bg);
    }
    .sidenav {
      width: 220px;
      border-right: 1px solid var(--sf-border);
      background: var(--sf-surface);
    }
    .guide {
      position: absolute;
      top: 0;
      right: 0;
      bottom: 0;
      z-index: 5;
      width: min(420px, 100vw);
      box-sizing: border-box;
      border-left: 1px solid var(--sf-border);
      background: var(--sf-surface);
      box-shadow: -2px 0 8px rgb(0 0 0 / 0.2);
    }
    .nav-title {
      margin: 12px 16px 4px;
      font-size: 12px;
      font-weight: 600;
      color: var(--sf-text-muted);
    }
    .active {
      background: color-mix(in srgb, var(--sf-secondary) 18%, transparent);
    }
  `,
})
export class ShellComponent {
  protected readonly theme = inject(ThemeService);
  protected readonly auth = inject(AuthService);

  protected readonly boards = [
    { link: '/board/long', label: '롱품', icon: 'menu_book' },
    { link: '/board/short', label: '숏품', icon: 'article' },
    { link: '/board/package', label: '패키지포지', icon: 'hub' },
    { link: '/create', label: '창작하기', icon: 'edit' },
    { link: '/issues', label: '이슈', icon: 'local_fire_department' },
  ];

  protected readonly narrow = toSignal(
    inject(BreakpointObserver)
      .observe('(max-width: 900px)')
      .pipe(map((r) => r.matches)),
    { initialValue: false },
  );
  protected readonly navOpen = signal(false);
  protected readonly navVisible = computed(() => !this.narrow() || this.navOpen());
  protected readonly guideOpen = signal(false);

  protected closeIfNarrow(): void {
    if (this.narrow()) this.navOpen.set(false);
  }
}

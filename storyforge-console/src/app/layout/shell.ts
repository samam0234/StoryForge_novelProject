import { Component, inject } from "@angular/core";
import {
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
} from "@angular/router";
import { Auth } from "../core/auth";

@Component({
  selector: "sf-shell",
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: ` <a class="skip" href="#content">본문으로 건너뛰기</a>
    <div class="workspace">
      <aside class="sidebar">
        <a class="brand" routerLink="/"
          ><span class="brand-mark">S</span> StoryForge
          <small>CONSOLE</small></a
        >
        <div class="nav-label">운영 워크스페이스</div>
        <nav aria-label="운영 메뉴">
          <a
            routerLink="/"
            routerLinkActive="active"
            [routerLinkActiveOptions]="{ exact: true }"
            ><span aria-hidden="true">◫</span> 운영 현황</a
          >
          <a routerLink="/reports" routerLinkActive="active"
            ><span aria-hidden="true">⚑</span> 신고 관리</a
          >
          <a routerLink="/users" routerLinkActive="active"
            ><span aria-hidden="true">◎</span> 회원 관리</a
          >
          <a routerLink="/audit" routerLinkActive="active"
            ><span aria-hidden="true">≡</span> 감사 기록</a
          >
        </nav>
        <div class="sidebar-note">
          <span class="status-dot"></span> 관리자 전용 공간
          <p>모든 운영 조치는 감사 기록으로 남습니다.</p>
        </div>
      </aside>
      <div class="main-area">
        <header class="topbar">
          <span>WORKSPACE <b>/</b> 운영 콘솔</span>
          <div>
            <span class="avatar">{{
              auth.user()?.displayName?.slice(0, 1)
            }}</span
            ><span>{{ auth.user()?.displayName }}</span
            ><button class="quiet" (click)="logout()">로그아웃</button>
          </div>
        </header>
        <main id="content" tabindex="-1"><router-outlet /></main>
        <footer>
          StoryForge Operations <span>창작의 흐름을 지키는 운영 공간</span>
        </footer>
      </div>
    </div>`,
})
export class Shell {
  readonly auth = inject(Auth);
  private readonly router = inject(Router);
  logout() {
    this.auth.clear();
    void this.router.navigateByUrl("/login");
  }
}

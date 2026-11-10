import { Component, inject, signal } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router";
import { finalize } from "rxjs";
import { Auth } from "../core/auth";
import { errorMessage } from "../core/api";

@Component({
  selector: "sf-login",
  imports: [FormsModule],
  template: ` <main class="login-layout">
    <section class="login-story">
      <div class="brand"><span class="brand-mark">S</span> StoryForge</div>
      <span class="eyebrow">OPERATIONS CONSOLE</span>
      <h1>이야기가 이어지는 곳,<br />그 뒤의 운영.</h1>
      <p>창작자와 커뮤니티를 살피고,<br />안전한 창작 환경을 함께 만듭니다.</p>
      <div class="login-caption">StoryForge · 운영자 전용 워크스페이스</div>
    </section>
    <section class="login-form">
      <span class="eyebrow">AUTHORIZED ACCESS</span>
      <h2>운영 콘솔 로그인</h2>
      <p class="muted">운영자 권한이 등록된 계정으로 접속하세요.</p>
      <form (ngSubmit)="submit()" #form="ngForm">
        <label
          >이메일<input
            type="email"
            name="email"
            [(ngModel)]="email"
            autocomplete="username"
            required
            placeholder="admin@example.com"
            [disabled]="busy()"
        /></label>
        <label
          >비밀번호<input
            type="password"
            name="password"
            [(ngModel)]="password"
            autocomplete="current-password"
            required
            [disabled]="busy()"
        /></label>
        @if (error()) {
          <p class="error" role="alert">{{ error() }}</p>
        }
        <button
          class="primary login-button"
          [disabled]="form.invalid || busy()"
        >
          {{ busy() ? "권한 확인 중…" : "운영 콘솔 접속 →" }}
        </button>
      </form>
      <p class="login-help">
        접근 권한이 필요하면 서비스 관리자에게 문의하세요.<br />보안을 위해 공용
        기기에서는 사용 후 로그아웃하세요.
      </p>
    </section>
  </main>`,
})
export class LoginPage {
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);
  email = "";
  password = "";
  readonly busy = signal(false);
  readonly error = signal("");
  submit() {
    if (this.busy() || !this.email || !this.password) return;
    this.busy.set(true);
    this.error.set("");
    this.auth
      .login(this.email, this.password)
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: () => {
          this.password = "";
          void this.router.navigateByUrl("/");
        },
        error: (e) => {
          this.password = "";
          this.error.set(errorMessage(e));
        },
      });
  }
}

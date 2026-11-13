import { Component, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { errorMessage } from '../../core/api/storyforge-api.service';
import { AuthService } from '../../core/auth/auth.service';

/** 로그인 / 회원가입 (docs/plan/auth.md) */
@Component({
  selector: 'sf-login-page',
  imports: [FormsModule, RouterLink, MatCheckboxModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatIconModule, MatInputModule],
  template: `
    <div class="wrap">
      <mat-card appearance="outlined" class="card">
        <mat-card-header>
          <mat-card-title>{{ mode() === 'login' ? '로그인' : '회원가입' }}</mat-card-title>
          <mat-card-subtitle>StoryForge로 장편을 완성하세요.</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <form class="sf-form" (ngSubmit)="submit()">
            @if (mode() === 'register') {
              <mat-form-field appearance="outline">
                <mat-label>이름</mat-label>
                <input matInput name="displayName" [(ngModel)]="displayName" maxlength="100" autocomplete="name" required />
              </mat-form-field>
            }
            <mat-form-field appearance="outline">
              <mat-label>이메일</mat-label>
              <input matInput name="email" type="email" [(ngModel)]="email" autocomplete="email" required />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>비밀번호</mat-label>
              <input
                matInput
                name="password"
                type="password"
                [(ngModel)]="password"
                maxlength="72"
                [autocomplete]="mode() === 'login' ? 'current-password' : 'new-password'"
                required
              />
              @if (mode() === 'register') {
                <mat-hint>8자 이상</mat-hint>
              }
            </mat-form-field>
            @if (mode() === 'register') {
              <mat-checkbox name="acceptTerms" [(ngModel)]="acceptTerms">
                <a routerLink="/terms" target="_blank">이용약관</a>에 동의합니다
              </mat-checkbox>
            }
            @if (error()) {
              <p class="error" role="alert">{{ error() }}</p>
            }
            <button mat-flat-button type="submit" [disabled]="busy() || !canSubmit()">
              {{ mode() === 'login' ? '로그인' : '가입하기' }}
            </button>
          </form>
        </mat-card-content>
        <mat-card-actions align="end">
          <a mat-button routerLink="/account-mail">비밀번호를 잊으셨나요?</a>
          <a mat-button routerLink="/issues">로그인 없이 둘러보기</a>
          <button mat-button type="button" (click)="toggle()">
            {{ mode() === 'login' ? '계정이 없나요? 회원가입' : '이미 계정이 있나요? 로그인' }}
          </button>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: `
    .wrap {
      display: grid;
      place-items: center;
      min-height: calc(100vh - 64px);
      padding: 24px 16px;
      box-sizing: border-box;
      background:
        radial-gradient(480px 240px at 50% 0%, color-mix(in srgb, var(--sf-accent) 30%, transparent), transparent 70%);
    }
    .card {
      width: 100%;
      max-width: 440px;
      background: var(--sf-surface);
      box-shadow: var(--sf-shadow);
    }
    form {
      margin-top: 16px;
    }
    .error {
      color: var(--sf-severity-high);
      margin: 4px 0 8px;
    }
  `,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly returnUrl = input<string>();

  protected readonly mode = signal<'login' | 'register'>('login');
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected email = '';
  protected password = '';
  protected displayName = '';
  protected acceptTerms = false;

  protected canSubmit(): boolean {
    if (!this.email.trim() || !this.password) return false;
    return this.mode() === 'login' || (this.password.length >= 8 && !!this.displayName.trim() && this.acceptTerms);
  }

  protected toggle(): void {
    this.mode.update((m) => (m === 'login' ? 'register' : 'login'));
    this.error.set(null);
  }

  protected submit(): void {
    if (!this.canSubmit() || this.busy()) return;
    this.busy.set(true);
    this.error.set(null);
    const email = this.email.trim();
    const request =
      this.mode() === 'login'
        ? this.auth.login({ email, password: this.password })
        : this.auth.register({ email, password: this.password, displayName: this.displayName.trim(), acceptTerms: this.acceptTerms });
    request.subscribe({
      next: () => this.router.navigateByUrl(this.returnUrl() || '/'),
      error: (e) => {
        this.busy.set(false);
        this.error.set(errorMessage(e));
      },
    });
  }
}

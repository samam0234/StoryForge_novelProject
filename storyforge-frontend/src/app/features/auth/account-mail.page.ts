import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/auth/auth.service';
import { errorMessage } from '../../core/api/storyforge-api.service';

@Component({
  selector: 'sf-account-mail-page',
  imports: [FormsModule, RouterLink, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule],
  template: `
    <div class="sf-page wrap"><mat-card appearance="outlined">
      <mat-card-header><mat-card-title>{{ mode === 'verify' ? '이메일 인증' : '비밀번호 재설정' }}</mat-card-title></mat-card-header>
      <mat-card-content>
        @if (message()) { <p role="status">{{ message() }}</p> }
        @if (error()) { <p role="alert">{{ error() }}</p> }
        @if (!done() && enabled()) {
          <form class="sf-form" (ngSubmit)="submit()">
            @if (mode === 'request') {
              <p>가입할 때 사용한 이메일로 재설정 링크를 보내드립니다.</p>
              <mat-form-field appearance="outline"><mat-label>이메일</mat-label>
                <input matInput type="email" name="email" autocomplete="email" [(ngModel)]="email" maxlength="254" required />
              </mat-form-field>
            } @else if (mode === 'reset') {
              <mat-form-field appearance="outline"><mat-label>새 비밀번호</mat-label>
                <input matInput type="password" name="password" autocomplete="new-password" [(ngModel)]="password" minlength="8" maxlength="72" required />
                <mat-hint>8자 이상, UTF-8 기준 72바이트 이하</mat-hint>
              </mat-form-field>
              <mat-form-field appearance="outline"><mat-label>새 비밀번호 확인</mat-label>
                <input matInput type="password" name="confirmation" autocomplete="new-password" [(ngModel)]="confirmation" required />
              </mat-form-field>
            } @else { <p>버튼을 눌러 이메일 인증을 완료하세요.</p> }
            <button mat-flat-button type="submit" [disabled]="busy() || !canSubmit()">{{ mode === 'request' ? '재설정 메일 요청' : '완료하기' }}</button>
          </form>
        }
      </mat-card-content>
      <mat-card-actions><a mat-button routerLink="/login">로그인으로 이동</a><button mat-button (click)="newRequest()">새 링크 요청</button></mat-card-actions>
    </mat-card></div>
  `,
  styles: `.wrap { max-width: 480px; margin: 32px auto; } mat-card { background: var(--sf-surface); } form { margin-top: 20px; gap: 24px; } [role=alert] { color: var(--sf-severity-high); }`,
})
export class AccountMailPage {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly base = `${environment.apiBaseUrl}/auth/mail`;
  private readonly fragment = inject(ActivatedRoute).snapshot.fragment ?? '';
  private readonly match = /^(verify|reset):([A-Za-z0-9_-]{43})$/.exec(this.fragment);
  mode = this.match?.[1] ?? 'request';
  private token = this.match?.[2] ?? '';
  readonly enabled = signal(false);
  readonly busy = signal(false);
  readonly done = signal(false);
  readonly message = signal('메일 기능을 확인하고 있습니다.');
  readonly error = signal('');
  email = ''; password = ''; confirmation = '';

  constructor() {
    if (this.fragment) void this.router.navigate([], { fragment: undefined, replaceUrl: true });
    if (this.fragment && !this.match) {
      this.error.set('올바르지 않은 링크입니다. 새 링크를 요청하세요.');
      this.done.set(true);
    }
    this.http.get<{ enabled: boolean }>(`${this.base}/status`).subscribe({
      next: r => { this.enabled.set(r.enabled); this.message.set(r.enabled ? '' : '현재 메일 기능을 사용할 수 없습니다.'); },
      error: e => { this.message.set(''); this.error.set(errorMessage(e)); },
    });
  }
  canSubmit(): boolean {
    return this.mode === 'request' ? !!this.email.trim() : this.mode === 'verify' ? !!this.token
      : this.password.length >= 8 && new TextEncoder().encode(this.password).length <= 72 && this.password === this.confirmation;
  }
  newRequest(): void {
    if (this.busy()) return;
    this.mode = 'request'; this.token = ''; this.password = ''; this.confirmation = '';
    this.done.set(false); this.error.set(''); this.message.set('');
  }
  submit(): void {
    if (this.busy() || !this.canSubmit()) return;
    this.busy.set(true); this.error.set('');
    const path = this.mode === 'request' ? 'reset-request' : this.mode;
    const body = this.mode === 'request' ? { email: this.email.trim() } : { token: this.token, password: this.password };
    this.http.post<{ message?: string } | null>(`${this.base}/${path}`, body).subscribe({
      next: r => {
        this.done.set(true); this.busy.set(false); this.password = ''; this.confirmation = ''; this.token = '';
        this.message.set(r?.message ?? (this.mode === 'verify' ? '이메일 인증을 완료했습니다.' : '비밀번호를 변경했습니다. 새 비밀번호로 로그인하세요.'));
        if (this.mode === 'reset') this.auth.clearSession();
        else if (this.mode === 'verify' && this.auth.isLoggedIn()) this.auth.refreshUser().subscribe();
      },
      error: e => { this.busy.set(false); this.error.set(errorMessage(e)); },
    });
  }
}

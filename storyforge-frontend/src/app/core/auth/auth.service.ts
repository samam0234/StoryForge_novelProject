import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, finalize, shareReplay, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, User } from '../models/models';

const STORAGE_KEY = 'storyforge.session';

interface Session {
  token: string;
  expiresAt: string;
  refreshToken?: string;
  refreshExpiresAt?: string;
  user: User;
}

/** 로그인 상태와 JWT 보관 (docs/plan/auth.md). 토큰 검증은 백엔드가 합니다. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly base = `${environment.apiBaseUrl}/auth`;

  private readonly session = signal<Session | null>(restore());
  private refreshing: Observable<AuthResponse> | null = null;
  readonly user = computed(() => this.session()?.user ?? null);
  readonly isLoggedIn = computed(() => this.token() !== null || this.hasRefresh());

  /** 만료된 액세스 토큰은 돌려주지 않습니다. 갱신 토큰이 있으면 세션은 유지합니다. */
  token(): string | null {
    const s = this.session();
    if (!s || isExpired(s.expiresAt)) return null;
    return s.token;
  }

  hasRefresh(): boolean {
    const s = this.session();
    return !!s?.refreshToken && !!s.refreshExpiresAt && !isExpired(s.refreshExpiresAt);
  }

  /** 동시에 여러 401이 나도 갱신 요청은 한 번만 보냅니다. */
  refreshSession(): Observable<AuthResponse> {
    const refreshToken = this.session()?.refreshToken;
    if (!refreshToken) return throwError(() => new Error('갱신 토큰이 없습니다.'));
    if (!this.refreshing) {
      this.refreshing = this.http.post<AuthResponse>(`${this.base}/refresh`, { refreshToken }).pipe(
        tap((r) => this.store(r)),
        finalize(() => { this.refreshing = null; }),
        shareReplay(1),
      );
    }
    return this.refreshing;
  }

  login(body: { email: string; password: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/login`, body).pipe(tap((r) => this.store(r)));
  }

  register(body: { email: string; password: string; displayName: string; acceptTerms: boolean }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/register`, body).pipe(tap((r) => this.store(r)));
  }

  /** 프로필을 고친 뒤 세션의 이름 등을 맞춥니다. */
  setUser(user: User): void {
    const s = this.session();
    if (!s) return;
    this.persist({ ...s, user });
  }

  refreshUser(): Observable<User> {
    return this.http.get<User>(`${this.base}/me`).pipe(tap((u) => this.setUser(u)));
  }

  logout(): void {
    const refreshToken = this.session()?.refreshToken;
    if (refreshToken) {
      this.http.post(`${this.base}/logout`, { refreshToken }).subscribe({ error: () => undefined });
    }
    this.clear();
    this.router.navigate(['/login']);
  }

  clearSession(): void { this.clear(); }

  requestVerification(): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.base}/mail/verify-request`, {});
  }

  /** 서버가 401을 돌려줬을 때: 세션을 지우고 돌아올 주소와 함께 로그인으로 보냅니다. */
  expire(): void {
    this.clear();
    this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
  }

  private store(r: AuthResponse): void {
    this.persist({
      token: r.accessToken,
      expiresAt: r.expiresAt,
      refreshToken: r.refreshToken,
      refreshExpiresAt: r.refreshExpiresAt,
      user: r.user,
    });
  }

  private persist(s: Session): void {
    this.session.set(s);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(s));
    } catch {
      /* 저장소를 쓸 수 없으면 새로고침 전까지만 유지 */
    }
  }

  private clear(): void {
    this.session.set(null);
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      /* 무시 */
    }
  }
}

function isExpired(iso: string): boolean {
  return new Date(iso).getTime() <= Date.now();
}

function restore(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const s = JSON.parse(raw) as Session;
    if (!s.token || !s.user) return null;
    if (!isExpired(s.expiresAt)) return s;
    return s.refreshToken && s.refreshExpiresAt && !isExpired(s.refreshExpiresAt) ? s : null;
  } catch {
    return null;
  }
}

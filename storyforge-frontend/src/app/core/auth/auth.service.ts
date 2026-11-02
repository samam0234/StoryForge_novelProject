import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, User } from '../models/models';

const STORAGE_KEY = 'storyforge.session';

interface Session {
  token: string;
  expiresAt: string;
  user: User;
}

/** 로그인 상태와 JWT 보관 (docs/plan/auth.md). 토큰 검증은 백엔드가 합니다. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly base = `${environment.apiBaseUrl}/auth`;

  private readonly session = signal<Session | null>(restore());
  readonly user = computed(() => this.session()?.user ?? null);
  readonly isLoggedIn = computed(() => this.session() !== null);

  /** 만료된 토큰은 돌려주지 않습니다. */
  token(): string | null {
    const s = this.session();
    if (!s) return null;
    if (isExpired(s)) {
      this.clear();
      return null;
    }
    return s.token;
  }

  login(body: { email: string; password: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/login`, body).pipe(tap((r) => this.store(r)));
  }

  register(body: { email: string; password: string; displayName: string }): Observable<AuthResponse> {
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
    this.clear();
    this.router.navigate(['/login']);
  }

  /** 서버가 401을 돌려줬을 때: 세션을 지우고 돌아올 주소와 함께 로그인으로 보냅니다. */
  expire(): void {
    this.clear();
    this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
  }

  private store(r: AuthResponse): void {
    this.persist({ token: r.accessToken, expiresAt: r.expiresAt, user: r.user });
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

function isExpired(s: Session): boolean {
  return new Date(s.expiresAt).getTime() <= Date.now();
}

function restore(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const s = JSON.parse(raw) as Session;
    return s.token && s.user && !isExpired(s) ? s : null;
  } catch {
    return null;
  }
}

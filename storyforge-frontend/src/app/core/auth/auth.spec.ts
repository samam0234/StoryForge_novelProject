import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { environment } from '../../../environments/environment';
import { authGuard, guestGuard } from './auth.guard';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

const API = environment.apiBaseUrl;
const future = () => new Date(Date.now() + 3_600_000).toISOString();
const response = (expiresAt = future()) => ({
  accessToken: 'token-1',
  tokenType: 'Bearer',
  expiresAt,
  user: { id: 1, email: 'a@b.c', displayName: '가' },
});

function setup() {
  TestBed.configureTestingModule({
    providers: [provideRouter([]), provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()],
  });
  return { auth: TestBed.inject(AuthService), http: TestBed.inject(HttpClient), ctrl: TestBed.inject(HttpTestingController) };
}

describe('AuthService', () => {
  beforeEach(() => localStorage.clear());

  it('로그인하면 세션을 저장하고 새 인스턴스에서 복원한다', () => {
    const { auth, ctrl } = setup();
    auth.login({ email: 'a@b.c', password: 'password1' }).subscribe();
    ctrl.expectOne(`${API}/auth/login`).flush(response());
    expect(auth.token()).toBe('token-1');
    expect(auth.user()?.displayName).toBe('가');
    expect(JSON.parse(localStorage.getItem('storyforge.session')!).token).toBe('token-1');

    TestBed.resetTestingModule();
    expect(setup().auth.token()).toBe('token-1');
  });

  it('만료된 세션은 복원하지 않는다', () => {
    localStorage.setItem('storyforge.session', JSON.stringify({ token: 'old', expiresAt: '2000-01-01T00:00:00Z', user: { id: 1 } }));
    const { auth } = setup();
    expect(auth.isLoggedIn()).toBe(false);
    expect(auth.token()).toBeNull();
  });

  it('깨진 저장값은 무시한다', () => {
    localStorage.setItem('storyforge.session', '{broken');
    expect(setup().auth.isLoggedIn()).toBe(false);
  });

  it('로그아웃하면 세션을 지우고 로그인으로 이동한다', () => {
    const { auth, ctrl } = setup();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    auth.login({ email: 'a@b.c', password: 'password1' }).subscribe();
    ctrl.expectOne(`${API}/auth/login`).flush(response());
    auth.logout();
    expect(auth.token()).toBeNull();
    expect(localStorage.getItem('storyforge.session')).toBeNull();
    expect(navigate).toHaveBeenCalledWith(['/login']);
  });
});

describe('authInterceptor', () => {
  beforeEach(() => localStorage.clear());

  function loggedIn() {
    const s = setup();
    s.auth.login({ email: 'a@b.c', password: 'password1' }).subscribe();
    s.ctrl.expectOne(`${API}/auth/login`).flush(response());
    return s;
  }

  it('API 요청에 토큰을 붙인다', () => {
    const { http, ctrl } = loggedIn();
    http.get(`${API}/projects`).subscribe();
    expect(ctrl.expectOne(`${API}/projects`).request.headers.get('Authorization')).toBe('Bearer token-1');
  });

  it('API가 아닌 요청에는 토큰을 붙이지 않는다', () => {
    const { http, ctrl } = loggedIn();
    http.get('https://example.com/x').subscribe();
    expect(ctrl.expectOne('https://example.com/x').request.headers.has('Authorization')).toBe(false);
  });

  it('401이면 세션을 만료시킨다', () => {
    const { auth, http, ctrl } = loggedIn();
    const expire = vi.spyOn(auth, 'expire').mockImplementation(() => undefined);
    http.get(`${API}/projects`).subscribe({ error: () => undefined });
    ctrl.expectOne(`${API}/projects`).flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(expire).toHaveBeenCalled();
  });

  it('로그인 실패 401은 세션 만료로 처리하지 않는다', () => {
    const { auth, ctrl } = setup();
    const expire = vi.spyOn(auth, 'expire').mockImplementation(() => undefined);
    auth.login({ email: 'a@b.c', password: 'wrong' }).subscribe({ error: () => undefined });
    ctrl.expectOne(`${API}/auth/login`).flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(expire).not.toHaveBeenCalled();
  });
});

describe('guards', () => {
  beforeEach(() => localStorage.clear());

  const run = (guard: typeof authGuard, url = '/projects/1') =>
    TestBed.runInInjectionContext(() => guard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot));

  it('authGuard는 비로그인을 돌아올 주소와 함께 로그인으로 보낸다', () => {
    setup();
    const result = run(authGuard) as UrlTree;
    expect(result.toString()).toBe('/login?returnUrl=%2Fprojects%2F1');
  });

  it('authGuard는 로그인 사용자를 통과시킨다', () => {
    localStorage.setItem('storyforge.session', JSON.stringify({ ...sessionOf(), expiresAt: future() }));
    setup();
    expect(run(authGuard)).toBe(true);
  });

  it('guestGuard는 로그인 사용자를 대시보드로 보낸다', () => {
    localStorage.setItem('storyforge.session', JSON.stringify({ ...sessionOf(), expiresAt: future() }));
    setup();
    expect((run(guestGuard) as UrlTree).toString()).toBe('/');
  });
});

function sessionOf() {
  return { token: 't', user: { id: 1, email: 'a@b.c', displayName: '가' } };
}

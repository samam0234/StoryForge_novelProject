import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { vi } from 'vitest';
import { environment } from '../../../environments/environment';
import { adminGuard } from '../../core/auth/auth.guard';
import { AuthService } from '../../core/auth/auth.service';
import { NotificationBellComponent } from '../../layout/notification-bell.component';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { firstValueFrom, isObservable, Observable } from 'rxjs';

const API = environment.apiBaseUrl;

describe('adminGuard', () => {
  function setup() {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);
    const auth = TestBed.inject(AuthService);
    vi.spyOn(auth, 'token').mockReturnValue('t');
    return { http };
  }

  async function run(user: { admin: boolean }) {
    const { http } = setup();
    const result = TestBed.runInInjectionContext(() =>
      adminGuard({} as ActivatedRouteSnapshot, { url: '/admin' } as RouterStateSnapshot),
    );
    expect(isObservable(result)).toBe(true);
    const value = firstValueFrom(result as Observable<boolean | UrlTree>);
    http.expectOne(`${API}/auth/me`).flush({ id: 1, email: 'a', displayName: '가', ...user });
    return value;
  }

  it('관리자는 통과한다', async () => {
    expect(await run({ admin: true })).toBe(true);
  });

  it('관리자가 아니면 첫 화면으로 보낸다', async () => {
    const value = await run({ admin: false });
    expect(TestBed.inject(Router).serializeUrl(value as UrlTree)).toBe('/');
  });
});

describe('NotificationBellComponent', () => {
  function setup() {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(NotificationBellComponent);
    return { http, fixture, instance: fixture.componentInstance as unknown as { open(n: unknown): void } };
  }

  const note = (link: string | null, read = false) => ({ id: 7, type: 'COMMENT', message: 'm', link, read, createdAt: '2026-01-01T00:00:00Z' });

  it('안 읽은 수를 읽어 표시한다', async () => {
    const { http, fixture } = setup();
    http.expectOne(`${API}/me/notifications/unread-count`).flush({ count: 3 });
    fixture.detectChanges();
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).querySelector('button')?.getAttribute('aria-label')).toBe('알림 3건 안 읽음');
    http.verify();
    fixture.destroy();
  });

  it('앱 안 경로만 따라가고 외부 주소는 무시한다', () => {
    const { http, fixture, instance } = setup();
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    http.expectOne(`${API}/me/notifications/unread-count`).flush({ count: 0 });

    instance.open(note('/works/1', true));
    expect(navigate).toHaveBeenCalledWith('/works/1');
    navigate.mockClear();
    instance.open(note('https://evil.example', true));
    expect(navigate).not.toHaveBeenCalled();
    fixture.destroy();
  });

  it('읽지 않은 알림을 열면 읽음으로 표시한다', () => {
    const { http, fixture, instance } = setup();
    vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    http.expectOne(`${API}/me/notifications/unread-count`).flush({ count: 1 });
    instance.open(note(null));
    expect(http.expectOne(`${API}/me/notifications/7/read`).request.method).toBe('PUT');
    fixture.destroy();
  });
});

import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { AccountMailPage } from './account-mail.page';
import { authInterceptor } from '../../core/auth/auth.interceptor';
import { environment } from '../../../environments/environment';

describe('AccountMailPage', () => {
  const base = `${environment.apiBaseUrl}/auth/mail`;
  function setup(fragment = '') {
    localStorage.setItem('storyforge.session', JSON.stringify({ token: 'revoked', expiresAt: '2099-01-01', user: { id: 1 } }));
    TestBed.configureTestingModule({
      imports: [AccountMailPage],
      providers: [provideRouter([]), provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { snapshot: { fragment } } }],
    });
    const fixture = TestBed.createComponent(AccountMailPage);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne(`${base}/status`).flush({ enabled: true });
    return { page: fixture.componentInstance, fixture, http };
  }
  afterEach(() => localStorage.clear());
  it('reset requires matching passwords, omits old JWT and clears the session on success', () => {
    const token = 'a'.repeat(43);
    const { page, http } = setup(`reset:${token}`);
    page.password = 'new-password'; page.confirmation = 'different';
    expect(page.canSubmit()).toBe(false);
    page.confirmation = page.password; page.submit();
    const request = http.expectOne(`${base}/reset`);
    expect(request.request.headers.has('Authorization')).toBe(false);
    expect(request.request.body.token).toBe(token);
    request.flush(null);
    expect(page.done()).toBe(true);
    expect(localStorage.getItem('storyforge.session')).toBeNull();
    expect(page.password).toBe(''); http.verify();
  });
  it('does not automatically consume a verification link and allows requesting a fresh link after failure', () => {
    const { page, http } = setup(`verify:${'b'.repeat(43)}`);
    http.expectNone(`${base}/verify`);
    page.submit();
    http.expectOne(`${base}/verify`).flush({ detail: '만료된 링크' }, { status: 409, statusText: 'Conflict' });
    expect(page.error()).toContain('만료');
    page.newRequest(); expect(page.mode).toBe('request'); expect(page.done()).toBe(false);
    page.email = 'user@example.test'; page.submit();
    http.expectOne(`${base}/reset-request`).flush({ message: '메일함을 확인하세요.' });
    expect(page.message()).toBe('메일함을 확인하세요.'); http.verify();
  });
  it('rejects malformed links without consuming them', () => {
    const { page, http } = setup('reset:bad');
    expect(page.done()).toBe(true); expect(page.error()).toContain('올바르지'); http.verify();
  });
});

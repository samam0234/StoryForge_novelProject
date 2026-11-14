import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

/** API 요청에 토큰을 붙이고, 401이면 로그인 화면으로 보냅니다. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  if (!req.url.startsWith(environment.apiBaseUrl)) return next(req);
  // 복구 링크는 기존 만료·폐기된 세션과 무관하게 사용할 수 있어야 합니다.
  if (req.url.startsWith(`${environment.apiBaseUrl}/auth/mail/`) && !req.url.endsWith('/verify-request')) return next(req);

  const token = auth.token();
  const request = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
  const isSessionCall = req.url.endsWith('/auth/login') || req.url.endsWith('/auth/register')
    || req.url.endsWith('/auth/refresh') || req.url.endsWith('/auth/logout');

  return next(request).pipe(
    catchError((e: unknown) => {
      if (!(e instanceof HttpErrorResponse) || e.status !== 401 || isSessionCall) return throwError(() => e);
      if (!auth.hasRefresh()) {
        auth.expire();
        return throwError(() => e);
      }
      return auth.refreshSession().pipe(
        switchMap(() => next(req.clone({ setHeaders: { Authorization: `Bearer ${auth.token() ?? ''}` } }))),
        catchError((refreshError: unknown) => {
          auth.expire();
          return throwError(() => refreshError);
        }),
      );
    }),
  );
};

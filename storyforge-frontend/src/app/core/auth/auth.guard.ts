import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of, switchMap } from 'rxjs';
import { AuthService } from './auth.service';

/** 로그인이 필요한 화면 */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const login = router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  if (auth.token()) return true;
  if (!auth.hasRefresh()) return login;
  return auth.refreshSession().pipe(map(() => true), catchError(() => of(login)));
};

/** 관리자 화면. 서버의 최신 정보로 판단하며, 실제 권한 검사는 백엔드가 합니다. */
export const adminGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const login = router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  const check = () => auth.refreshUser().pipe(
    map((u) => (u.admin ? true : router.createUrlTree(['/']))),
    catchError(() => of(router.createUrlTree(['/']))),
  );
  if (auth.token()) return check();
  if (!auth.hasRefresh()) return login;
  return auth.refreshSession().pipe(switchMap(() => check()), catchError(() => of(login)));
};

/** 로그인 화면: 이미 로그인했으면 대시보드로 */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.token()) return router.createUrlTree(['/']);
  if (!auth.hasRefresh()) return true;
  return auth.refreshSession().pipe(
    map(() => router.createUrlTree(['/'])),
    catchError(() => of(true)),
  );
};

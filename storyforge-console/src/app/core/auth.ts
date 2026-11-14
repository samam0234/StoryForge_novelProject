import {
  HttpClient,
  HttpErrorResponse,
  HttpInterceptorFn,
} from "@angular/common/http";
import { inject, Injectable, signal } from "@angular/core";
import { CanActivateFn, Router } from "@angular/router";
import { catchError, map, of, switchMap, tap, throwError } from "rxjs";
import { User } from "./api";

const KEY = "storyforge.console.token";
const REFRESH_KEY = "storyforge.console.refresh";
@Injectable({ providedIn: "root" })
export class Auth {
  private readonly http = inject(HttpClient);
  readonly user = signal<User | null>(null);
  private tokenValue: string | null = this.restore();
  private refreshValue: string | null = this.restoreRefresh();

  token() {
    return this.tokenValue;
  }
  hasRefresh() {
    return !!this.refreshValue;
  }
  login(email: string, password: string) {
    return this.http
      .post<{ accessToken: string; refreshToken?: string; user: User }>("/api/v1/auth/login", {
        email,
        password,
      })
      .pipe(
        switchMap((result) => {
          if (!result.user.admin)
            return throwError(
              () => new Error("운영자 권한이 있는 계정으로 로그인하세요."),
            );
          this.tokenValue = result.accessToken;
          this.refreshValue = result.refreshToken ?? null;
          return this.verify();
        }),
        tap(() => {
          try {
            sessionStorage.setItem(KEY, this.tokenValue!);
            if (this.refreshValue) sessionStorage.setItem(REFRESH_KEY, this.refreshValue);
          } catch {
            /* 메모리 세션 유지 */
          }
        }),
        catchError((error) => {
          this.clear();
          return throwError(() => error);
        }),
      );
  }
  verify() {
    return this.http.get<User>("/api/v1/auth/me").pipe(
      map((user) => {
        if (!user.admin) {
          this.clear();
          throw new Error("운영자 권한이 필요합니다.");
        }
        this.user.set(user);
        return user;
      }),
    );
  }
  clear() {
    this.tokenValue = null;
    this.refreshValue = null;
    this.user.set(null);
    try {
      sessionStorage.removeItem(KEY);
      sessionStorage.removeItem(REFRESH_KEY);
    } catch {
      /* 무시 */
    }
  }
  refresh() {
    const refreshToken = this.refreshValue;
    if (!refreshToken) return throwError(() => new Error("갱신 토큰이 없습니다."));
    return this.http
      .post<{ accessToken: string; refreshToken?: string }>("/api/v1/auth/refresh", { refreshToken })
      .pipe(
        tap((result) => {
          this.tokenValue = result.accessToken;
          this.refreshValue = result.refreshToken ?? null;
          try {
            sessionStorage.setItem(KEY, result.accessToken);
            if (result.refreshToken) sessionStorage.setItem(REFRESH_KEY, result.refreshToken);
          } catch {
            /* 메모리 세션 유지 */
          }
        }),
      );
  }
  private restoreRefresh() {
    try {
      return sessionStorage.getItem(REFRESH_KEY);
    } catch {
      return null;
    }
  }
  private restore() {
    try {
      return sessionStorage.getItem(KEY);
    } catch {
      return null;
    }
  }
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(Auth);
  const router = inject(Router);
  const internal = req.url.startsWith("/api/v1/");
  const token = auth.token();
  return next(
    internal && token
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req,
  ).pipe(
    catchError((error) => {
      if (
        internal &&
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        token &&
        !req.url.endsWith("/auth/refresh")
      ) {
        if (!auth.hasRefresh()) {
          auth.clear();
          void router.navigateByUrl("/login");
          return throwError(() => error);
        }
        return auth.refresh().pipe(
          switchMap(() =>
            next(
              req.clone({
                setHeaders: { Authorization: `Bearer ${auth.token()}` },
              }),
            ),
          ),
          catchError((refreshError) => {
            auth.clear();
            void router.navigateByUrl("/login");
            return throwError(() => refreshError);
          }),
        );
      }
      return throwError(() => error);
    }),
  );
};

export const adminGuard: CanActivateFn = () => {
  const auth = inject(Auth);
  const router = inject(Router);
  if (!auth.token()) return router.createUrlTree(["/login"]);
  return auth.verify().pipe(
    map(() => true),
    catchError(() => {
      auth.clear();
      return of(router.createUrlTree(["/login"]));
    }),
  );
};

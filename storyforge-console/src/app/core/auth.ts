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
@Injectable({ providedIn: "root" })
export class Auth {
  private readonly http = inject(HttpClient);
  readonly user = signal<User | null>(null);
  private tokenValue: string | null = this.restore();

  token() {
    return this.tokenValue;
  }
  login(email: string, password: string) {
    return this.http
      .post<{ accessToken: string; user: User }>("/api/v1/auth/login", {
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
          return this.verify();
        }),
        tap(() => {
          try {
            sessionStorage.setItem(KEY, this.tokenValue!);
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
    this.user.set(null);
    try {
      sessionStorage.removeItem(KEY);
    } catch {
      /* 무시 */
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
        token
      ) {
        auth.clear();
        void router.navigateByUrl("/login");
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

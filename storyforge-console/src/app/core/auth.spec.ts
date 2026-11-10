import {
  HttpClient,
  provideHttpClient,
  withInterceptors,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { Router } from "@angular/router";
import { isObservable } from "rxjs";
import { Auth, adminGuard, authInterceptor } from "./auth";

describe("운영자 인증", () => {
  let http: HttpTestingController;
  let auth: Auth;
  const router = {
    navigateByUrl: vi.fn(),
    createUrlTree: vi.fn(() => "/login"),
  };
  beforeEach(() => {
    sessionStorage.clear();
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: router },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(Auth);
  });
  afterEach(() => {
    http.verify();
    sessionStorage.clear();
  });
  const admin = {
    id: 1,
    email: "admin@example.com",
    displayName: "운영자",
    admin: true,
  };

  function login() {
    auth.login(admin.email, "password").subscribe();
    http
      .expectOne("/api/v1/auth/login")
      .flush({ accessToken: "admin-token", user: admin });
    const verify = http.expectOne("/api/v1/auth/me");
    expect(verify.request.headers.get("Authorization")).toBe(
      "Bearer admin-token",
    );
    verify.flush(admin);
  }

  it("일반 회원은 세션을 저장하지 않는다", () => {
    let message = "";
    auth
      .login("member@example.com", "password")
      .subscribe({ error: (e) => (message = e.message) });
    http
      .expectOne("/api/v1/auth/login")
      .flush({ accessToken: "member-token", user: { ...admin, admin: false } });
    expect(message).toContain("운영자 권한");
    expect(auth.token()).toBeNull();
    expect(sessionStorage.length).toBe(0);
  });

  it("서버의 관리자 재확인 후에만 세션을 저장한다", () => {
    login();
    expect(auth.user()?.admin).toBe(true);
    expect(sessionStorage.getItem("storyforge.console.token")).toBe(
      "admin-token",
    );
  });

  it("외부 주소에 토큰을 보내지 않는다", () => {
    login();
    TestBed.inject(HttpClient).get("https://example.com/data").subscribe();
    const external = http.expectOne("https://example.com/data");
    expect(external.request.headers.has("Authorization")).toBe(false);
    external.flush({});
  });

  it("만료된 토큰은 지우고 로그인 화면으로 이동한다", () => {
    login();
    TestBed.inject(HttpClient)
      .get("/api/v1/admin/overview")
      .subscribe({ error: () => {} });
    http
      .expectOne("/api/v1/admin/overview")
      .flush(
        { detail: "로그인이 필요합니다." },
        { status: 401, statusText: "Unauthorized" },
      );
    expect(auth.token()).toBeNull();
    expect(router.navigateByUrl).toHaveBeenCalledWith("/login");
  });

  it("이동 시 서버에서 권한을 다시 확인한다", () => {
    login();
    const result = TestBed.runInInjectionContext(() =>
      adminGuard({} as never, {} as never),
    );
    expect(isObservable(result)).toBe(true);
    if (isObservable(result)) result.subscribe();
    http.expectOne("/api/v1/auth/me").flush({ ...admin, admin: false });
    expect(auth.token()).toBeNull();
    expect(router.createUrlTree).toHaveBeenCalledWith(["/login"]);
  });
});

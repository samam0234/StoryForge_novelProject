import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { TestBed } from "@angular/core/testing";
import { ReportsPage } from "./reports";

describe("신고 일괄 처리", () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ReportsPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  function create() {
    const fixture = TestBed.createComponent(ReportsPage);
    http
      .expectOne((r) => r.url === "/api/v1/admin/reports")
      .flush({ items: [], page: 0, size: 20, total: 0 });
    fixture.detectChanges();
    return fixture;
  }

  it("조치 성공 후 선택을 지우고 새 목록을 조회한다", () => {
    const fixture = create();
    const page = fixture.componentInstance;
    page.toggle(4);
    page.toggle(9);
    page.note = " 검토 근거 ";
    page.action = "DISMISS";
    page.submit();
    const action = http.expectOne("/api/v1/admin/reports/batch");
    expect(action.request.body).toEqual({
      reportIds: [4, 9],
      action: "DISMISS",
      note: "검토 근거",
    });
    expect(page.saving()).toBe(true);
    action.flush({ requested: 2, processed: 2 });
    http
      .expectOne((r) => r.url === "/api/v1/admin/reports")
      .flush({ items: [], page: 0, size: 20, total: 0 });
    expect(page.selected()).toEqual([]);
    expect(page.message()).toContain("조치 완료");
  });

  it("충돌은 오류로 표시하고 성공으로 알리지 않는다", () => {
    const page = create().componentInstance;
    page.toggle(4);
    page.note = "검토";
    page.submit();
    http
      .expectOne("/api/v1/admin/reports/batch")
      .flush(
        { detail: "이미 처리된 신고입니다." },
        { status: 409, statusText: "Conflict" },
      );
    expect(page.error()).toBe("이미 처리된 신고입니다.");
    expect(page.message()).toBe("");
    expect(page.saving()).toBe(false);
  });

  it("선택이나 사유가 없으면 조치를 보내지 않는다", () => {
    const page = create().componentInstance;
    page.submit();
    page.toggle(4);
    page.note = " ";
    page.submit();
    http.expectNone("/api/v1/admin/reports/batch");
  });
});

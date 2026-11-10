import { DatePipe, KeyValuePipe } from "@angular/common";
import { Component, DestroyRef, inject, signal } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { RouterLink } from "@angular/router";
import { ConsoleApi, errorMessage, Overview } from "../core/api";

const LABELS: Record<string, string> = {
  BIBLE_DRAFT: "바이블 작성",
  BIBLE_CONFIRMED: "바이블 확정",
  OUTLINE_DRAFT: "아웃라인 작성",
  OUTLINE_CONFIRMED: "아웃라인 확정",
  CHAPTER_GENERATING: "챕터 생성 중",
  CHAPTER_REVIEW: "챕터 검토",
  COMPLETED: "완료",
};
@Component({
  selector: "sf-overview",
  imports: [DatePipe, KeyValuePipe, RouterLink],
  template: ` <div class="page-heading">
      <div>
        <span class="eyebrow">OVERVIEW</span>
        <h1>운영 현황</h1>
        <p>창작과 커뮤니티의 현재 흐름을 한눈에 살펴보세요.</p>
      </div>
      <button class="quiet" [disabled]="busy()" (click)="load()">
        ↻ 새로고침
      </button>
    </div>
    @if (error()) {
      <p class="error" role="alert">{{ error() }}</p>
    }
    @if (busy()) {
      <p class="muted" role="status">운영 현황을 불러오는 중입니다…</p>
    }
    @if (data(); as d) {
      <div class="metrics">
        <article>
          <span>전체 회원</span><strong>{{ d.users }}</strong
          ><small>이용 정지 {{ d.suspendedUsers }}명</small>
        </article>
        <article>
          <span>창작 프로젝트</span><strong>{{ d.projects }}</strong
          ><small>전체 파이프라인 기준</small>
        </article>
        <article>
          <span>게시 작품</span><strong>{{ d.works }}</strong
          ><small>숨김 작품 포함</small>
        </article>
        <article class="attention">
          <span>처리 대기 신고</span><strong>{{ d.openReports }}</strong
          ><a routerLink="/reports">신고 검토하기 ↗</a>
        </article>
      </div>
      <div class="overview-grid">
        <section class="panel">
          <div class="section-heading">
            <div>
              <h2>창작 파이프라인</h2>
              <p>프로젝트 단계별 분포</p>
            </div>
            <span class="badge">{{ d.projects }} PROJECTS</span>
          </div>
          @for (item of d.pipeline | keyvalue; track item.key) {
            <div class="pipeline-row">
              <span>{{ label(item.key) }}</span>
              <div class="bar">
                <i
                  [style.width.%]="
                    d.projects ? (item.value / d.projects) * 100 : 0
                  "
                ></i>
              </div>
              <b>{{ item.value }}</b>
            </div>
          }
          @if (!d.projects) {
            <div class="empty">아직 생성된 프로젝트가 없습니다.</div>
          }
        </section>
        <section class="panel">
          <span class="eyebrow">DAILY OPERATIONS</span>
          <h2>운영 바로가기</h2>
          <a class="quick-link" routerLink="/reports"
            ><span
              ><b>신고 검토</b
              ><small>대기 신고를 확인하고 조치하세요.</small></span
            >↗</a
          ><a class="quick-link" routerLink="/users"
            ><span
              ><b>회원 관리</b
              ><small>계정 상태와 이용 정지를 관리하세요.</small></span
            >↗</a
          ><a class="quick-link" routerLink="/audit"
            ><span
              ><b>감사 기록</b
              ><small>운영 조치와 사유를 확인하세요.</small></span
            >↗</a
          >
        </section>
      </div>
      <p class="updated">
        마지막 조회 {{ d.capturedAt | date: "yyyy.MM.dd HH:mm:ss" }} · 새로고침
        시 갱신
      </p>
    }`,
})
export class OverviewPage {
  private readonly api = inject(ConsoleApi);
  private readonly destroy = inject(DestroyRef);
  readonly busy = signal(false);
  readonly error = signal("");
  readonly data = signal<Overview | null>(null);
  constructor() {
    this.load();
  }
  label(key: string) {
    return LABELS[key] ?? key;
  }
  load() {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set("");
    this.api
      .overview()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (d) => {
          this.data.set(d);
          this.busy.set(false);
        },
        error: (e) => {
          this.data.set(null);
          this.error.set(errorMessage(e));
          this.busy.set(false);
        },
      });
  }
}

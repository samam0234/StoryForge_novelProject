import { DatePipe } from "@angular/common";
import { Component, DestroyRef, inject, signal } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormsModule } from "@angular/forms";
import { finalize } from "rxjs";
import { Action, ConsoleApi, errorMessage, Page, Report } from "../core/api";
import { Pager } from "../shared/pager";

@Component({
  selector: "sf-reports",
  imports: [FormsModule, DatePipe, Pager],
  template: ` <div class="page-heading">
      <div>
        <span class="eyebrow">MODERATION</span>
        <h1>신고 관리</h1>
        <p>신고 내용을 검토하고, 근거와 함께 조치를 남기세요.</p>
      </div>
      <span class="badge">{{ data()?.total ?? 0 }} REPORTS</span>
    </div>
    <div class="toolbar">
      <label
        >처리 상태<select
          [(ngModel)]="status"
          (ngModelChange)="load(0)"
          [disabled]="busy() || saving()"
        >
          <option value="OPEN">처리 대기</option>
          <option value="RESOLVED">처리 완료</option>
          <option value="DISMISSED">기각</option>
        </select></label
      ><button
        class="quiet"
        (click)="load(page)"
        [disabled]="busy() || saving()"
      >
        ↻ 새로고침</button
      ><span class="muted">같은 대상의 대기 신고는 함께 처리됩니다.</span>
    </div>
    @if (error()) {
      <p class="error" role="alert">{{ error() }}</p>
    }
    @if (message()) {
      <p class="success" role="status">{{ message() }}</p>
    }
    @if (busy()) {
      <p role="status" class="muted">신고 목록을 불러오는 중입니다…</p>
    }
    <div class="report-list">
      @for (r of data()?.items ?? []; track r.id) {
        <article
          class="panel report"
          [class.selected]="selected().includes(r.id)"
        >
          <div class="report-top">
            <label class="check-label"
              ><input
                type="checkbox"
                [checked]="selected().includes(r.id)"
                (change)="toggle(r.id)"
                [disabled]="saving()"
                [attr.aria-label]="'신고 ' + r.id + ' 선택'"
              /><span class="badge">{{ r.targetType }} #{{ r.targetId }}</span
              ><span class="reason">{{ reason(r.reason) }}</span></label
            ><time>{{ r.createdAt | date: "yyyy.MM.dd HH:mm" }}</time>
          </div>
          <h2>{{ r.target.title }}</h2>
          <p class="excerpt">
            {{ r.target.excerpt || "미리보기 내용이 없습니다." }}
          </p>
          <div class="report-detail">
            <span>신고 내용</span>
            <p>{{ r.detail || "추가 설명 없음" }}</p>
          </div>
          <div class="report-meta">
            <span
              >작성자 {{ r.target.author?.displayName || "삭제된 계정" }}</span
            ><span>신고자 {{ r.reporter.displayName }}</span
            ><span>대기 신고 {{ r.openReports }}건</span>
            @if (r.target.hidden) {
              <span class="badge warn">숨김</span>
            }
            @if (r.target.authorSuspended) {
              <span class="badge warn">작성자 정지</span>
            }
          </div>
          @if (r.resolutionNote) {
            <p class="muted">처리 메모 · {{ r.resolutionNote }}</p>
          }
        </article>
      } @empty {
        @if (!busy() && !error()) {
          <div class="panel empty">해당 상태의 신고가 없습니다.</div>
        }
      }
    </div>
    <sf-pager
      [total]="data()?.total ?? 0"
      [page]="page"
      [busy]="busy() || saving()"
      (change)="load($event)"
    />
    @if (selected().length) {
      <form class="panel decision" (ngSubmit)="submit()" #form="ngForm">
        <div class="section-heading">
          <h2>{{ selected().length }}건 선택</h2>
          <button
            type="button"
            class="quiet"
            (click)="selected.set([])"
            [disabled]="saving()"
          >
            선택 취소
          </button>
        </div>
        <div class="decision-fields">
          <label
            >조치<select
              name="action"
              [(ngModel)]="action"
              [disabled]="saving()"
            >
              <option value="DISMISS">기각</option>
              <option value="HIDE">콘텐츠 숨김</option>
              <option value="RESTORE">콘텐츠 복원</option>
              <option value="SUSPEND">작성자 정지</option>
              <option value="UNSUSPEND">작성자 정지 해제</option>
            </select></label
          ><label class="grow"
            >조치 사유<input
              name="note"
              [(ngModel)]="note"
              required
              maxlength="300"
              placeholder="판단 근거를 입력하세요 (필수)"
              [disabled]="saving()"
          /></label>
        </div>
        <p class="muted">
          선택한 신고에 같은 조치를 적용합니다. 하나라도 실패하면 전체를
          되돌립니다. 정지·숨김 사유는 대상자에게 전달됩니다.
        </p>
        <button
          class="primary"
          [disabled]="form.invalid || !note.trim() || saving()"
        >
          {{ saving() ? "조치 처리 중…" : "선택한 신고에 조치 적용" }}
        </button>
      </form>
    }`,
})
export class ReportsPage {
  private readonly api = inject(ConsoleApi);
  private readonly destroy = inject(DestroyRef);
  status = "OPEN";
  page = 0;
  action: Action = "DISMISS";
  note = "";
  readonly data = signal<Page<Report> | null>(null);
  readonly selected = signal<number[]>([]);
  readonly busy = signal(false);
  readonly saving = signal(false);
  readonly error = signal("");
  readonly message = signal("");
  constructor() {
    this.load(0);
  }
  reason(value: string) {
    return (
      (
        {
          SPAM: "도배·광고",
          ABUSE: "욕설·괴롭힘",
          SEXUAL: "부적절한 성적 내용",
          VIOLENCE: "불법·폭력 조장",
          COPYRIGHT: "저작권 침해",
          PERSONAL_INFO: "개인정보 노출",
          OTHER: "기타",
        } as Record<string, string>
      )[value] ?? value
    );
  }
  toggle(id: number) {
    this.selected.update((ids) =>
      ids.includes(id) ? ids.filter((x) => x !== id) : [...ids, id],
    );
  }
  load(page: number) {
    if (this.busy() || this.saving()) return;
    this.page = page;
    this.selected.set([]);
    this.data.set(null);
    this.busy.set(true);
    this.error.set("");
    this.api
      .reports(this.status, page)
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.busy.set(false)),
      )
      .subscribe({
        next: (d) => this.data.set(d),
        error: (e) => this.error.set(errorMessage(e)),
      });
  }
  submit() {
    if (this.saving() || !this.selected().length || !this.note.trim()) return;
    this.saving.set(true);
    this.error.set("");
    this.message.set("");
    this.api
      .decide(this.selected(), this.action, this.note.trim())
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (r) => {
          this.saving.set(false);
          this.note = "";
          this.message.set(
            r.requested + "건 검토 · 대상 " + r.processed + "개 조치 완료",
          );
          this.load(this.page);
        },
        error: (e) => {
          this.saving.set(false);
          this.error.set(errorMessage(e));
        },
      });
  }
}

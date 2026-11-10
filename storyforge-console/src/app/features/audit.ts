import { DatePipe } from "@angular/common";
import { Component, DestroyRef, inject, signal } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormsModule } from "@angular/forms";
import { finalize } from "rxjs";
import { Audit, ConsoleApi, errorMessage, Page } from "../core/api";
import { Pager } from "../shared/pager";

@Component({
  selector: "sf-audit",
  imports: [FormsModule, DatePipe, Pager],
  template: ` <div class="page-heading">
      <div>
        <span class="eyebrow">AUDIT TRAIL</span>
        <h1>감사 기록</h1>
        <p>누가, 언제, 어떤 근거로 조치했는지 확인하세요.</p>
      </div>
      <span class="badge">읽기 전용</span>
    </div>
    <form class="toolbar" (ngSubmit)="load(0)" #form="ngForm">
      <label
        >운영자 ID<input
          type="number"
          name="actor"
          [(ngModel)]="actorId"
          min="1"
          step="1"
          placeholder="전체 운영자"
          [disabled]="busy()" /></label
      ><button class="primary" [disabled]="busy() || form.invalid">조회</button
      ><span class="muted"
        >신고 조치와 회원 상태 변경의 이력이 표시됩니다.</span
      >
    </form>
    @if (error()) {
      <p class="error" role="alert">{{ error() }}</p>
    }
    @if (busy()) {
      <p class="muted" role="status">감사 기록을 불러오는 중입니다…</p>
    }
    <section class="panel table-panel">
      <div class="table-scroll">
        <table>
          <thead>
            <tr>
              <th>처리 시각</th>
              <th>운영자</th>
              <th>조치</th>
              <th>대상</th>
              <th>사유</th>
            </tr>
          </thead>
          <tbody>
            @for (a of data()?.items ?? []; track a.id) {
              <tr>
                <td>
                  {{ a.createdAt | date: "yyyy.MM.dd HH:mm:ss"
                  }}<small>#{{ a.id }}</small>
                </td>
                <td>
                  {{ a.actorName }}<small>운영자 #{{ a.actorId }}</small>
                </td>
                <td>
                  <span class="badge">{{ actionLabel(a.action) }}</span>
                </td>
                <td>
                  {{ a.targetType }} #{{ a.targetId }}
                  @if (a.reportId) {
                    <small>신고 #{{ a.reportId }}</small>
                  }
                </td>
                <td class="audit-note">{{ a.note || "메모 없음" }}</td>
              </tr>
            } @empty {
              <tr>
                <td colspan="5" class="empty">
                  {{ busy() ? "불러오는 중…" : "감사 기록이 없습니다." }}
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
      <sf-pager
        [total]="data()?.total ?? 0"
        [page]="page"
        [busy]="busy()"
        (change)="load($event)"
      />
    </section>`,
})
export class AuditPage {
  private readonly api = inject(ConsoleApi);
  private readonly destroy = inject(DestroyRef);
  actorId: number | null = null;
  page = 0;
  private appliedActor = "";
  readonly data = signal<Page<Audit> | null>(null);
  readonly busy = signal(false);
  readonly error = signal("");
  constructor() {
    this.load(0);
  }
  actionLabel(action: string) {
    return (
      (
        {
          HIDE: "숨김",
          RESTORE: "복원",
          DISMISS: "기각",
          SUSPEND: "정지",
          UNSUSPEND: "정지 해제",
        } as Record<string, string>
      )[action] ?? action
    );
  }
  load(page: number) {
    if (this.busy()) return;
    if (page === 0)
      this.appliedActor = this.actorId == null ? "" : String(this.actorId);
    this.page = page;
    this.busy.set(true);
    this.data.set(null);
    this.error.set("");
    this.api
      .audit(page, this.appliedActor)
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.busy.set(false)),
      )
      .subscribe({
        next: (d) => this.data.set(d),
        error: (e) => this.error.set(errorMessage(e)),
      });
  }
}

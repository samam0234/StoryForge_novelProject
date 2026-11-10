import { DatePipe } from "@angular/common";
import { Component, DestroyRef, inject, signal } from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormsModule } from "@angular/forms";
import { finalize } from "rxjs";
import { ConsoleApi, errorMessage, Member, Page } from "../core/api";
import { Pager } from "../shared/pager";

@Component({
  selector: "sf-users",
  imports: [FormsModule, DatePipe, Pager],
  template: ` <div class="page-heading">
      <div>
        <span class="eyebrow">MEMBERS</span>
        <h1>회원 관리</h1>
        <p>회원의 이용 상태를 살피고 필요한 조치를 관리하세요.</p>
      </div>
    </div>
    <form class="toolbar" (ngSubmit)="load(0)">
      <label class="grow"
        >회원 검색<input
          name="q"
          [(ngModel)]="query"
          maxlength="200"
          placeholder="이름 또는 이메일로 검색"
          [disabled]="busy() || saving()" /></label
      ><label
        >이용 상태<select
          name="status"
          [(ngModel)]="status"
          [disabled]="busy() || saving()"
        >
          <option value="">전체</option>
          <option value="false">정상</option>
          <option value="true">정지</option>
        </select></label
      ><button class="primary" [disabled]="busy() || saving()">검색</button>
    </form>
    @if (error()) {
      <p class="error" role="alert">{{ error() }}</p>
    }
    @if (message()) {
      <p class="success" role="status">{{ message() }}</p>
    }
    @if (busy()) {
      <p class="muted" role="status">회원 목록을 불러오는 중입니다…</p>
    }
    <section class="panel table-panel">
      <div class="table-scroll">
        <table>
          <thead>
            <tr>
              <th>회원</th>
              <th>이메일</th>
              <th>상태</th>
              <th>가입일</th>
              <th>조치</th>
            </tr>
          </thead>
          <tbody>
            @for (u of data()?.items ?? []; track u.id) {
              <tr>
                <td>
                  <b>{{ u.displayName }}</b
                  ><small
                    >#{{ u.id }}
                    @if (u.admin) {
                      · 운영자
                    }
                  </small>
                </td>
                <td>{{ u.email }}</td>
                <td>
                  <span class="badge" [class.warn]="u.suspended">{{
                    u.suspended ? "이용 정지" : "정상"
                  }}</span>
                  @if (u.suspendedReason) {
                    <small>{{ u.suspendedReason }}</small>
                  }
                </td>
                <td>{{ u.createdAt | date: "yyyy.MM.dd" }}</td>
                <td>
                  <button
                    class="quiet"
                    [disabled]="u.admin || saving()"
                    (click)="target.set(u); note = ''"
                  >
                    {{ u.suspended ? "정지 해제" : "이용 정지" }}
                  </button>
                </td>
              </tr>
            } @empty {
              <tr>
                <td colspan="5" class="empty">
                  {{ busy() ? "불러오는 중…" : "검색 결과가 없습니다." }}
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
      <sf-pager
        [total]="data()?.total ?? 0"
        [page]="page"
        [busy]="busy() || saving()"
        (change)="load($event)"
      />
    </section>
    @if (target(); as u) {
      <form class="panel decision" (ngSubmit)="submit()" #form="ngForm">
        <h2>
          {{ u.displayName }} · {{ u.suspended ? "정지 해제" : "이용 정지" }}
        </h2>
        <p class="muted">
          {{
            u.suspended
              ? "다시 로그인하고 글을 쓸 수 있습니다."
              : "로그인과 쓰기가 차단됩니다. 기존 게시글은 유지됩니다."
          }}
          사유는 대상자에게 전달됩니다.
        </p>
        <label
          >조치 사유<textarea
            name="note"
            [(ngModel)]="note"
            required
            maxlength="300"
            rows="3"
            [disabled]="saving()"
          ></textarea>
        </label>
        <div class="actions">
          <button
            type="button"
            class="quiet"
            (click)="target.set(null)"
            [disabled]="saving()"
          >
            취소</button
          ><button
            class="primary"
            [disabled]="form.invalid || !note.trim() || saving()"
          >
            {{ saving() ? "처리 중…" : "조치 확정" }}
          </button>
        </div>
      </form>
    }`,
})
export class UsersPage {
  private readonly api = inject(ConsoleApi);
  private readonly destroy = inject(DestroyRef);
  query = "";
  status = "";
  page = 0;
  note = "";
  private appliedQuery = "";
  private appliedStatus = "";
  readonly data = signal<Page<Member> | null>(null);
  readonly target = signal<Member | null>(null);
  readonly busy = signal(false);
  readonly saving = signal(false);
  readonly error = signal("");
  readonly message = signal("");
  constructor() {
    this.load(0);
  }
  load(page: number) {
    if (this.busy() || this.saving()) return;
    if (page === 0) {
      this.appliedQuery = this.query;
      this.appliedStatus = this.status;
    }
    this.page = page;
    this.target.set(null);
    this.data.set(null);
    this.busy.set(true);
    this.error.set("");
    this.api
      .users(this.appliedQuery, this.appliedStatus, page)
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
    const user = this.target();
    if (!user || this.saving() || !this.note.trim()) return;
    this.saving.set(true);
    this.error.set("");
    this.message.set("");
    this.api
      .suspend(user.id, !user.suspended, this.note.trim())
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.target.set(null);
          this.message.set("회원 상태를 변경했습니다.");
          this.load(this.page);
        },
        error: (e) => {
          this.saving.set(false);
          this.error.set(errorMessage(e));
        },
      });
  }
}

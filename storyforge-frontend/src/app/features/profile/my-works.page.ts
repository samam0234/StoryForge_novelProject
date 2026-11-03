import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { CommunityApi } from '../../core/api/community-api.service';
import { errorMessage } from '../../core/api/storyforge-api.service';
import { KIND_LABEL, PackageRequest, VISIBILITY_OPTIONS, Visibility, WorkCard, labelOf } from '../../core/models/community';

/** 내 작품 관리: 내가 게시한 작품과 패키지 편입 요청 */
@Component({
  selector: 'sf-my-works-page',
  imports: [DatePipe, DecimalPipe, RouterLink, MatButtonModule, MatIconModule, MatProgressBarModule],
  template: `
    <div class="sf-page">
      <header class="sf-row head">
        <div>
          <h1>내 작품 관리</h1>
          <p class="sf-muted">게시한 작품의 정보와 본문을 고칩니다. 쓰고 있는 원고는 '내 프로젝트'에 있습니다.</p>
        </div>
        <span class="sf-spacer"></span>
        <a mat-stroked-button routerLink="/package-forge"><mat-icon>hub</mat-icon> 패키지 만들기</a>
      </header>

      @if (requests().length) {
        <section class="requests" aria-label="패키지 편입 요청">
          <h2>패키지 편입 요청 {{ requests().length }}</h2>
          <ul>
            @for (r of requests(); track r.itemId) {
              <li class="sf-row">
                <span>
                  <a [routerLink]="['/users', r.requester.id]">{{ r.requester.displayName }}</a>님이 「<a [routerLink]="['/works', r.work.id]">{{ r.work.title }}</a>」을(를)
                  패키지 「<a [routerLink]="['/works', r.packageId]">{{ r.packageTitle }}</a>」에 넣고 싶어 합니다.
                </span>
                <span class="sf-spacer"></span>
                <button mat-flat-button (click)="decide(r, true)">승인</button>
                <button mat-button (click)="decide(r, false)">거절</button>
              </li>
            }
          </ul>
        </section>
      }

      @if (loading()) {
        <mat-progress-bar mode="indeterminate" />
      } @else if (error()) {
        <div class="sf-empty" role="alert">{{ error() }}</div>
      } @else if (!works().length) {
        <div class="sf-empty">
          아직 게시한 작품이 없습니다. 프로젝트의 '게시' 탭에서 승인된 챕터를 올릴 수 있습니다.
          <a routerLink="/">내 프로젝트로</a>
        </div>
      } @else {
        <div class="table-wrap">
          <table class="sf-table">
            <thead>
              <tr>
                <th scope="col">작품</th>
                <th scope="col">종류</th>
                <th scope="col">공개</th>
                <th scope="col">리비전</th>
                <th scope="col" class="num">조회</th>
                <th scope="col" class="num">추천</th>
                <th scope="col" class="num">Clone</th>
                <th scope="col">수정일</th>
                <th scope="col"><span class="sr">관리</span></th>
              </tr>
            </thead>
            <tbody>
              @for (w of works(); track w.id) {
                <tr>
                  <td class="title"><a [routerLink]="['/works', w.id]">{{ w.title }}</a></td>
                  <td>{{ kinds[w.kind] }}</td>
                  <td>{{ visibilityLabel(w.visibility) }}</td>
                  <td>r{{ w.revision }}</td>
                  <td class="num">{{ w.viewCount | number }}</td>
                  <td class="num">{{ w.likeCount | number }}</td>
                  <td class="num">{{ w.cloneCount | number }}</td>
                  <td class="date">{{ w.updatedAt | date: 'yyyy-MM-dd' }}</td>
                  <td class="row-actions">
                    @if (w.kind === 'PACKAGE') {
                      <a mat-button [routerLink]="['/package-forge', w.id]">편집</a>
                    } @else {
                      <a mat-button [routerLink]="['/me/works', w.id, 'edit']">수정</a>
                    }
                    <button mat-button (click)="remove(w)">삭제</button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `,
  styles: `
    h1 {
      margin: 0 0 4px;
      font-size: 24px;
    }
    h2 {
      margin: 0 0 8px;
      font-size: 15px;
    }
    .head {
      margin-bottom: 16px;
    }
    .requests {
      margin-bottom: 16px;
      padding: 12px 16px;
      border: 1px solid var(--sf-accent);
      border-radius: var(--sf-radius);
      background: var(--sf-surface);
    }
    .requests ul {
      list-style: none;
      margin: 0;
      padding: 0;
    }
    .table-wrap {
      overflow-x: auto;
    }
    .title a {
      font-weight: 600;
      text-decoration: none;
      color: var(--sf-emphasis);
    }
    .num {
      text-align: right;
    }
    .date {
      white-space: nowrap;
      color: var(--sf-text-muted);
    }
    .row-actions {
      white-space: nowrap;
    }
    .sr {
      position: absolute;
      width: 1px;
      height: 1px;
      overflow: hidden;
      clip-path: inset(50%);
    }
  `,
})
export class MyWorksPage {
  private readonly api = inject(CommunityApi);
  private readonly snack = inject(MatSnackBar);

  protected readonly kinds = KIND_LABEL;
  protected readonly works = signal<WorkCard[]>([]);
  protected readonly requests = signal<PackageRequest[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.api.myWorks().subscribe({
      next: (list) => {
        this.works.set(list);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(errorMessage(e));
        this.loading.set(false);
      },
    });
    this.api.packageRequests().subscribe((r) => this.requests.set(r));
  }

  protected visibilityLabel(v: Visibility): string {
    return labelOf(VISIBILITY_OPTIONS, v);
  }

  protected decide(r: PackageRequest, approve: boolean): void {
    this.api.decidePackageRequest(r.itemId, approve).subscribe({
      next: () => this.requests.update((list) => list.filter((x) => x.itemId !== r.itemId)),
      error: (e) => this.snack.open(errorMessage(e), '닫기', { duration: 4000 }),
    });
  }

  protected remove(w: WorkCard): void {
    if (!confirm(`"${w.title}" 게시를 삭제할까요? 추천, 댓글, 공유 기록이 함께 지워집니다. 프로젝트 원고는 남습니다.`)) return;
    this.api.deleteWork(w.id).subscribe({
      next: () => this.works.update((list) => list.filter((x) => x.id !== w.id)),
      error: (e) => this.snack.open(errorMessage(e), '닫기', { duration: 4000 }),
    });
  }
}

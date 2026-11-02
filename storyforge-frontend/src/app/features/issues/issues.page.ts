import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RouterLink } from '@angular/router';
import { CommunityApi } from '../../core/api/community-api.service';
import { errorMessage } from '../../core/api/storyforge-api.service';
import { IssueBoard } from '../../core/models/community';
import { UserChipComponent } from '../../shared/components/user-chip/user-chip.component';
import { WorkCardComponent } from '../../shared/components/work-card/work-card.component';

/** 이슈: 달성 축하, 최근 7일 인기작, 새 작품 */
@Component({
  selector: 'sf-issues-page',
  imports: [DatePipe, RouterLink, MatIconModule, MatProgressBarModule, UserChipComponent, WorkCardComponent],
  template: `
    <div class="sf-page">
      <h1>이슈</h1>
      <p class="sf-muted">최근 인기 작품과 작가들의 달성 소식입니다.</p>

      @if (error()) {
        <div class="sf-empty" role="alert">{{ error() }}</div>
      } @else if (!board()) {
        <mat-progress-bar mode="indeterminate" />
      } @else if (board(); as b) {
        <h2><mat-icon inline>celebration</mat-icon> 축하합니다</h2>
        @if (!b.celebrations.length) {
          <div class="sf-empty">아직 달성 소식이 없습니다. 하루 추천 100개, 누적 추천 100개 같은 단위를 넘으면 여기에 나옵니다.</div>
        } @else {
          <ul class="celebrations">
            @for (c of b.celebrations; track c.id) {
              <li>
                <mat-icon class="trophy" aria-hidden="true">emoji_events</mat-icon>
                <div class="body">
                  <a [routerLink]="['/works', c.work.id]"><strong>{{ c.work.title }}</strong></a>
                  <span>{{ c.message }}</span>
                  <div class="sf-row by">
                    <sf-user-chip [user]="c.work.author" />
                    <span class="sf-muted">{{ c.createdAt | date: 'yyyy-MM-dd HH:mm' }}</span>
                    <a [routerLink]="['/works', c.work.id]" class="cheer">축하 댓글 남기기</a>
                  </div>
                </div>
              </li>
            }
          </ul>
        }

        <h2><mat-icon inline>local_fire_department</mat-icon> 최근 7일 인기</h2>
        @if (!b.trending.length) {
          <div class="sf-empty">최근 7일 동안 반응이 있었던 작품이 아직 없습니다.</div>
        } @else {
          <div class="sf-card-grid">
            @for (w of b.trending; track w.id) {
              <sf-work-card [work]="w" />
            }
          </div>
        }

        <h2><mat-icon inline>new_releases</mat-icon> 새 작품</h2>
        @if (!b.fresh.length) {
          <div class="sf-empty">아직 게시된 작품이 없습니다.</div>
        } @else {
          <div class="sf-card-grid">
            @for (w of b.fresh; track w.id) {
              <sf-work-card [work]="w" />
            }
          </div>
        }
      }
    </div>
  `,
  styles: `
    h1 {
      margin: 0 0 4px;
      font-size: 24px;
    }
    h2 {
      font-size: 18px;
      margin: 28px 0 12px;
    }
    .celebrations {
      list-style: none;
      margin: 0;
      padding: 0;
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 12px;
    }
    .celebrations li {
      display: flex;
      gap: 12px;
      padding: 14px;
      border: 1px solid var(--sf-border);
      border-left: 4px solid var(--sf-accent);
      border-radius: var(--sf-radius);
      background: var(--sf-surface);
    }
    .trophy {
      color: var(--sf-accent);
      flex: none;
    }
    .body {
      display: flex;
      flex-direction: column;
      gap: 4px;
      min-width: 0;
    }
    .body a {
      color: var(--sf-emphasis);
    }
    .by {
      font-size: 12px;
    }
    .cheer {
      font-size: 12px;
    }
  `,
})
export class IssuesPage {
  private readonly api = inject(CommunityApi);
  protected readonly board = signal<IssueBoard | null>(null);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.api.issues().subscribe({
      next: (b) => this.board.set(b),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}

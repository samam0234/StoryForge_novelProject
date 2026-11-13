import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { KIND_LABEL, PACKAGE_TYPES, WorkCard, labelOf } from '../../../core/models/community';
import { UserChipComponent } from '../user-chip/user-chip.component';

/** 게시판, 프로필, 이슈에서 함께 쓰는 작품 카드 */
@Component({
  selector: 'sf-work-card',
  imports: [RouterLink, DatePipe, DecimalPipe, MatIconModule, UserChipComponent],
  template: `
    <article class="card">
      <a class="main" [routerLink]="['/works', work().id]">
        @if (work().coverUrl) {
          <img class="cover" [src]="work().coverUrl!" alt="" />
        } @else {
          <span class="cover-fallback" aria-hidden="true"></span>
        }
        <div class="sf-row meta">
          <span class="sf-badge">{{ kindLabel() }}</span>
          @if (work().genre) {
            <span class="sf-muted">{{ work().genre }}</span>
          }
          @if (work().parentWorkId || work().parentTitle) {
            <span class="sf-muted derived" [title]="'원작: ' + work().parentTitle"><mat-icon inline>call_split</mat-icon> 파생작</span>
          }
          @if (work().visibility !== 'PUBLIC') {
            <span class="sf-muted"><mat-icon inline>{{ work().visibility === 'PRIVATE' ? 'lock' : 'link' }}</mat-icon></span>
          }
        </div>
        <h3>{{ work().title }}</h3>
        @if (work().synopsis) {
          <p class="synopsis">{{ work().synopsis }}</p>
        }
      </a>
      @if (work().tags.length) {
        <div class="tags">
          @for (t of work().tags; track t) {
            <span>#{{ t }}</span>
          }
        </div>
      }
      <footer>
        <sf-user-chip [user]="work().author" />
        <span class="sf-spacer"></span>
        <span class="stat" title="분량">
          @if (work().kind === 'PACKAGE') {
            {{ work().chapterCount }}작품
          } @else {
            {{ work().chapterCount }}장
          }
        </span>
        <span class="stat" title="추천"><mat-icon inline>favorite</mat-icon> {{ work().likeCount | number }}</span>
        <span class="stat" title="Clone"><mat-icon inline>call_split</mat-icon> {{ work().cloneCount | number }}</span>
        <span class="stat" title="댓글"><mat-icon inline>chat_bubble</mat-icon> {{ work().commentCount | number }}</span>
      </footer>
      <p class="sf-muted date">{{ work().publishedAt | date: 'yyyy-MM-dd' }}</p>
    </article>
  `,
  styles: `
    .card {
      display: flex;
      flex-direction: column;
      gap: 8px;
      height: 100%;
      box-sizing: border-box;
      padding: 14px;
      border: 1px solid var(--sf-border);
      border-radius: var(--sf-radius);
      background:
        linear-gradient(180deg, color-mix(in srgb, var(--sf-accent) 10%, var(--sf-surface)), var(--sf-surface) 36%);
      box-shadow: var(--sf-shadow);
      transition: transform 0.18s ease, border-color 0.18s ease;
    }
    .card:hover {
      transform: translateY(-3px);
      border-color: var(--sf-secondary);
    }
    .main {
      text-decoration: none;
      flex: 1;
    }
    .cover,
    .cover-fallback {
      display: block;
      width: 100%;
      height: 148px;
      object-fit: cover;
      border-radius: 12px;
      margin-bottom: 10px;
    }
    .cover-fallback {
      background:
        radial-gradient(120px 80px at 20% 20%, color-mix(in srgb, #fff 35%, transparent), transparent 70%),
        linear-gradient(135deg, var(--sf-primary), var(--sf-secondary) 48%, var(--sf-accent));
    }
    .meta {
      font-size: 12px;
    }
    h3 {
      margin: 8px 0 4px;
      font-size: 16px;
      color: var(--sf-emphasis);
      overflow-wrap: anywhere;
    }
    .synopsis {
      margin: 0;
      font-size: 13px;
      color: var(--sf-text-muted);
      display: -webkit-box;
      -webkit-line-clamp: 3;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
    .tags {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      font-size: 12px;
      color: var(--sf-text-muted);
    }
    footer {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
    }
    .stat {
      font-size: 12px;
      color: var(--sf-text-muted);
      white-space: nowrap;
    }
    .date {
      margin: 0;
      font-size: 11px;
    }
  `,
})
export class WorkCardComponent {
  readonly work = input.required<WorkCard>();
  protected readonly kindLabel = computed(() => {
    const w = this.work();
    return w.kind === 'PACKAGE' ? labelOf(PACKAGE_TYPES, w.packageType) || KIND_LABEL.PACKAGE : KIND_LABEL[w.kind];
  });
}

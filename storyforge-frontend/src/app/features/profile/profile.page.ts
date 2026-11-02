import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { CommunityApi } from '../../core/api/community-api.service';
import { errorMessage } from '../../core/api/storyforge-api.service';
import { Achievement, ActivityEntry, Contributions, Profile, WorkCard } from '../../core/models/community';
import { ContributionHeatmapComponent } from '../../shared/components/contribution-heatmap/contribution-heatmap.component';
import { WorkCardComponent } from '../../shared/components/work-card/work-card.component';

/** 유저 프로필: 작품, 활동 잔디와 내역, 업적 */
@Component({
  selector: 'sf-profile-page',
  imports: [
    DatePipe,
    DecimalPipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatTabsModule,
    MatTooltipModule,
    ContributionHeatmapComponent,
    WorkCardComponent,
  ],
  template: `
    <div class="sf-page">
      @if (error()) {
        <div class="sf-empty" role="alert">{{ error() }}</div>
      } @else if (!profile()) {
        <mat-progress-bar mode="indeterminate" />
      } @else if (profile(); as p) {
        <header class="head">
          <span class="avatar" [style.background]="p.user.avatarColor ?? 'var(--sf-primary)'" aria-hidden="true">{{ initial() }}</span>
          <div class="info">
            <h1>{{ p.user.displayName }}</h1>
            @if (p.bio) {
              <p class="bio">{{ p.bio }}</p>
            }
            <div class="sf-row meta">
              <span class="sf-muted">{{ p.joinedAt | date: 'yyyy-MM-dd' }} 가입</span>
              @if (p.website) {
                <a [href]="p.website" target="_blank" rel="noopener noreferrer nofollow">{{ p.website }}</a>
              }
            </div>
          </div>
          @if (p.self) {
            <a mat-stroked-button routerLink="/me/settings"><mat-icon>settings</mat-icon> 계정 설정</a>
          }
        </header>
        <div class="stats">
          <div><strong>{{ p.stats.works | number }}</strong><span>공개 작품</span></div>
          <div><strong>{{ p.stats.likesReceived | number }}</strong><span>받은 추천</span></div>
          <div><strong>{{ p.stats.clonesReceived | number }}</strong><span>받은 Clone</span></div>
          <div><strong>{{ p.stats.achievements | number }}</strong><span>업적</span></div>
        </div>

        <section class="grass" aria-label="활동 잔디">
          @if (grass(); as g) {
            <sf-contribution-heatmap [data]="g" [selected]="day()" (pick)="pickDay($event)" />
          }
        </section>

        <mat-tab-group [preserveContent]="true">
          <mat-tab [label]="'작품 ' + works().length">
            <div class="tab">
              @if (!works().length) {
                <div class="sf-empty">공개한 작품이 없습니다.</div>
              } @else {
                <div class="sf-card-grid">
                  @for (w of works(); track w.id) {
                    <sf-work-card [work]="w" />
                  }
                </div>
              }
            </div>
          </mat-tab>
          <mat-tab label="활동 내역">
            <div class="tab">
              <div class="sf-row feed-head">
                <strong>{{ day() ? day() + ' 활동' : '최근 활동' }}</strong>
                <span class="sf-spacer"></span>
                @if (day()) {
                  <button mat-button (click)="pickDay(day()!)">전체 보기</button>
                }
              </div>
              @if (!feed().length) {
                <div class="sf-empty">표시할 활동이 없습니다.@if (!p.self) { 비공개 프로젝트 활동은 건수만 잔디에 집계됩니다.}</div>
              }
              <ul class="feed">
                @for (e of feed(); track e.id) {
                  <li>
                    <mat-icon [matTooltip]="e.actor === 'AGENT' ? '에이전트 작업' : '수동 작업'">{{ e.actor === 'AGENT' ? 'smart_toy' : 'person' }}</mat-icon>
                    <div class="entry">
                      <span>{{ e.summary }}</span>
                      @if (e.workId) {
                        <a [routerLink]="['/works', e.workId]">{{ e.workTitle }}</a>
                      } @else if (e.postId) {
                        <a [routerLink]="['/posts', e.postId]">{{ e.postTitle }}</a>
                      } @else if (e.projectId) {
                        <a [routerLink]="['/projects', e.projectId]">{{ e.projectTitle }}</a>
                      }
                    </div>
                    <span class="sf-muted when">{{ e.createdAt | date: 'yyyy-MM-dd HH:mm' }}</span>
                  </li>
                }
              </ul>
              @if (feedTotal() > 20) {
                <mat-paginator [length]="feedTotal()" [pageIndex]="feedPage()" [pageSize]="20" [hidePageSize]="true" (page)="onFeedPage($event)" aria-label="활동 페이지" />
              }
            </div>
          </mat-tab>
          <mat-tab [label]="'업적 ' + earnedCount() + '/' + achievements().length">
            <div class="tab">
              <ul class="badges">
                @for (a of achievements(); track a.code) {
                  <li [class.earned]="a.earnedAt" [attr.data-tier]="a.tier">
                    <mat-icon class="badge-icon" aria-hidden="true">{{ a.earnedAt ? a.icon : 'lock' }}</mat-icon>
                    <div class="badge-body">
                      <strong>{{ a.name }}</strong>
                      <span class="sf-muted">{{ a.description }}</span>
                      @if (a.earnedAt) {
                        <small class="sf-muted">{{ tierLabel(a.tier) }} · {{ a.earnedAt | date: 'yyyy-MM-dd' }} 달성</small>
                      } @else {
                        <small class="sf-muted">{{ tierLabel(a.tier) }} · {{ a.progress | number }} / {{ a.target | number }}</small>
                      }
                    </div>
                  </li>
                }
              </ul>
            </div>
          </mat-tab>
        </mat-tab-group>
      }
    </div>
  `,
  styles: `
    .head {
      display: flex;
      gap: 16px;
      align-items: flex-start;
      flex-wrap: wrap;
    }
    .avatar {
      width: 72px;
      height: 72px;
      flex: none;
      display: grid;
      place-items: center;
      border-radius: 50%;
      color: #fff;
      font-size: 32px;
      font-weight: 700;
    }
    .info {
      flex: 1;
      min-width: 200px;
    }
    h1 {
      margin: 0;
      font-size: 24px;
      overflow-wrap: anywhere;
    }
    .bio {
      margin: 4px 0;
      white-space: pre-wrap;
    }
    .meta {
      font-size: 13px;
    }
    .stats {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(110px, 1fr));
      gap: 8px;
      margin: 16px 0;
    }
    .stats div {
      display: flex;
      flex-direction: column;
      padding: 10px 12px;
      border: 1px solid var(--sf-border);
      border-radius: 8px;
      background: var(--sf-surface);
    }
    .stats strong {
      font-size: 20px;
    }
    .stats span {
      font-size: 12px;
      color: var(--sf-text-muted);
    }
    .grass {
      padding: 12px;
      border: 1px solid var(--sf-border);
      border-radius: var(--sf-radius);
      background: var(--sf-surface);
      margin-bottom: 16px;
    }
    .tab {
      padding: 16px 0;
    }
    .feed-head {
      margin-bottom: 8px;
    }
    .feed {
      list-style: none;
      margin: 0;
      padding: 0;
    }
    .feed li {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 8px 0;
      border-bottom: 1px solid var(--sf-border);
    }
    .feed mat-icon {
      flex: none;
      color: var(--sf-text-muted);
    }
    .entry {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      font-size: 14px;
    }
    .when {
      font-size: 12px;
      white-space: nowrap;
    }
    mat-paginator {
      background: transparent;
    }
    .badges {
      list-style: none;
      margin: 0;
      padding: 0;
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
      gap: 8px;
    }
    .badges li {
      display: flex;
      gap: 10px;
      padding: 12px;
      border: 1px solid var(--sf-border);
      border-radius: 8px;
      background: var(--sf-surface);
      opacity: 0.6;
    }
    .badges li.earned {
      opacity: 1;
      border-left: 4px solid var(--sf-accent);
    }
    .badge-icon {
      flex: none;
      color: var(--sf-text-muted);
    }
    .earned .badge-icon {
      color: var(--sf-accent);
    }
    .badge-body {
      display: flex;
      flex-direction: column;
      gap: 2px;
      font-size: 13px;
    }
  `,
})
export class ProfilePage {
  private readonly api = inject(CommunityApi);
  readonly userId = input.required<string>();

  protected readonly profile = signal<Profile | null>(null);
  protected readonly works = signal<WorkCard[]>([]);
  protected readonly grass = signal<Contributions | null>(null);
  protected readonly feed = signal<ActivityEntry[]>([]);
  protected readonly feedTotal = signal(0);
  protected readonly feedPage = signal(0);
  protected readonly day = signal<string | null>(null);
  protected readonly achievements = signal<Achievement[]>([]);
  protected readonly error = signal<string | null>(null);

  protected readonly initial = computed(() => Array.from(this.profile()?.user.displayName ?? '?')[0]);
  protected readonly earnedCount = computed(() => this.achievements().filter((a) => a.earnedAt).length);

  constructor() {
    effect(() => {
      const id = Number(this.userId());
      this.profile.set(null);
      this.error.set(null);
      this.day.set(null);
      this.feedPage.set(0);
      this.api.profile(id).subscribe({
        next: (p) => this.profile.set(p),
        error: (e) => this.error.set(errorMessage(e)),
      });
      this.api.userWorks(id).subscribe((w) => this.works.set(w));
      this.api.contributions(id).subscribe((g) => this.grass.set(g));
      this.api.achievements(id).subscribe((a) => this.achievements.set(a));
      // 날짜·페이지 변경은 loadFeed를 직접 부릅니다. 여기서 추적하면 프로필 전체를 다시 읽고 탭이 처음으로 돌아갑니다.
      untracked(() => this.loadFeed(id));
    });
  }

  private loadFeed(id: number): void {
    this.api.userActivity(id, { date: this.day() ?? undefined, page: this.feedPage(), size: 20 }).subscribe((r) => {
      this.feed.set(r.items);
      this.feedTotal.set(r.total);
    });
  }

  protected pickDay(date: string): void {
    this.day.update((d) => (d === date ? null : date));
    this.feedPage.set(0);
    this.loadFeed(Number(this.userId()));
  }

  protected onFeedPage(e: PageEvent): void {
    this.feedPage.set(e.pageIndex);
    this.loadFeed(Number(this.userId()));
  }

  protected tierLabel(tier: Achievement['tier']): string {
    return { BRONZE: '브론즈', SILVER: '실버', GOLD: '골드', PLATINUM: '플래티넘' }[tier];
  }
}

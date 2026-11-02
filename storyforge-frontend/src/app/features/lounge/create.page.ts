import { DatePipe } from '@angular/common';
import { Component, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { CommunityApi } from '../../core/api/community-api.service';
import { StoryforgeApi, errorMessage } from '../../core/api/storyforge-api.service';
import { AuthService } from '../../core/auth/auth.service';
import { GENRES, POST_CATEGORIES, PostCard, PostCategory, labelOf } from '../../core/models/community';
import { ProjectForm } from '../../core/models/models';
import { UserChipComponent } from '../../shared/components/user-chip/user-chip.component';
import { ProjectCreateDialog } from '../projects/project-create.dialog';
import { PostEditDialog } from './post-edit.dialog';

/** 창작하기: 새 작품 시작과 창작 라운지(일반 커뮤니티 게시판) */
@Component({
  selector: 'sf-create-page',
  imports: [
    DatePipe,
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatChipsModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    UserChipComponent,
  ],
  template: `
    <div class="sf-page">
      <h1>창작하기</h1>
      <p class="sf-muted">어떤 방식으로 시작할지 고르세요. 시작한 작업은 '내 프로젝트'에 모입니다.</p>
      <section class="starts" aria-label="시작 방법">
        <button type="button" class="start" (click)="start('LONG')">
          <mat-icon>menu_book</mat-icon>
          <strong>장편 시작</strong>
          <span>Story Bible부터 챕터까지 순서대로 완성합니다.</span>
        </button>
        <button type="button" class="start" (click)="start('SHORT')">
          <mat-icon>article</mat-icon>
          <strong>단편 시작</strong>
          <span>같은 절차를 1~3장으로 짧게 진행합니다.</span>
        </button>
        <a class="start" routerLink="/board/long">
          <mat-icon>call_split</mat-icon>
          <strong>세계관 가져와 시작</strong>
          <span>마음에 드는 작품을 Clone해 그 세계에서 씁니다.</span>
        </a>
        <a class="start" routerLink="/package-forge">
          <mat-icon>hub</mat-icon>
          <strong>패키지포지</strong>
          <span>게시한 작품을 유니버스, 옴니버스 등으로 묶습니다.</span>
        </a>
      </section>

      <h2>장르로 시작</h2>
      <div class="genres">
        @for (g of genres; track g) {
          <button type="button" class="genre" (click)="start('LONG', g)">{{ g }}</button>
        }
      </div>

      <div class="sf-row lounge-head">
        <h2>창작 라운지</h2>
        <span class="sf-spacer"></span>
        <button mat-flat-button (click)="write()"><mat-icon>edit</mat-icon> 글쓰기</button>
      </div>
      <div class="sf-row filters">
        <mat-chip-listbox aria-label="분류" [value]="category()" (change)="setCategory($event.value)">
          <mat-chip-option value="ALL">전체</mat-chip-option>
          @for (c of categories; track c.value) {
            <mat-chip-option [value]="c.value">{{ c.label }}</mat-chip-option>
          }
        </mat-chip-listbox>
        <span class="sf-spacer"></span>
        <form (ngSubmit)="search()" class="search">
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>라운지 검색</mat-label>
            <input matInput name="q" [(ngModel)]="q" maxlength="100" />
            <button matSuffix mat-icon-button type="submit" aria-label="검색"><mat-icon>search</mat-icon></button>
          </mat-form-field>
        </form>
      </div>

      @if (error()) {
        <div class="sf-empty" role="alert">{{ error() }}</div>
      } @else if (!posts().length) {
        <div class="sf-empty">글이 없습니다. 첫 글을 남겨 보세요.</div>
      } @else {
        <div class="table-wrap">
          <table class="sf-table posts">
            <thead>
              <tr>
                <th scope="col">분류</th>
                <th scope="col">제목</th>
                <th scope="col">글쓴이</th>
                <th scope="col" class="num">추천</th>
                <th scope="col" class="num">조회</th>
                <th scope="col">날짜</th>
              </tr>
            </thead>
            <tbody>
              @for (p of posts(); track p.id) {
                <tr>
                  <td><span class="sf-badge">{{ categoryLabel(p.category) }}</span></td>
                  <td class="title">
                    <a [routerLink]="['/posts', p.id]">{{ p.title }}</a>
                    @if (p.commentCount) {
                      <span class="sf-muted"> [{{ p.commentCount }}]</span>
                    }
                  </td>
                  <td><sf-user-chip [user]="p.author" /></td>
                  <td class="num">{{ p.likeCount }}</td>
                  <td class="num">{{ p.viewCount }}</td>
                  <td class="date">{{ p.createdAt | date: 'yyyy-MM-dd' }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        <mat-paginator [length]="total()" [pageIndex]="page()" [pageSize]="20" [hidePageSize]="true" (page)="onPage($event)" aria-label="라운지 페이지" />
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
      margin: 24px 0 8px;
    }
    .starts {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 12px;
      margin-top: 16px;
    }
    .start {
      display: flex;
      flex-direction: column;
      gap: 6px;
      align-items: flex-start;
      text-align: left;
      padding: 16px;
      font: inherit;
      color: var(--sf-text);
      text-decoration: none;
      border: 1px solid var(--sf-border);
      border-radius: var(--sf-radius);
      background: var(--sf-surface);
      cursor: pointer;
    }
    .start:hover {
      border-color: var(--sf-secondary);
    }
    .start mat-icon {
      color: var(--sf-emphasis);
    }
    .start span {
      font-size: 13px;
      color: var(--sf-text-muted);
    }
    .genres {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
    }
    .genre {
      padding: 6px 12px;
      font: inherit;
      font-size: 13px;
      color: var(--sf-text);
      border: 1px solid var(--sf-border);
      border-radius: 999px;
      background: var(--sf-surface);
      cursor: pointer;
    }
    .genre:hover {
      border-color: var(--sf-secondary);
    }
    .filters {
      margin-bottom: 12px;
    }
    .search {
      min-width: 220px;
    }
    .search mat-form-field {
      width: 100%;
    }
    .table-wrap {
      overflow-x: auto;
    }
    .title a {
      text-decoration: none;
      color: var(--sf-text);
      font-weight: 500;
    }
    .title a:hover {
      text-decoration: underline;
    }
    .num {
      text-align: right;
    }
    .date {
      white-space: nowrap;
      color: var(--sf-text-muted);
    }
    mat-paginator {
      background: transparent;
    }
  `,
})
export class CreatePage {
  private readonly api = inject(CommunityApi);
  private readonly projects = inject(StoryforgeApi);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);

  protected readonly genres = GENRES;
  protected readonly categories = POST_CATEGORIES;
  protected readonly posts = signal<PostCard[]>([]);
  protected readonly total = signal(0);
  protected readonly page = signal(0);
  protected readonly category = signal<PostCategory | 'ALL'>('ALL');
  protected readonly query = signal('');
  protected readonly error = signal<string | null>(null);
  private readonly reload = signal(0);
  protected q = '';

  constructor() {
    effect(() => {
      this.reload();
      const category = this.category();
      this.api
        .posts({ category: category === 'ALL' ? undefined : category, q: this.query(), page: this.page(), size: 20 })
        .subscribe({
          next: (r) => {
            this.posts.set(r.items);
            this.total.set(r.total);
          },
          error: (e) => this.error.set(errorMessage(e)),
        });
    });
  }

  protected categoryLabel(c: PostCategory): string {
    return labelOf(POST_CATEGORIES, c);
  }

  protected setCategory(value: PostCategory | 'ALL' | undefined): void {
    this.page.set(0);
    this.category.set(value ?? 'ALL');
  }

  protected search(): void {
    this.page.set(0);
    this.query.set(this.q.trim());
  }

  protected onPage(e: PageEvent): void {
    this.page.set(e.pageIndex);
  }

  private requireLogin(): boolean {
    if (this.auth.isLoggedIn()) return true;
    this.router.navigate(['/login'], { queryParams: { returnUrl: '/create' } });
    return false;
  }

  protected start(form: ProjectForm, genre?: string): void {
    if (!this.requireLogin()) return;
    this.dialog
      .open(ProjectCreateDialog, { width: '480px', data: { form, genre } })
      .afterClosed()
      .subscribe((body) => {
        if (!body) return;
        this.projects.createProject(body).subscribe({
          next: (p) => this.router.navigate(['/projects', p.id]),
          error: (e) => this.snack.open(errorMessage(e), '닫기', { duration: 4000 }),
        });
      });
  }

  protected write(): void {
    if (!this.requireLogin()) return;
    this.dialog
      .open(PostEditDialog, { width: '640px', maxWidth: '95vw', data: null })
      .afterClosed()
      .subscribe((body) => {
        if (!body) return;
        this.api.createPost(body).subscribe({
          next: (p) => this.router.navigate(['/posts', p.id]),
          error: (e) => this.snack.open(errorMessage(e), '닫기', { duration: 4000 }),
        });
      });
  }
}

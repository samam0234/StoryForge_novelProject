import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { CommunityApi } from '../../core/api/community-api.service';
import { errorMessage } from '../../core/api/storyforge-api.service';
import { GENRES, WorkCard, WorkKind } from '../../core/models/community';
import { WorkCardComponent } from '../../shared/components/work-card/work-card.component';

const BOARDS: Record<string, { kind: WorkKind; title: string; hint: string }> = {
  long: { kind: 'LONG', title: '롱품', hint: '장편 소설. 읽고, 추천하고, 세계관이나 원고를 Clone해 이어 쓸 수 있습니다.' },
  short: { kind: 'SHORT', title: '숏품', hint: '단편 소설. 짧게 읽고 가볍게 파생시켜 보세요.' },
  package: { kind: 'PACKAGE', title: '패키지포지', hint: '단독 작품을 묶은 통합본. 유니버스, 옴니버스, 사가 등으로 연결된 세계를 읽습니다.' },
};

/** 롱품, 숏품, 패키지포지 게시판 */
@Component({
  selector: 'sf-board-page',
  imports: [
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSelectModule,
    WorkCardComponent,
  ],
  template: `
    <div class="sf-page">
      <header class="sf-row head">
        <div>
          <h1>{{ board().title }}</h1>
          <p class="sf-muted">{{ board().hint }}</p>
        </div>
        <span class="sf-spacer"></span>
        @if (board().kind === 'PACKAGE') {
          <a mat-flat-button routerLink="/package-forge"><mat-icon>hub</mat-icon> 패키지 만들기</a>
        } @else {
          <a mat-flat-button routerLink="/create"><mat-icon>edit</mat-icon> 창작하기</a>
        }
      </header>

      <form class="filters" (ngSubmit)="search()">
        <mat-form-field appearance="outline" subscriptSizing="dynamic" class="q">
          <mat-label>제목, 줄거리, 태그 검색</mat-label>
          <input matInput name="q" [(ngModel)]="q" maxlength="100" />
          <button matSuffix mat-icon-button type="submit" aria-label="검색"><mat-icon>search</mat-icon></button>
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>장르</mat-label>
          <mat-select name="genre" [ngModel]="genre()" (ngModelChange)="setGenre($event)">
            <mat-option value="">전체</mat-option>
            @for (g of genres; track g) {
              <mat-option [value]="g">{{ g }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>정렬</mat-label>
          <mat-select name="sort" [ngModel]="sort()" (ngModelChange)="setSort($event)">
            <mat-option value="latest">최신순</mat-option>
            <mat-option value="popular">추천순</mat-option>
            <mat-option value="views">조회순</mat-option>
            <mat-option value="clones">Clone순</mat-option>
          </mat-select>
        </mat-form-field>
      </form>

      @if (loading()) {
        <mat-progress-bar mode="indeterminate" />
      } @else if (error()) {
        <div class="sf-empty" role="alert">{{ error() }}</div>
      } @else if (!works().length) {
        <div class="sf-empty">조건에 맞는 작품이 없습니다.</div>
      } @else {
        <div class="sf-card-grid">
          @for (w of works(); track w.id) {
            <sf-work-card [work]="w" />
          }
        </div>
        <mat-paginator
          [length]="total()"
          [pageIndex]="page()"
          [pageSize]="20"
          [hidePageSize]="true"
          (page)="onPage($event)"
          aria-label="게시판 페이지"
        />
      }
    </div>
  `,
  styles: `
    h1 {
      margin: 0 0 4px;
      font-size: 24px;
    }
    .head {
      margin-bottom: 16px;
    }
    .filters {
      display: grid;
      grid-template-columns: minmax(200px, 1fr) 160px 140px;
      gap: 8px;
      margin-bottom: 16px;
    }
    @media (max-width: 640px) {
      .filters {
        grid-template-columns: 1fr;
      }
    }
    mat-paginator {
      background: transparent;
      margin-top: 8px;
    }
  `,
})
export class BoardPage {
  private readonly api = inject(CommunityApi);
  readonly kind = input.required<string>();

  protected readonly genres = GENRES;
  protected readonly board = computed(() => BOARDS[this.kind()] ?? BOARDS['long']);
  protected readonly works = signal<WorkCard[]>([]);
  protected readonly total = signal(0);
  protected readonly page = signal(0);
  protected readonly sort = signal('latest');
  protected readonly genre = signal('');
  protected readonly query = signal('');
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected q = '';

  constructor() {
    effect(() => {
      const kind = this.board().kind;
      const request = { kind, genre: this.genre(), q: this.query(), sort: this.sort(), page: this.page(), size: 20 };
      this.loading.set(true);
      this.error.set(null);
      this.api.board(request).subscribe({
        next: (r) => {
          this.works.set(r.items);
          this.total.set(r.total);
          this.loading.set(false);
        },
        error: (e) => {
          this.error.set(errorMessage(e));
          this.loading.set(false);
        },
      });
    });
  }

  protected search(): void {
    this.page.set(0);
    this.query.set(this.q.trim());
  }

  protected setGenre(genre: string): void {
    this.page.set(0);
    this.genre.set(genre);
  }

  protected setSort(sort: string): void {
    this.page.set(0);
    this.sort.set(sort);
  }

  protected onPage(e: PageEvent): void {
    this.page.set(e.pageIndex);
  }
}

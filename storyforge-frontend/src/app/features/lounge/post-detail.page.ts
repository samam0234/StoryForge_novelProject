import { DatePipe } from '@angular/common';
import { Component, effect, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { CommunityApi } from '../../core/api/community-api.service';
import { errorMessage } from '../../core/api/storyforge-api.service';
import { AuthService } from '../../core/auth/auth.service';
import { POST_CATEGORIES, PostCategory, PostDetail, labelOf } from '../../core/models/community';
import { UserChipComponent } from '../../shared/components/user-chip/user-chip.component';
import { CommentThreadComponent } from '../board/comment-thread.component';
import { PostEditDialog } from './post-edit.dialog';

/** 라운지 게시글 읽기 */
@Component({
  selector: 'sf-post-detail-page',
  imports: [DatePipe, RouterLink, MatButtonModule, MatIconModule, MatProgressBarModule, UserChipComponent, CommentThreadComponent],
  template: `
    <div class="sf-page narrow">
      @if (error()) {
        <div class="sf-empty" role="alert">{{ error() }}</div>
      } @else if (!post()) {
        <mat-progress-bar mode="indeterminate" />
      } @else if (post(); as p) {
        <a mat-button routerLink="/create"><mat-icon>arrow_back</mat-icon> 라운지</a>
        <article>
          <span class="sf-badge">{{ categoryLabel(p.card.category) }}</span>
          <h1>{{ p.card.title }}</h1>
          <div class="sf-row meta">
            <sf-user-chip [user]="p.card.author" />
            <span class="sf-muted">{{ p.card.createdAt | date: 'yyyy-MM-dd HH:mm' }}</span>
            <span class="sf-muted">조회 {{ p.card.viewCount }}</span>
          </div>
          <p class="content">{{ p.content }}</p>
          <div class="sf-row">
            @if (!p.owned) {
              <button mat-stroked-button (click)="toggleLike()" [attr.aria-pressed]="p.liked">
                <mat-icon>{{ p.liked ? 'favorite' : 'favorite_border' }}</mat-icon> 추천 {{ p.card.likeCount }}
              </button>
            } @else {
              <span class="sf-muted">추천 {{ p.card.likeCount }}</span>
              <span class="sf-spacer"></span>
              <button mat-button (click)="edit()">수정</button>
              <button mat-button (click)="remove()">삭제</button>
            }
          </div>
        </article>
        <h2>댓글 {{ p.card.commentCount }}</h2>
        <sf-comment-thread [postId]="p.card.id" (changed)="commentCount($event)" />
      }
    </div>
  `,
  styles: `
    .narrow {
      max-width: 800px;
    }
    h1 {
      margin: 8px 0;
      font-size: 22px;
      overflow-wrap: anywhere;
    }
    h2 {
      font-size: 16px;
      margin: 24px 0 8px;
    }
    .meta {
      font-size: 13px;
    }
    .content {
      white-space: pre-wrap;
      overflow-wrap: anywhere;
      line-height: 1.75;
      margin: 16px 0;
    }
  `,
})
export class PostDetailPage {
  private readonly api = inject(CommunityApi);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);

  readonly postId = input.required<string>();
  protected readonly post = signal<PostDetail | null>(null);
  protected readonly error = signal<string | null>(null);

  constructor() {
    effect(() => {
      this.api.post(Number(this.postId())).subscribe({
        next: (p) => this.post.set(p),
        error: (e) => this.error.set(errorMessage(e)),
      });
    });
  }

  protected categoryLabel(c: PostCategory): string {
    return labelOf(POST_CATEGORIES, c);
  }

  protected commentCount(count: number): void {
    const p = this.post();
    if (p) this.post.set({ ...p, card: { ...p.card, commentCount: count } });
  }

  protected toggleLike(): void {
    const p = this.post();
    if (!p) return;
    if (!this.auth.isLoggedIn()) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    this.api.likePost(p.card.id, !p.liked).subscribe({
      next: (r) => this.post.set({ ...p, liked: r.liked, card: { ...p.card, likeCount: r.likeCount } }),
      error: (e) => this.snack.open(errorMessage(e), '닫기', { duration: 4000 }),
    });
  }

  protected edit(): void {
    const p = this.post();
    if (!p) return;
    this.dialog
      .open(PostEditDialog, { width: '640px', maxWidth: '95vw', data: { category: p.card.category, title: p.card.title, content: p.content } })
      .afterClosed()
      .subscribe((body) => {
        if (!body) return;
        this.api.updatePost(p.card.id, body).subscribe({
          next: (card) => this.post.set({ ...p, card, content: body.content }),
          error: (e) => this.snack.open(errorMessage(e), '닫기', { duration: 4000 }),
        });
      });
  }

  protected remove(): void {
    const p = this.post();
    if (!p || !confirm('글을 삭제할까요? 되돌릴 수 없습니다.')) return;
    this.api.deletePost(p.card.id).subscribe({
      next: () => this.router.navigate(['/create']),
      error: (e) => this.snack.open(errorMessage(e), '닫기', { duration: 4000 }),
    });
  }
}

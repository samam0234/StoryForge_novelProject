import { DatePipe, NgTemplateOutlet } from '@angular/common';
import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { CommunityApi } from '../../core/api/community-api.service';
import { errorMessage } from '../../core/api/storyforge-api.service';
import { AuthService } from '../../core/auth/auth.service';
import { CommentView } from '../../core/models/community';
import { UserChipComponent } from '../../shared/components/user-chip/user-chip.component';
import { ReportDialog, ReportDialogData } from '../moderation/report.dialog';

/** 작품·게시글 공용 댓글. 내용은 글자로만 보여 줍니다 (HTML로 넣지 않음). */
@Component({
  selector: 'sf-comment-thread',
  imports: [DatePipe, NgTemplateOutlet, FormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, UserChipComponent],
  template: `
    @if (auth.isLoggedIn()) {
      <div class="write">
        <mat-form-field appearance="outline" subscriptSizing="dynamic" class="sf-full">
          <mat-label>댓글 쓰기</mat-label>
          <textarea matInput rows="2" maxlength="2000" [(ngModel)]="draft" name="draft"></textarea>
        </mat-form-field>
        <button mat-flat-button [disabled]="busy() || !draft.trim()" (click)="add(null, draft)">등록</button>
      </div>
    } @else {
      <p class="sf-muted"><a routerLink="/login">로그인</a>하면 댓글을 쓸 수 있습니다.</p>
    }

    @if (!threads().length) {
      <div class="sf-empty">첫 댓글을 남겨 보세요.</div>
    }
    <ul class="list">
      @for (t of threads(); track t.comment.id) {
        <li>
          <ng-container *ngTemplateOutlet="row; context: { $implicit: t.comment, canReply: true }" />
          @if (t.replies.length) {
            <ul class="replies">
              @for (r of t.replies; track r.id) {
                <li><ng-container *ngTemplateOutlet="row; context: { $implicit: r, canReply: false }" /></li>
              }
            </ul>
          }
          @if (replyTo() === t.comment.id) {
            <div class="write reply">
              <mat-form-field appearance="outline" subscriptSizing="dynamic" class="sf-full">
                <mat-label>답글</mat-label>
                <textarea matInput rows="2" maxlength="2000" [(ngModel)]="replyDraft" name="reply"></textarea>
              </mat-form-field>
              <button mat-stroked-button [disabled]="busy() || !replyDraft.trim()" (click)="add(t.comment.id, replyDraft)">등록</button>
            </div>
          }
        </li>
      }
    </ul>

    <ng-template #row let-c let-canReply="canReply">
      <div class="sf-row meta">
        <sf-user-chip [user]="c.author" />
        <span class="sf-muted when">{{ c.createdAt | date: 'yyyy-MM-dd HH:mm' }}{{ c.updatedAt !== c.createdAt ? ' (수정됨)' : '' }}</span>
        <span class="sf-spacer"></span>
        @if (canReply && auth.isLoggedIn()) {
          <button mat-button (click)="replyTo.set(replyTo() === c.id ? null : c.id)">답글</button>
        }
        @if (c.canEdit) {
          <button mat-button (click)="startEdit(c)">수정</button>
        }
        @if (c.canDelete) {
          <button mat-button (click)="remove(c)">삭제</button>
        }
        @if (auth.isLoggedIn() && !c.hidden && auth.user()?.id !== c.author.id) {
          <button mat-button (click)="reportComment(c)">신고</button>
        }
      </div>
      @if (editing() === c.id) {
        <div class="write">
          <mat-form-field appearance="outline" subscriptSizing="dynamic" class="sf-full">
            <mat-label>댓글 수정</mat-label>
            <textarea matInput rows="2" maxlength="2000" [(ngModel)]="editDraft" name="edit"></textarea>
          </mat-form-field>
          <button mat-stroked-button [disabled]="busy() || !editDraft.trim()" (click)="saveEdit(c)">저장</button>
        </div>
      } @else {
        <p class="content" [class.masked]="c.hidden">{{ c.content }}</p>
      }
    </ng-template>
  `,
  styles: `
    :host {
      display: block;
    }
    .write {
      display: flex;
      gap: 8px;
      align-items: center;
      margin-bottom: 12px;
    }
    .reply {
      margin: 8px 0 0 24px;
    }
    .list,
    .replies {
      list-style: none;
      margin: 0;
      padding: 0;
    }
    .list > li {
      padding: 10px 0;
      border-bottom: 1px solid var(--sf-border);
    }
    .replies {
      margin: 8px 0 0 24px;
      padding-left: 12px;
      border-left: 2px solid var(--sf-border);
    }
    .replies > li + li {
      margin-top: 8px;
    }
    .when {
      font-size: 12px;
    }
    .masked {
      color: var(--sf-text-muted);
      font-style: italic;
    }
    .content {
      margin: 4px 0 0;
      white-space: pre-wrap;
      overflow-wrap: anywhere;
    }
  `,
})
export class CommentThreadComponent {
  private readonly api = inject(CommunityApi);
  private readonly snack = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  protected readonly auth = inject(AuthService);

  /** 둘 중 하나를 넘깁니다. */
  readonly workId = input<number | null>(null);
  readonly postId = input<number | null>(null);
  readonly changed = output<number>();

  private readonly comments = signal<CommentView[]>([]);
  protected readonly busy = signal(false);
  protected readonly replyTo = signal<number | null>(null);
  protected readonly editing = signal<number | null>(null);
  protected draft = '';
  protected replyDraft = '';
  protected editDraft = '';

  protected readonly threads = computed(() => {
    const all = this.comments();
    return all
      .filter((c) => c.parentId == null)
      .map((comment) => ({ comment, replies: all.filter((r) => r.parentId === comment.id) }));
  });

  constructor() {
    effect(() => {
      const work = this.workId();
      const post = this.postId();
      if (work != null) this.api.workComments(work).subscribe((list) => this.comments.set(list));
      else if (post != null) this.api.postComments(post).subscribe((list) => this.comments.set(list));
    });
  }

  protected add(parentId: number | null, text: string): void {
    const body = { content: text.trim(), parentId };
    const work = this.workId();
    const request: Observable<CommentView> = work != null ? this.api.addWorkComment(work, body) : this.api.addPostComment(this.postId() as number, body);
    this.run(request, (c) => {
      this.comments.update((list) => [...list, c]);
      this.draft = '';
      this.replyDraft = '';
      this.replyTo.set(null);
      this.changed.emit(this.comments().length);
    });
  }

  protected startEdit(c: CommentView): void {
    this.editDraft = c.content;
    this.editing.set(c.id);
  }

  protected saveEdit(c: CommentView): void {
    this.run(this.api.editComment(c.id, this.editDraft.trim()), (updated) => {
      this.comments.update((list) => list.map((x) => (x.id === c.id ? updated : x)));
      this.editing.set(null);
    });
  }

  protected reportComment(c: CommentView): void {
    const data: ReportDialogData = { targetType: 'COMMENT', targetId: c.id, label: `${c.author.displayName}님의 댓글` };
    this.dialog.open(ReportDialog, { data, maxWidth: '95vw' });
  }

  protected remove(c: CommentView): void {
    if (!confirm('댓글을 삭제할까요? 답글도 함께 지워집니다.')) return;
    this.run(this.api.deleteComment(c.id), () => {
      this.comments.update((list) => list.filter((x) => x.id !== c.id && x.parentId !== c.id));
      this.changed.emit(this.comments().length);
    });
  }

  private run<T>(request: Observable<T>, done: (r: T) => void): void {
    this.busy.set(true);
    request.subscribe({
      next: (r) => {
        this.busy.set(false);
        done(r);
      },
      error: (e) => {
        this.busy.set(false);
        this.snack.open(errorMessage(e), '닫기', { duration: 4000 });
      },
    });
  }
}

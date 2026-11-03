import { DatePipe } from '@angular/common';
import { Component, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CommunityApi } from '../../core/api/community-api.service';
import { errorMessage } from '../../core/api/storyforge-api.service';
import { Collaborator, CommitTarget, WorkCommit, WorkGraph } from '../../core/models/community';
import { CommitGraphComponent } from '../../shared/components/commit-graph/commit-graph.component';
import { WorkGraphComponent } from '../../shared/components/work-graph/work-graph.component';
import { ProjectContext } from './project-context';

/** 프로젝트 작업 기록과, 참여자만 보는 작업 커밋 */
@Component({
  selector: 'sf-project-activity-page',
  imports: [
    DatePipe,
    FormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    WorkGraphComponent,
    CommitGraphComponent,
  ],
  template: `
    <div class="sf-page">
      <p class="sf-muted hint">막대를 누르면 그날 한 작업이 표에 나옵니다. 기록표의 점을 누르면 그 문단이 열립니다. 게시하면 요약만 작품의 작업 그래프로 공개되고, 인용 구간은 참여자에게만 남습니다.</p>
      @if (graph(); as g) {
        <sf-work-graph [graph]="g" />
      }

      <h2>함께 쓰기</h2>
      <p class="sf-muted hint">초대를 수락한 사람만 이 원고와 작업 커밋을 봅니다. 게시와 삭제는 주인만 할 수 있습니다.</p>
      @if (!members().length) {
        <p class="sf-muted">아직 함께 쓰는 사람이 없습니다.</p>
      } @else {
        <ul class="members">
          @for (m of members(); track m.id) {
            <li>
              <span>{{ m.user.displayName }}</span>
              <span class="sf-badge">{{ m.status === 'ACCEPTED' ? '참여 중' : '수락 대기' }}</span>
              <span class="sf-muted">{{ m.createdAt | date: 'yyyy-MM-dd' }}</span>
            </li>
          }
        </ul>
      }
      @if (isOwner()) {
        <form class="invite" (ngSubmit)="invite()">
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>초대할 이메일</mat-label>
            <input matInput name="email" type="email" [(ngModel)]="email" maxlength="200" required />
          </mat-form-field>
          <button mat-stroked-button type="submit">초대</button>
        </form>
      }

      <h2>작업 커밋</h2>
      <form class="commit" (ngSubmit)="saveCommit()">
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>무엇을 고쳤는지</mat-label>
          <textarea matInput name="message" [(ngModel)]="message" maxlength="500" rows="2" required></textarea>
        </mat-form-field>
        <div class="sf-row">
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>구간</mat-label>
            <mat-select name="target" [(ngModel)]="target">
              <mat-option value="BIBLE">Story Bible</mat-option>
              <mat-option value="OUTLINE">Outline</mat-option>
              <mat-option value="CHAPTER">챕터</mat-option>
            </mat-select>
          </mat-form-field>
          @if (target === 'CHAPTER') {
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>장 번호</mat-label>
              <input matInput name="chapter" type="number" min="1" [(ngModel)]="chapterNumber" />
            </mat-form-field>
          }
        </div>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>고친 문장</mat-label>
          <textarea matInput name="quote" [(ngModel)]="quote" maxlength="2000" rows="3" placeholder="작업한 문장을 붙여 넣으면 클릭했을 때 그 구간이 표시됩니다."></textarea>
        </mat-form-field>
        <div class="sf-row">
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>시작</mat-label>
            <input matInput name="start" type="number" min="0" [(ngModel)]="startOffset" />
          </mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>끝</mat-label>
            <input matInput name="end" type="number" min="0" [(ngModel)]="endOffset" />
          </mat-form-field>
          <button mat-flat-button type="submit">커밋 남기기</button>
        </div>
      </form>
      <sf-commit-graph [commits]="commits()" [projectId]="ctx.projectId()" />
    </div>
  `,
  styles: `
    .hint {
      margin: 0 0 12px;
      font-size: 13px;
    }
    h2 {
      margin: 28px 0 8px;
      font-size: 18px;
    }
    .members {
      list-style: none;
      margin: 0 0 12px;
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 6px;
    }
    .members li {
      display: flex;
      gap: 8px;
      align-items: center;
    }
    .invite,
    .commit {
      display: flex;
      flex-direction: column;
      gap: 8px;
      max-width: 640px;
      margin-bottom: 12px;
    }
    .sf-row {
      align-items: center;
    }
  `,
})
export class ProjectActivityPage {
  protected readonly ctx = inject(ProjectContext);
  private readonly api = inject(CommunityApi);
  private readonly snack = inject(MatSnackBar);

  protected readonly graph = signal<WorkGraph | null>(null);
  protected readonly members = signal<Collaborator[]>([]);
  protected readonly commits = signal<WorkCommit[]>([]);
  protected readonly isOwner = signal(false);

  protected email = '';
  protected message = '';
  protected target: CommitTarget = 'CHAPTER';
  protected chapterNumber: number | null = 1;
  protected quote = '';
  protected startOffset: number | null = null;
  protected endOffset: number | null = null;

  constructor() {
    effect(() => {
      const id = this.ctx.projectId();
      if (id == null) return;
      this.api.projectGraph(id).subscribe((g) => this.graph.set(g));
      this.api.collaborators(id).subscribe((rows) => this.members.set(rows));
      this.api.commits(id).subscribe((rows) => this.commits.set(rows));
      this.isOwner.set(this.ctx.project()?.role !== 'COLLABORATOR');
    });
  }

  protected invite(): void {
    const id = this.ctx.projectId();
    const email = this.email.trim();
    if (id == null || !email) return;
    this.api.invite(id, email).subscribe({
      next: (row) => {
        this.members.update((list) => [...list, row]);
        this.email = '';
        this.snack.open('초대를 보냈습니다. 상대가 창작하기에서 수락하면 원고가 공유됩니다.', '닫기', { duration: 4000 });
      },
      error: (e) => this.snack.open(errorMessage(e), '닫기', { duration: 4000 }),
    });
  }

  protected saveCommit(): void {
    const id = this.ctx.projectId();
    const message = this.message.trim();
    if (id == null || !message) return;
    const quote = this.quote.trim();
    this.api.addCommit(id, {
      message,
      target: this.target,
      chapterNumber: this.target === 'CHAPTER' ? this.chapterNumber : null,
      quote: quote || null,
      startOffset: quote ? this.startOffset : null,
      endOffset: quote ? this.endOffset : null,
    }).subscribe({
      next: (row) => {
        this.commits.update((list) => [row, ...list]);
        this.message = '';
        this.quote = '';
        this.startOffset = null;
        this.endOffset = null;
        this.api.projectGraph(id).subscribe((g) => this.graph.set(g));
      },
      error: (e) => this.snack.open(errorMessage(e), '닫기', { duration: 4000 }),
    });
  }
}

import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { CommunityApi } from '../../core/api/community-api.service';
import { errorMessage } from '../../core/api/storyforge-api.service';
import { AuthService } from '../../core/auth/auth.service';
import { UserSummary } from '../../core/models/community';

const COLORS = ['#1f4e79', '#2a78d6', '#008300', '#6250d6', '#c2185b', '#b26a00', '#d95926', '#455a64'];

/** 계정 설정: 프로필, 비밀번호, 계정 삭제 */
@Component({
  selector: 'sf-account-settings-page',
  imports: [FormsModule, RouterLink, MatButtonModule, MatCardModule, MatFormFieldModule, MatIconModule, MatInputModule],
  template: `
    <div class="sf-page narrow">
      <h1>계정 설정</h1>
      <section class="preview" aria-label="프로필 미리보기">
        <span class="avatar" [style.background]="avatarColor">{{ displayName.trim().slice(0, 1) || '?' }}</span>
        <div>
          <strong>{{ displayName.trim() || '이름' }}</strong>
          <p class="sf-muted">{{ bio.trim() || '소개가 아직 없습니다.' }}</p>
        </div>
      </section>
      <mat-card appearance="outlined" class="card">
        <mat-card-header><mat-card-title>이메일 인증</mat-card-title></mat-card-header>
        <mat-card-content><p>{{ auth.user()?.emailVerified ? '인증된 이메일입니다.' : '아직 이메일을 인증하지 않았습니다.' }}</p></mat-card-content>
        @if (!auth.user()?.emailVerified) {
          <mat-card-actions><button mat-button [disabled]="busy()" (click)="verifyEmail()">인증 메일 요청</button></mat-card-actions>
        }
      </mat-card>
      <mat-card appearance="outlined" class="card">
        <mat-card-header>
          <mat-card-title>프로필</mat-card-title>
          <mat-card-subtitle>이름, 소개, 웹사이트는 공개 프로필에 보입니다. 이메일({{ auth.user()?.email }})은 공개되지 않습니다.</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content class="sf-form body">
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>이름</mat-label>
            <input matInput name="displayName" [(ngModel)]="displayName" maxlength="100" required />
          </mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>소개</mat-label>
            <textarea matInput name="bio" rows="3" [(ngModel)]="bio" maxlength="500"></textarea>
            <mat-hint align="end">{{ bio.length }}/500</mat-hint>
          </mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>웹사이트</mat-label>
            <input matInput name="website" type="url" [(ngModel)]="website" maxlength="300" placeholder="https://" />
          </mat-form-field>
          <div>
            <span class="sf-muted label">아바타 색</span>
            <div class="colors" role="radiogroup" aria-label="아바타 색">
              @for (c of colors; track c) {
                <button
                  type="button"
                  class="color"
                  role="radio"
                  [attr.aria-checked]="avatarColor === c"
                  [attr.aria-label]="'색 ' + c"
                  [class.on]="avatarColor === c"
                  [style.background]="c"
                  (click)="avatarColor = c"
                ></button>
              }
            </div>
          </div>
        </mat-card-content>
        <mat-card-actions align="end">
          <a mat-button [routerLink]="['/users', auth.user()?.id]">내 프로필 보기</a>
          <button mat-flat-button [disabled]="busy() || !displayName.trim()" (click)="saveProfile()"><mat-icon>save</mat-icon> 저장</button>
        </mat-card-actions>
      </mat-card>

      <mat-card appearance="outlined" class="card">
        <mat-card-header><mat-card-title>비밀번호 변경</mat-card-title></mat-card-header>
        <mat-card-content class="sf-form body">
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>현재 비밀번호</mat-label>
            <input matInput name="currentPassword" type="password" autocomplete="current-password" [(ngModel)]="currentPassword" />
          </mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>새 비밀번호 (8자 이상)</mat-label>
            <input matInput name="newPassword" type="password" autocomplete="new-password" maxlength="72" [(ngModel)]="newPassword" />
            <mat-hint>{{ newPassword.length }}/72</mat-hint>
          </mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>새 비밀번호 확인</mat-label>
            <input matInput name="confirmPassword" type="password" autocomplete="new-password" [(ngModel)]="confirmPassword" />
            @if (confirmPassword && confirmPassword !== newPassword) {
              <mat-error>비밀번호가 서로 다릅니다.</mat-error>
            }
          </mat-form-field>
        </mat-card-content>
        <mat-card-actions align="end">
          <button mat-stroked-button [disabled]="busy() || !currentPassword || newPassword.length < 8 || newPassword !== confirmPassword" (click)="savePassword()">변경</button>
        </mat-card-actions>
      </mat-card>

      <mat-card appearance="outlined" class="card">
        <mat-card-header>
          <mat-card-title>차단한 사용자</mat-card-title>
          <mat-card-subtitle>차단한 사람의 작품, 글, 댓글은 보이지 않고, 그 사람은 내 글에 댓글을 달 수 없습니다.</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content class="blocks">
          @for (u of blocked(); track u.id) {
            <div class="sf-row">
              <a [routerLink]="['/users', u.id]">{{ u.displayName }}</a>
              <span class="sf-spacer"></span>
              <button mat-button (click)="unblock(u.id)">차단 해제</button>
            </div>
          } @empty {
            <p class="sf-muted">차단한 사용자가 없습니다.</p>
          }
        </mat-card-content>
      </mat-card>

      <mat-card appearance="outlined" class="card">
        <mat-card-header>
          <mat-card-title>계정 삭제</mat-card-title>
          <mat-card-subtitle>프로젝트, 게시 작품, 댓글, 활동 기록이 모두 삭제되며 되돌릴 수 없습니다. 다른 사람이 Clone한 프로젝트는 남습니다.</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content class="sf-form body">
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>비밀번호 확인</mat-label>
            <input matInput name="deletePassword" type="password" autocomplete="current-password" [(ngModel)]="deletePassword" />
          </mat-form-field>
        </mat-card-content>
        <mat-card-actions align="end">
          <button mat-stroked-button class="danger" [disabled]="busy() || !deletePassword" (click)="remove()"><mat-icon>delete_forever</mat-icon> 계정 삭제</button>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: `
    .narrow {
      max-width: 720px;
    }
    .preview {
      display: flex;
      gap: 14px;
      align-items: center;
      margin-bottom: 16px;
      padding: 16px;
      border-radius: var(--sf-radius);
      border: 1px solid var(--sf-border);
      background: var(--sf-surface);
      box-shadow: var(--sf-shadow);
    }
    .avatar {
      width: 56px;
      height: 56px;
      flex: none;
      display: grid;
      place-items: center;
      border-radius: 50%;
      color: #fff;
      font-size: 22px;
      font-weight: 700;
    }
    .preview p {
      margin: 4px 0 0;
    }
    h1 {
      margin: 0 0 16px;
      font-size: 24px;
    }
    .card {
      background: var(--sf-surface);
      margin-bottom: 16px;
    }
    .body {
      margin-top: 16px;
      gap: 16px;
    }
    .label {
      font-size: 13px;
    }
    .blocks {
      margin-top: 8px;
    }
    .colors {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      margin-top: 6px;
    }
    .color {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      border: 2px solid transparent;
      outline: 1px solid var(--sf-border);
      cursor: pointer;
    }
    .color.on {
      outline: 3px solid var(--sf-accent);
    }
    .danger {
      color: var(--sf-severity-high);
    }
  `,
})
export class AccountSettingsPage {
  private readonly api = inject(CommunityApi);
  private readonly snack = inject(MatSnackBar);
  protected readonly auth = inject(AuthService);

  protected readonly colors = COLORS;
  protected readonly busy = signal(false);
  protected readonly blocked = signal<UserSummary[]>([]);
  protected displayName = this.auth.user()?.displayName ?? '';
  protected bio = this.auth.user()?.bio ?? '';
  protected website = this.auth.user()?.website ?? '';
  protected avatarColor = this.auth.user()?.avatarColor ?? null;
  protected currentPassword = '';
  protected newPassword = '';
  protected confirmPassword = '';
  protected deletePassword = '';

  constructor() {
    this.api.blockedUsers().subscribe((list) => this.blocked.set(list));
    // 세션에 저장한 값이 오래됐을 수 있어 서버 값으로 채웁니다.
    this.auth.refreshUser().subscribe((u) => {
      this.displayName = u.displayName;
      this.bio = u.bio ?? '';
      this.website = u.website ?? '';
      this.avatarColor = u.avatarColor ?? null;
    });
  }

  protected unblock(userId: number): void {
    this.api.block(userId, false).subscribe({
      next: () => this.blocked.update((list) => list.filter((u) => u.id !== userId)),
      error: (e) => this.fail(e),
    });
  }

  protected saveProfile(): void {
    this.busy.set(true);
    this.api
      .updateProfile({
        displayName: this.displayName.trim(),
        bio: this.bio.trim() || null,
        website: this.website.trim() || null,
        avatarColor: this.avatarColor,
      })
      .subscribe({
        next: (u) => {
          this.busy.set(false);
          this.auth.setUser(u);
          this.snack.open('저장했습니다.', undefined, { duration: 2000 });
        },
        error: (e) => this.fail(e),
      });
  }

  protected savePassword(): void {
    this.busy.set(true);
    this.api.changePassword({ currentPassword: this.currentPassword, newPassword: this.newPassword }).subscribe({
      next: () => {
        this.busy.set(false);
        this.currentPassword = '';
        this.newPassword = '';
        this.confirmPassword = '';
        this.snack.open('비밀번호를 바꿨습니다. 다시 로그인하세요.', undefined, { duration: 3000 });
        this.auth.logout();
      },
      error: (e) => this.fail(e),
    });
  }

  protected remove(): void {
    if (!confirm('계정을 삭제할까요? 프로젝트와 작품이 모두 지워지며 되돌릴 수 없습니다.')) return;
    this.busy.set(true);
    this.api.deleteAccount(this.deletePassword).subscribe({
      next: () => this.auth.logout(),
      error: (e) => this.fail(e),
    });
  }

  protected verifyEmail(): void {
    this.busy.set(true);
    this.auth.requestVerification().subscribe({
      next: r => { this.busy.set(false); this.snack.open(r.message, '닫기', { duration: 5000 }); },
      error: e => this.fail(e),
    });
  }

  private fail(e: unknown): void {
    this.busy.set(false);
    this.snack.open(errorMessage(e), '닫기', { duration: 4000 });
  }
}

import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CommunityApi } from '../../core/api/community-api.service';
import { errorMessage } from '../../core/api/storyforge-api.service';
import { SHARE_CHANNELS, ShareChannel } from '../../core/models/community';

export interface ShareDialogData {
  workId: number;
  title: string;
}

/** 공유: 어디에 공유하는지 고르면 전용 링크를 만들고 공유 그래프에 남깁니다. */
@Component({
  selector: 'sf-share-dialog',
  imports: [FormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatIconModule, MatInputModule, MatSelectModule],
  template: `
    <h2 mat-dialog-title>「{{ data.title }}」 공유</h2>
    <mat-dialog-content class="sf-form body">
      @if (!link()) {
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>채널</mat-label>
          <mat-select [(ngModel)]="channel" name="channel">
            @for (c of channels; track c.value) {
              <mat-option [value]="c.value">{{ c.label }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>어디에 공유하나요? (선택)</mat-label>
          <input matInput [(ngModel)]="target" name="target" maxlength="200" placeholder="예: 내 블로그, 소설 동호회" />
          <mat-hint>공유 그래프에 내 이름과 함께 공개됩니다. 이메일 주소 같은 개인정보는 적지 마세요.</mat-hint>
        </mat-form-field>
      } @else {
        <p>링크를 만들었습니다. 이 링크로 들어온 방문이 공유 그래프에 집계됩니다.</p>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>공유 링크</mat-label>
          <input matInput [value]="link()" readonly />
          <button matSuffix mat-icon-button type="button" aria-label="링크 복사" (click)="copy()"><mat-icon>content_copy</mat-icon></button>
        </mat-form-field>
        @if (externalUrl(); as url) {
          <a mat-stroked-button [href]="url" target="_blank" rel="noopener noreferrer"><mat-icon>open_in_new</mat-icon> {{ channelLabel() }}에서 열기</a>
        }
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="!!link()">{{ link() ? '닫기' : '취소' }}</button>
      @if (!link()) {
        <button mat-flat-button [disabled]="busy()" (click)="create()"><mat-icon>share</mat-icon> 링크 만들기</button>
      }
    </mat-dialog-actions>
  `,
  styles: `
    .body {
      gap: 16px;
      padding-top: 8px;
      min-width: min(420px, 80vw);
    }
  `,
})
export class ShareDialog {
  protected readonly data = inject<ShareDialogData>(MAT_DIALOG_DATA);
  private readonly api = inject(CommunityApi);
  private readonly snack = inject(MatSnackBar);

  protected readonly channels = SHARE_CHANNELS;
  protected readonly busy = signal(false);
  protected readonly link = signal('');
  protected readonly externalUrl = signal<string | null>(null);
  protected channel: ShareChannel = 'LINK';
  protected target = '';

  protected channelLabel(): string {
    return SHARE_CHANNELS.find((c) => c.value === this.channel)?.label ?? '';
  }

  protected create(): void {
    this.busy.set(true);
    this.api.share(this.data.workId, { channel: this.channel, target: this.target.trim() || null }).subscribe({
      next: (r) => {
        this.busy.set(false);
        const url = `${location.origin}/works/${this.data.workId}?s=${r.token}`;
        this.link.set(url);
        this.externalUrl.set(intentUrl(this.channel, url, this.data.title));
      },
      error: (e) => {
        this.busy.set(false);
        this.snack.open(errorMessage(e), '닫기', { duration: 4000 });
      },
    });
  }

  protected copy(): void {
    navigator.clipboard?.writeText(this.link()).then(
      () => this.snack.open('복사했습니다.', undefined, { duration: 2000 }),
      () => this.snack.open('복사하지 못했습니다. 직접 선택해 복사해 주세요.', '닫기', { duration: 4000 }),
    );
  }
}

function intentUrl(channel: ShareChannel, url: string, title: string): string | null {
  const u = encodeURIComponent(url);
  const t = encodeURIComponent(title);
  switch (channel) {
    case 'X':
      return `https://twitter.com/intent/tweet?url=${u}&text=${t}`;
    case 'FACEBOOK':
      return `https://www.facebook.com/sharer/sharer.php?u=${u}`;
    case 'EMAIL':
      return `mailto:?subject=${t}&body=${u}`;
    default:
      return null;
  }
}

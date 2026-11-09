import { HttpClient } from '@angular/common/http';
import { Component, inject, input, output, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { environment } from '../../../environments/environment';

/** 파일 선택, 끌어다 놓기, 클립보드 이미지로 표지를 올립니다. */
@Component({
  selector: 'sf-image-drop',
  imports: [MatIconModule],
  template: `
    <div
      class="drop"
      [class.over]="over()"
      (dragover)="onDrag($event, true)"
      (dragleave)="onDrag($event, false)"
      (drop)="onDrop($event)"
      (paste)="onPaste($event)"
      tabindex="0"
    >
      @if (url()) {
        <img [src]="url()!" alt="표지 미리보기" />
      } @else {
        <mat-icon>image</mat-icon>
        <p>이미지를 끌어다 놓거나, 복사한 그림을 붙여 넣거나, 파일을 고르세요.</p>
      }
      <input type="file" accept="image/jpeg,image/png,image/webp,image/gif" (change)="onFile($event)" />
      @if (error()) {
        <p class="err" role="alert">{{ error() }}</p>
      }
    </div>
  `,
  styles: `
    .drop {
      position: relative;
      display: grid;
      place-items: center;
      gap: 6px;
      min-height: 120px;
      padding: 12px;
      border: 1px dashed var(--sf-border);
      border-radius: 8px;
      background: var(--sf-surface-2);
      text-align: center;
      color: var(--sf-text-muted);
      font-size: 13px;
    }
    .drop.over {
      border-color: var(--sf-secondary);
    }
    img {
      max-width: 100%;
      max-height: 180px;
      object-fit: contain;
    }
    input {
      position: absolute;
      inset: 0;
      opacity: 0;
      cursor: pointer;
    }
    .err {
      color: #b42318;
    }
    p {
      margin: 0;
    }
  `,
})
export class ImageDropComponent {
  private readonly http = inject(HttpClient);
  readonly url = input<string | null>(null);
  readonly uploaded = output<string>();

  protected readonly over = signal(false);
  protected readonly error = signal<string | null>(null);

  protected onDrag(event: DragEvent, over: boolean): void {
    event.preventDefault();
    this.over.set(over);
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.over.set(false);
    const file = event.dataTransfer?.files?.[0];
    if (file) this.send(file);
  }

  protected onPaste(event: ClipboardEvent): void {
    const items = event.clipboardData?.items;
    if (!items) return;
    for (const item of items) {
      if (item.type.startsWith('image/')) {
        const file = item.getAsFile();
        if (file) this.send(file);
        event.preventDefault();
        return;
      }
    }
  }

  protected onFile(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) this.send(file);
  }

  private send(file: File): void {
    this.error.set(null);
    const body = new FormData();
    body.append('file', file);
    this.http.post<{ url: string }>(`${environment.apiBaseUrl}/media`, body).subscribe({
      next: (r) => this.uploaded.emit(r.url),
      error: () => this.error.set('이미지를 올리지 못했습니다. 5MB 이하 jpeg, png, webp, gif만 됩니다.'),
    });
  }
}

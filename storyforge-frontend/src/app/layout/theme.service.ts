import { DOCUMENT } from '@angular/common';
import { Injectable, effect, inject, signal } from '@angular/core';

export type ThemeMode = 'dark' | 'light';

const STORAGE_KEY = 'storyforge.theme';

/** Light / Dark 토글. 다크 모드가 기본입니다 (기획서 §9.1). */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  readonly mode = signal<ThemeMode>(this.restore());

  constructor() {
    effect(() => {
      const mode = this.mode();
      this.document.documentElement.style.colorScheme = mode;
      this.document.documentElement.dataset['theme'] = mode;
      try {
        localStorage.setItem(STORAGE_KEY, mode);
      } catch {
        /* 저장소를 쓸 수 없으면 무시 */
      }
    });
  }

  toggle(): void {
    this.mode.update((m) => (m === 'dark' ? 'light' : 'dark'));
  }

  private restore(): ThemeMode {
    try {
      return localStorage.getItem(STORAGE_KEY) === 'light' ? 'light' : 'dark';
    } catch {
      return 'dark';
    }
  }
}

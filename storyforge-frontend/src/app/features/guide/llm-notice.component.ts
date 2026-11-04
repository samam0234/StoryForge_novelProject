import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import { ModerationApi } from '../../core/api/moderation-api.service';

/** 외부 언어 모델로 원고가 나갈 때만 보여 줍니다. */
@Component({
  selector: 'sf-llm-notice',
  template: `
    @if (notice()?.external && notice()?.notice) {
      <p class="notice" role="note">{{ notice()!.notice }}</p>
    }
  `,
  styles: `
    .notice {
      margin: 0 0 12px;
      padding: 8px 12px;
      border: 1px solid var(--sf-border);
      border-radius: var(--sf-radius);
      background: var(--sf-surface);
      font-size: 13px;
      line-height: 1.5;
    }
  `,
})
export class LlmNoticeComponent {
  private readonly api = inject(ModerationApi);
  protected readonly notice = toSignal(this.api.llmNotice().pipe(catchError(() => of(null))), { initialValue: null });
}

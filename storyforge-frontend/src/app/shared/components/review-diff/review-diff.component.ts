import { Component, computed, input } from '@angular/core';
import { diffWordsWithSpace } from 'diff';

type Kind = 'same' | 'added' | 'removed' | 'modified';

interface Piece {
  kind: Kind;
  value: string;
}

/** 작업 앞뒤를 코드 리뷰처럼 표시합니다. 삭제 빨강, 추가 초록, 맞바뀐 구간 주황. */
@Component({
  selector: 'sf-review-diff',
  template: `
    <section class="review" aria-label="변경 비교">
      <h3>{{ summary() || '변경 비교' }}</h3>
      <p class="legend">
        <span class="removed">삭제</span>
        <span class="added">추가</span>
        <span class="modified">수정</span>
      </p>
      @if (!before() && !after()) {
        <p class="sf-muted">이 기록에는 비교할 본문이 없습니다. 이번 변경 이후에 고친 내용부터 색으로 남습니다.</p>
      } @else {
        <pre class="diff">@for (part of parts(); track $index) {<span [class]="part.kind">{{ part.value }}</span>}</pre>
      }
    </section>
  `,
  styles: `
    .review {
      margin-top: 12px;
      padding: 12px;
      border: 1px solid var(--sf-border);
      border-radius: 8px;
      background: var(--sf-surface);
    }
    h3 {
      margin: 0 0 8px;
      font-size: 14px;
    }
    .legend {
      display: flex;
      gap: 12px;
      font-size: 12px;
    }
    .legend span {
      padding: 0 4px;
      border-radius: 3px;
    }
    .diff {
      white-space: pre-wrap;
      font-family: inherit;
      line-height: 1.6;
    }
    .removed {
      background: #f8d0d0;
      color: #8a1f1f;
      text-decoration: line-through;
    }
    .added {
      background: #d7f5d7;
      color: #146214;
    }
    .modified {
      background: #ffe3c2;
      color: #8a4b08;
    }
  `,
})
export class ReviewDiffComponent {
  readonly summary = input('');
  readonly before = input<string | null>(null);
  readonly after = input<string | null>(null);

  protected readonly parts = computed(() => paint(this.before() ?? '', this.after() ?? ''));
}

function paint(before: string, after: string): Piece[] {
  const raw = diffWordsWithSpace(before, after);
  const out: Piece[] = [];
  for (let i = 0; i < raw.length; i++) {
    const current = raw[i];
    const next = raw[i + 1];
    if (current.removed && next?.added) {
      out.push({ kind: 'removed', value: current.value }, { kind: 'modified', value: next.value });
      i++;
      continue;
    }
    out.push({
      kind: current.added ? 'added' : current.removed ? 'removed' : 'same',
      value: current.value,
    });
  }
  return out;
}

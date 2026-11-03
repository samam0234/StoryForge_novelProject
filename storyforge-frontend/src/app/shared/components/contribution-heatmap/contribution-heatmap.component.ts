import { Component, computed, input, output } from '@angular/core';
import { Contributions } from '../../../core/models/community';

interface Cell {
  date: string;
  count: number;
  level: number;
  future: boolean;
}

const DAY_MS = 86_400_000;
const MONTHS = ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월'];

/**
 * 활동 잔디 (GitHub 방식): 활동이 많은 날일수록 진합니다. 칸을 누르면 그날 활동을 고릅니다.
 * 색만으로 구분하지 않도록 칸마다 건수를 레이블로 둡니다.
 */
@Component({
  selector: 'sf-contribution-heatmap',
  template: `
    <div class="scroll">
      <div class="grid-wrap">
        <div class="months" aria-hidden="true">
          @for (m of monthLabels(); track m.column) {
            <span [style.grid-column]="m.column + 1">{{ m.label }}</span>
          }
        </div>
        <div class="body">
          <div class="weekdays" aria-hidden="true">
            <span></span><span>월</span><span></span><span>수</span><span></span><span>금</span><span></span>
          </div>
          <div class="weeks" role="group" aria-label="최근 1년 활동">
            @for (week of weeks(); track $index) {
              <div class="week">
                @for (c of week; track c.date) {
                  @if (c.future) {
                    <span class="cell none"></span>
                  } @else {
                    <button
                      type="button"
                      class="cell"
                      [attr.data-level]="c.level"
                      [class.selected]="selected() === c.date"
                      [attr.aria-label]="c.date + ': 활동 ' + c.count + '건'"
                      [title]="c.date + ': 활동 ' + c.count + '건'"
                      (click)="pick.emit(c.date)"
                    ></button>
                  }
                }
              </div>
            }
          </div>
        </div>
      </div>
    </div>
    <div class="sf-row legend">
      <span class="sf-muted">최근 1년 활동 {{ data().total }}건 · 현재 연속 {{ data().currentStreak }}일 · 최장 연속 {{ data().longestStreak }}일</span>
      <span class="sf-spacer"></span>
      <span class="sf-muted">적음</span>
      @for (l of [0, 1, 2, 3, 4]; track l) {
        <span class="cell static" [attr.data-level]="l"></span>
      }
      <span class="sf-muted">많음</span>
    </div>
  `,
  styles: `
    :host {
      display: block;
    }
    .scroll {
      overflow-x: auto;
    }
    .grid-wrap {
      display: inline-block;
      min-width: max-content;
    }
    .months {
      display: grid;
      grid-template-columns: repeat(53, 14px);
      margin-left: 24px;
      font-size: 11px;
      color: var(--sf-text-muted);
      height: 16px;
    }
    .months span {
      white-space: nowrap;
    }
    .body {
      display: flex;
      gap: 4px;
    }
    .weekdays {
      display: grid;
      grid-template-rows: repeat(7, 14px);
      width: 20px;
      font-size: 10px;
      line-height: 14px;
      color: var(--sf-text-muted);
    }
    .weeks {
      display: flex;
    }
    .week {
      display: grid;
      grid-template-rows: repeat(7, 14px);
      width: 14px;
    }
    .cell {
      display: block;
      width: 11px;
      height: 11px;
      padding: 0;
      border: 0;
      border-radius: 2px;
      background: var(--sf-surface-2);
      outline: 1px solid color-mix(in srgb, var(--sf-text) 8%, transparent);
      outline-offset: -1px;
    }
    button.cell {
      cursor: pointer;
    }
    .cell.none {
      background: transparent;
      outline: none;
    }
    .cell[data-level='1'] {
      background: color-mix(in srgb, var(--sf-series-manual) 30%, var(--sf-surface-2));
    }
    .cell[data-level='2'] {
      background: color-mix(in srgb, var(--sf-series-manual) 55%, var(--sf-surface-2));
    }
    .cell[data-level='3'] {
      background: color-mix(in srgb, var(--sf-series-manual) 80%, var(--sf-surface-2));
    }
    .cell[data-level='4'] {
      background: var(--sf-series-manual);
    }
    .cell.selected,
    button.cell:focus-visible {
      outline: 2px solid var(--sf-accent);
      outline-offset: 0;
    }
    .legend {
      margin-top: 8px;
      font-size: 12px;
      gap: 4px;
    }
    .static {
      display: inline-block;
    }
  `,
})
export class ContributionHeatmapComponent {
  readonly data = input.required<Contributions>();
  readonly selected = input<string | null>(null);
  readonly pick = output<string>();

  protected readonly weeks = computed<Cell[][]>(() => {
    const d = this.data();
    const counts = new Map(d.days.map((x) => [x.date, x]));
    const start = Date.parse(`${d.from}T00:00:00Z`);
    const end = Date.parse(`${d.to}T00:00:00Z`);
    const weeks: Cell[][] = [];
    for (let t = start, i = 0; t <= end || i % 7 !== 0; t += DAY_MS, i++) {
      if (i % 7 === 0) weeks.push([]);
      const date = new Date(t).toISOString().slice(0, 10);
      const hit = counts.get(date);
      weeks[weeks.length - 1].push({ date, count: hit?.count ?? 0, level: hit?.level ?? 0, future: t > end });
    }
    return weeks;
  });

  protected readonly monthLabels = computed(() => {
    const labels: { column: number; label: string }[] = [];
    let last = -1;
    this.weeks().forEach((week, column) => {
      const month = Number(week[0].date.slice(5, 7)) - 1;
      if (month !== last && column < this.weeks().length - 1) {
        labels.push({ column, label: MONTHS[month] });
        last = month;
      }
    });
    return labels;
  });
}

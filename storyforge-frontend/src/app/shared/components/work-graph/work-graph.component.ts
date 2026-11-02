import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, input, signal } from '@angular/core';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { GraphDay, WorkGraph } from '../../../core/models/community';

type Range = '30' | '90' | 'all';

const DAY_MS = 86_400_000;

/**
 * 작업 그래프: 수동·에이전트 비율, 날짜별 막대(누르면 그날 작업만), 작업 기록 표.
 * 막대는 작업 횟수, 비율은 횟수와 변경 글자 수를 함께 보여 줍니다.
 */
@Component({
  selector: 'sf-work-graph',
  imports: [DatePipe, DecimalPipe, MatButtonToggleModule, MatIconModule],
  template: `
    <section class="ratio" aria-label="수동과 에이전트 작업 비율">
      <div class="ratio-row">
        <span class="ratio-label">작업 횟수</span>
        <div class="bar" role="img" [attr.aria-label]="'수동 ' + eventShare() + '%, 에이전트 ' + (100 - eventShare()) + '%'">
          @if (graph().manual.events) {
            <span class="manual" [style.flex-grow]="graph().manual.events"></span>
          }
          @if (graph().agent.events) {
            <span class="agent" [style.flex-grow]="graph().agent.events"></span>
          }
        </div>
        <span class="ratio-value">수동 {{ graph().manual.events | number }} · 에이전트 {{ graph().agent.events | number }}</span>
      </div>
      <div class="ratio-row">
        <span class="ratio-label">변경 글자</span>
        <div class="bar" role="img" [attr.aria-label]="'수동 ' + charShare() + '%, 에이전트 ' + (100 - charShare()) + '%'">
          @if (graph().manual.chars) {
            <span class="manual" [style.flex-grow]="graph().manual.chars"></span>
          }
          @if (graph().agent.chars) {
            <span class="agent" [style.flex-grow]="graph().agent.chars"></span>
          }
        </div>
        <span class="ratio-value">수동 {{ charShare() }}% · 에이전트 {{ 100 - charShare() }}%</span>
      </div>
    </section>

    <div class="sf-row toolbar">
      <div class="legend">
        <span><i class="swatch manual"></i> 수동</span>
        <span><i class="swatch agent"></i> 에이전트</span>
      </div>
      <span class="sf-spacer"></span>
      <mat-button-toggle-group hideSingleSelectionIndicator [value]="range()" (change)="setRange($event.value)" aria-label="기간">
        <mat-button-toggle value="30">30일</mat-button-toggle>
        <mat-button-toggle value="90">90일</mat-button-toggle>
        <mat-button-toggle value="all">전체</mat-button-toggle>
      </mat-button-toggle-group>
    </div>

    @if (!columns().length) {
      <div class="sf-empty">이 기간에는 작업 기록이 없습니다.</div>
    } @else {
      <div class="plot" role="group" aria-label="날짜별 작업 횟수. 막대를 누르면 그날 작업만 표에 나옵니다.">
        <div class="axis" aria-hidden="true">
          <span>{{ maxCount() }}</span>
          <span>0</span>
        </div>
        <div class="columns">
          @for (c of columns(); track c.date) {
            <button
              type="button"
              class="col"
              [class.selected]="selectedDay() === c.date"
              [class.empty]="!c.manual && !c.agent"
              [disabled]="!c.manual && !c.agent"
              [attr.aria-label]="c.date + ': 수동 ' + c.manual + '건, 에이전트 ' + c.agent + '건'"
              [attr.aria-pressed]="selectedDay() === c.date"
              (click)="toggleDay(c.date)"
              (mouseenter)="hovered.set(c)"
              (mouseleave)="hovered.set(null)"
              (focus)="hovered.set(c)"
              (blur)="hovered.set(null)"
            >
              <span class="stack">
                @if (c.agent) {
                  <span class="seg agent" [style.height.%]="(c.agent / maxCount()) * 100"></span>
                }
                @if (c.manual) {
                  <span class="seg manual" [style.height.%]="(c.manual / maxCount()) * 100"></span>
                }
              </span>
            </button>
          }
        </div>
        @if (hovered(); as h) {
          <div class="tooltip" role="status">
            <strong>{{ h.date }}</strong>
            <span><i class="swatch manual"></i> 수동 {{ h.manual }}건 ({{ h.manualChars | number }}자)</span>
            <span><i class="swatch agent"></i> 에이전트 {{ h.agent }}건 ({{ h.agentChars | number }}자)</span>
          </div>
        }
      </div>
      <div class="x-axis sf-muted" aria-hidden="true">
        <span>{{ columns()[0].date }}</span>
        <span>{{ columns()[columns().length - 1].date }}</span>
      </div>
    }

    <div class="sf-row table-head">
      <h4>
        @if (selectedDay(); as d) {
          {{ d }} 작업
        } @else {
          작업 기록
        }
        <span class="sf-muted">{{ rows().length | number }}건</span>
      </h4>
      <span class="sf-spacer"></span>
      @if (selectedDay()) {
        <button type="button" class="link" (click)="selectedDay.set(null)">전체 보기</button>
      }
    </div>
    <div class="table-wrap">
      <table class="sf-table">
        <thead>
          <tr>
            <th scope="col">시각</th>
            <th scope="col">주체</th>
            <th scope="col">작업</th>
            <th scope="col" class="num">변경 글자</th>
          </tr>
        </thead>
        <tbody>
          @for (e of rows(); track e.id) {
            <tr>
              <td class="time">{{ e.createdAt | date: 'yyyy-MM-dd HH:mm' }}</td>
              <td>
                <span class="actor">
                  <mat-icon inline>{{ e.actor === 'AGENT' ? 'smart_toy' : 'person' }}</mat-icon>
                  {{ e.actor === 'AGENT' ? '에이전트' : '수동' }}
                </span>
              </td>
              <td>{{ e.summary }}</td>
              <td class="num">{{ e.changedChars ? (e.changedChars | number) : '-' }}</td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
  styles: `
    :host {
      display: block;
    }
    .ratio {
      display: flex;
      flex-direction: column;
      gap: 8px;
      margin-bottom: 16px;
    }
    .ratio-row {
      display: grid;
      grid-template-columns: 64px minmax(80px, 1fr) minmax(170px, auto);
      gap: 8px;
      align-items: center;
      font-size: 13px;
    }
    .ratio-label {
      color: var(--sf-text-muted);
    }
    .bar {
      display: flex;
      gap: 2px;
      height: 10px;
      border-radius: 4px;
      overflow: hidden;
      background: var(--sf-surface-2);
    }
    .bar span {
      min-width: 2px;
      flex-basis: 0;
    }
    .manual {
      background: var(--sf-series-manual);
    }
    .agent {
      background: var(--sf-series-agent);
    }
    .legend {
      display: flex;
      gap: 12px;
      font-size: 13px;
    }
    .swatch {
      display: inline-block;
      width: 10px;
      height: 10px;
      border-radius: 3px;
      margin-right: 4px;
    }
    .toolbar {
      margin-bottom: 12px;
    }
    .plot {
      position: relative;
      display: flex;
      gap: 6px;
      height: 180px;
    }
    .axis {
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      font-size: 11px;
      color: var(--sf-text-muted);
      text-align: right;
      min-width: 20px;
    }
    .columns {
      flex: 1;
      min-width: 0;
      display: flex;
      align-items: stretch;
      gap: 1px;
      border-bottom: 1px solid var(--sf-border);
      overflow-x: auto;
    }
    .col {
      flex: 1 0 6px;
      max-width: 40px;
      padding: 0 1px;
      border: 0;
      background: transparent;
      cursor: pointer;
      display: flex;
      align-items: stretch;
    }
    .col.empty {
      cursor: default;
    }
    .col:not(.empty):hover,
    .col:focus-visible {
      background: var(--sf-surface-2);
      outline: none;
    }
    .col.selected {
      background: color-mix(in srgb, var(--sf-secondary) 18%, transparent);
    }
    .stack {
      flex: 1;
      display: flex;
      flex-direction: column;
      justify-content: flex-end;
      gap: 2px;
    }
    .seg {
      display: block;
      min-height: 3px;
    }
    .stack .seg:first-child {
      border-radius: 4px 4px 0 0;
    }
    .tooltip {
      position: absolute;
      top: 0;
      right: 0;
      display: flex;
      flex-direction: column;
      gap: 2px;
      padding: 8px 10px;
      font-size: 12px;
      border: 1px solid var(--sf-border);
      border-radius: 6px;
      background: var(--sf-surface);
      box-shadow: 0 2px 8px rgb(0 0 0 / 0.2);
      pointer-events: none;
    }
    .x-axis {
      display: flex;
      justify-content: space-between;
      font-size: 11px;
      margin: 4px 0 16px 26px;
    }
    .table-head h4 {
      margin: 0;
      font-size: 14px;
    }
    .table-head {
      margin-bottom: 8px;
    }
    .link {
      border: 0;
      background: none;
      color: var(--sf-emphasis);
      cursor: pointer;
      font: inherit;
      font-size: 13px;
    }
    .table-wrap {
      max-height: 360px;
      overflow: auto;
    }
    .time {
      white-space: nowrap;
      color: var(--sf-text-muted);
    }
    .actor {
      white-space: nowrap;
    }
    .num {
      text-align: right;
      white-space: nowrap;
    }
  `,
})
export class WorkGraphComponent {
  readonly graph = input.required<WorkGraph>();

  protected readonly range = signal<Range>('all');
  protected readonly selectedDay = signal<string | null>(null);
  protected readonly hovered = signal<GraphDay | null>(null);

  protected readonly eventShare = computed(() => share(this.graph().manual.events, this.graph().agent.events));
  protected readonly charShare = computed(() => share(this.graph().manual.chars, this.graph().agent.chars));

  /** 빈 날도 칸을 둡니다. 쉬는 기간이 그래프에 보여야 합니다. */
  protected readonly columns = computed<GraphDay[]>(() => {
    const days = this.graph().days;
    if (!days.length) return [];
    const byDate = new Map(days.map((d) => [d.date, d]));
    const last = toTime(todayOf(days[days.length - 1].date));
    const range = this.range();
    const first = range === 'all' ? toTime(days[0].date) : last - (Number(range) - 1) * DAY_MS;
    const out: GraphDay[] = [];
    for (let t = first; t <= last; t += DAY_MS) {
      const date = toDate(t);
      out.push(byDate.get(date) ?? { date, manual: 0, agent: 0, manualChars: 0, agentChars: 0 });
    }
    return out.some((d) => d.manual || d.agent) ? out : [];
  });

  protected readonly maxCount = computed(() => Math.max(1, ...this.columns().map((c) => c.manual + c.agent)));

  protected readonly rows = computed(() => {
    const day = this.selectedDay();
    const events = this.graph().events;
    if (day) return events.filter((e) => e.day === day);
    const first = this.columns()[0]?.date;
    return first ? events.filter((e) => e.day >= first) : [];
  });

  protected setRange(range: Range): void {
    this.range.set(range);
    this.selectedDay.set(null);
  }

  protected toggleDay(date: string): void {
    this.selectedDay.update((d) => (d === date ? null : date));
  }
}

function share(manual: number, agent: number): number {
  const total = manual + agent;
  return total ? Math.round((manual / total) * 100) : 0;
}

/** 서버 시간대의 날짜와 어긋나지 않도록 UTC 자정으로 계산합니다. */
function toTime(date: string): number {
  return Date.parse(`${date}T00:00:00Z`);
}

function toDate(time: number): string {
  return new Date(time).toISOString().slice(0, 10);
}

/** 마지막 기록일과 오늘 중 늦은 날을 끝으로 씁니다. */
function todayOf(lastDay: string): string {
  const now = new Date();
  const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
  return today > lastDay ? today : lastDay;
}

import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SHARE_CHANNELS, ShareChannel, ShareGraph, labelOf } from '../../../core/models/community';

const ROW = 34;
const TOP = 8;
const WIDTH = 640;
const MAX_USERS = 10;

interface Node {
  key: string;
  label: string;
  count: number;
  y: number;
  color: string;
}

/**
 * 공유 그래프: 왼쪽 유저 → 오른쪽 채널로 이어지는 선(굵을수록 많이 공유)과, 누가 어디에 공유했는지 표.
 * 채널 색은 순위가 아니라 채널에 고정합니다.
 */
@Component({
  selector: 'sf-share-graph',
  imports: [DatePipe, DecimalPipe, RouterLink],
  template: `
    <div class="sf-row summary">
      <span><strong>{{ graph().total | number }}</strong> 번 공유</span>
      <span><strong>{{ graph().visits | number }}</strong> 번 방문</span>
    </div>
    @if (!graph().total) {
      <div class="sf-empty">아직 공유 기록이 없습니다.</div>
    } @else {
      <div class="flow">
        <svg [attr.viewBox]="'0 0 ' + width + ' ' + height()" role="img" aria-label="유저별 공유 채널 연결. 아래 표에 같은 내용이 있습니다.">
          @for (l of lines(); track l.key) {
            <path
              [attr.d]="l.path"
              fill="none"
              [attr.stroke]="l.color"
              [attr.stroke-width]="l.weight"
              stroke-opacity="0.55"
              stroke-linecap="round"
            >
              <title>{{ l.title }}</title>
            </path>
          }
          @for (n of users(); track n.key) {
            <circle cx="172" [attr.cy]="n.y" r="5" fill="var(--sf-text-muted)" />
            <text x="160" [attr.y]="n.y + 4" text-anchor="end" class="label">{{ n.label }}</text>
          }
          @for (n of channels(); track n.key) {
            <circle [attr.cx]="width - 172" [attr.cy]="n.y" r="5" [attr.fill]="n.color" />
            <text [attr.x]="width - 160" [attr.y]="n.y + 4" class="label">{{ n.label }} {{ n.count }}</text>
          }
        </svg>
      </div>
      @if (hiddenUsers()) {
        <p class="sf-muted note">유저 {{ hiddenUsers() }}명은 그래프에서 '그 외'로 묶었습니다. 표에는 모두 나옵니다.</p>
      }
      <div class="table-wrap">
        <table class="sf-table">
          <thead>
            <tr>
              <th scope="col">시각</th>
              <th scope="col">누가</th>
              <th scope="col">채널</th>
              <th scope="col">어디에</th>
              <th scope="col" class="num">방문</th>
            </tr>
          </thead>
          <tbody>
            @for (s of graph().items; track s.id) {
              <tr>
                <td class="time">{{ s.createdAt | date: 'yyyy-MM-dd HH:mm' }}</td>
                <td><a [routerLink]="['/users', s.user.id]">{{ s.user.displayName }}</a></td>
                <td>{{ channelLabel(s.channel) }}</td>
                <td>{{ s.target || '-' }}</td>
                <td class="num">{{ s.visitCount | number }}</td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    }
  `,
  styles: `
    :host {
      display: block;
    }
    .summary {
      gap: 16px;
      margin-bottom: 12px;
      font-size: 14px;
    }
    .flow {
      overflow-x: auto;
    }
    svg {
      display: block;
      width: 100%;
      max-width: 720px;
      min-width: 480px;
      margin: 0 auto;
    }
    .label {
      font-size: 13px;
      fill: var(--sf-text);
    }
    .note {
      font-size: 12px;
    }
    .table-wrap {
      margin-top: 12px;
      max-height: 320px;
      overflow: auto;
    }
    .time {
      white-space: nowrap;
      color: var(--sf-text-muted);
    }
    .num {
      text-align: right;
    }
  `,
})
export class ShareGraphComponent {
  readonly graph = input.required<ShareGraph>();
  protected readonly width = WIDTH;

  private readonly topUsers = computed(() => {
    const counts = new Map<number, { label: string; count: number }>();
    for (const s of this.graph().items) {
      const entry = counts.get(s.user.id) ?? { label: s.user.displayName, count: 0 };
      entry.count++;
      counts.set(s.user.id, entry);
    }
    return [...counts.entries()].sort((a, b) => b[1].count - a[1].count);
  });

  protected readonly hiddenUsers = computed(() => Math.max(0, this.topUsers().length - MAX_USERS));

  protected readonly users = computed<Node[]>(() => {
    const top = this.topUsers();
    const shown = top.slice(0, MAX_USERS).map(([id, e], i) => ({
      key: String(id),
      label: truncate(e.label),
      count: e.count,
      y: TOP + ROW * i + ROW / 2,
      color: '',
    }));
    if (top.length > MAX_USERS) {
      const rest = top.slice(MAX_USERS).reduce((sum, [, e]) => sum + e.count, 0);
      shown.push({ key: 'other', label: '그 외', count: rest, y: TOP + ROW * MAX_USERS + ROW / 2, color: '' });
    }
    return shown;
  });

  protected readonly channels = computed<Node[]>(() => {
    const counts = new Map<ShareChannel, number>();
    for (const s of this.graph().items) counts.set(s.channel, (counts.get(s.channel) ?? 0) + 1);
    return SHARE_CHANNELS.filter((c) => counts.has(c.value)).map((c, i) => ({
      key: c.value,
      label: c.label,
      count: counts.get(c.value) ?? 0,
      y: TOP + ROW * i + ROW / 2,
      color: channelColor(c.value),
    }));
  });

  protected readonly height = computed(() => TOP * 2 + ROW * Math.max(this.users().length, this.channels().length, 1));

  protected readonly lines = computed(() => {
    const users = new Map(this.users().map((u) => [u.key, u]));
    const channels = new Map(this.channels().map((c) => [c.key, c]));
    const weights = new Map<string, { user: Node; channel: Node; count: number }>();
    for (const s of this.graph().items) {
      const user = users.get(String(s.user.id)) ?? users.get('other');
      const channel = channels.get(s.channel);
      if (!user || !channel) continue;
      const key = `${user.key}>${channel.key}`;
      const entry = weights.get(key) ?? { user, channel, count: 0 };
      entry.count++;
      weights.set(key, entry);
    }
    const x1 = 178;
    const x2 = WIDTH - 178;
    const mid = (x1 + x2) / 2;
    return [...weights.entries()].map(([key, l]) => ({
      key,
      color: l.channel.color,
      weight: Math.min(2 + l.count, 10),
      path: `M ${x1} ${l.user.y} C ${mid} ${l.user.y}, ${mid} ${l.channel.y}, ${x2} ${l.channel.y}`,
      title: `${l.user.label} → ${l.channel.label}: ${l.count}번`,
    }));
  });

  protected channelLabel(c: ShareChannel): string {
    return labelOf(SHARE_CHANNELS, c);
  }
}

function truncate(s: string): string {
  return s.length > 12 ? `${s.slice(0, 11)}…` : s;
}

function channelColor(c: ShareChannel): string {
  return `var(--sf-series-${SHARE_CHANNELS.findIndex((o) => o.value === c) + 1})`;
}

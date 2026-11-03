import { Component, computed, input } from '@angular/core';
import { LINK_TYPES, LinkType, PackageItemView, PackageLinkView, labelOf } from '../../../core/models/community';

const SIZE = 680;
const R = 190;
const NODE_R = 9;

/** 패키지 세계관 지도: 작품을 원형으로 놓고 연결을 선으로 잇습니다. 독서 순서대로 시계 방향입니다. */
@Component({
  selector: 'sf-package-map',
  template: `
    <div class="map">
      <svg [attr.viewBox]="'0 0 ' + size + ' ' + size" role="img" aria-label="패키지 작품 연결 지도. 아래 목록에 같은 내용이 있습니다.">
        <defs>
          <marker id="sf-arrow" viewBox="0 0 10 10" refX="10" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
            <path d="M 0 0 L 10 5 L 0 10 z" fill="var(--sf-text-muted)" />
          </marker>
        </defs>
        @for (e of edges(); track e.id) {
          <line [attr.x1]="e.x1" [attr.y1]="e.y1" [attr.x2]="e.x2" [attr.y2]="e.y2" stroke="var(--sf-text-muted)" stroke-width="1.5" marker-end="url(#sf-arrow)" />
          <text [attr.x]="e.mx" [attr.y]="e.my" text-anchor="middle" class="edge-label">{{ e.text }}</text>
        }
        @for (n of nodes(); track n.id) {
          <circle [attr.cx]="n.x" [attr.cy]="n.y" [attr.r]="nodeR" class="node" />
          <text [attr.x]="n.x" [attr.y]="n.y + 4" text-anchor="middle" class="order">{{ n.order }}</text>
          <text [attr.x]="n.lx" [attr.y]="n.ly" [attr.text-anchor]="n.anchor" class="label">{{ n.title }}<title>{{ n.fullTitle }}</title></text>
        }
      </svg>
    </div>
    @if (links().length) {
      <ul class="list">
        @for (e of edges(); track e.id) {
          <li>{{ e.from }} → {{ e.to }}: {{ e.text }}</li>
        }
      </ul>
    } @else {
      <p class="sf-muted note">아직 연결이 없습니다.</p>
    }
  `,
  styles: `
    :host {
      display: block;
    }
    .map {
      max-width: 680px;
      margin: 0 auto;
    }
    svg {
      display: block;
      width: 100%;
    }
    .node {
      fill: var(--sf-primary);
    }
    .order {
      font-size: 10px;
      font-weight: 700;
      fill: #fff;
    }
    .label {
      font-size: 13px;
      font-weight: 600;
      fill: var(--sf-text);
    }
    .edge-label {
      font-size: 11px;
      fill: var(--sf-text-muted);
      paint-order: stroke;
      stroke: var(--sf-surface);
      stroke-width: 4px;
    }
    .list {
      margin: 8px 0 0;
      padding-left: 18px;
      font-size: 13px;
    }
    .note {
      font-size: 13px;
      text-align: center;
    }
  `,
})
export class PackageMapComponent {
  readonly items = input.required<PackageItemView[]>();
  readonly links = input.required<PackageLinkView[]>();
  protected readonly size = SIZE;
  protected readonly nodeR = NODE_R;

  protected readonly nodes = computed(() => {
    const items = this.items();
    const c = SIZE / 2;
    return items.map((item, i) => {
      const angle = (2 * Math.PI * i) / Math.max(items.length, 1) - Math.PI / 2;
      const cos = Math.cos(angle);
      const sin = Math.sin(angle);
      const title = item.work.title.length > 12 ? `${item.work.title.slice(0, 11)}…` : item.work.title;
      return {
        id: item.id,
        order: i + 1,
        fullTitle: item.work.title,
        title,
        x: c + R * cos,
        y: c + R * sin,
        lx: c + (R + 16) * cos,
        ly: c + (R + 16) * sin + 4 + (Math.abs(cos) < 0.3 ? (sin > 0 ? 10 : -6) : 0),
        anchor: Math.abs(cos) < 0.3 ? 'middle' : cos > 0 ? 'start' : 'end',
      };
    });
  });

  protected readonly edges = computed(() => {
    const byId = new Map(this.nodes().map((n) => [n.id, n]));
    return this.links().flatMap((l) => {
      const a = byId.get(l.fromItemId);
      const b = byId.get(l.toItemId);
      if (!a || !b) return [];
      const dx = b.x - a.x;
      const dy = b.y - a.y;
      const len = Math.hypot(dx, dy) || 1;
      const pad = NODE_R + 3;
      return [
        {
          id: l.id,
          from: a.fullTitle,
          to: b.fullTitle,
          x1: a.x + (dx / len) * pad,
          y1: a.y + (dy / len) * pad,
          x2: b.x - (dx / len) * pad,
          y2: b.y - (dy / len) * pad,
          mx: (a.x + b.x) / 2,
          my: (a.y + b.y) / 2,
          text: l.label || this.typeLabel(l.type),
        },
      ];
    });
  });

  private typeLabel(t: LinkType): string {
    return labelOf(LINK_TYPES, t);
  }
}

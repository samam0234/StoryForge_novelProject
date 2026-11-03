import { DatePipe } from '@angular/common';
import { Component, computed, inject, input } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CLONE_MODES, CloneMode, CloneNetwork, NetworkNode, labelOf } from '../../../core/models/community';

const NODE_W = 210;
const NODE_H = 52;
const GAP_X = 40;
const GAP_Y = 14;

interface Placed extends NetworkNode {
  x: number;
  y: number;
}

/**
 * Clone 네트워크: 원작에서 파생된 게시작 트리와 Clone 기록.
 * 공유 그래프가 "어디로 퍼졌나"라면 이 그래프는 "어떤 작품으로 갈라졌나"입니다.
 */
@Component({
  selector: 'sf-clone-network',
  imports: [DatePipe, RouterLink],
  template: `
    <div class="sf-row summary">
      <span>이 작품 Clone 기록</span>
      <span class="sf-badge">세계관 {{ network().world }}</span>
      <span class="sf-badge">플롯 {{ network().plot }}</span>
      <span class="sf-badge">전체 원고 {{ network().full }}</span>
    </div>
    <div class="tree">
      <svg [attr.width]="size().w" [attr.height]="size().h" role="img" aria-label="원작과 게시된 파생작 트리. 노드를 누르면 작품으로 이동합니다.">
        @for (e of edges(); track e.key) {
          <path [attr.d]="e.path" fill="none" stroke="var(--sf-border)" stroke-width="2" />
        }
        @for (n of placed(); track n.workId) {
          <a [attr.href]="'/works/' + n.workId" (click)="open($event, n.workId)">
            <rect
              [attr.x]="n.x"
              [attr.y]="n.y"
              [attr.width]="nodeW"
              [attr.height]="nodeH"
              rx="8"
              class="node"
              [class.current]="n.current"
            />
            <text [attr.x]="n.x + 10" [attr.y]="n.y + 21" class="title">{{ clip(n.title) }}</text>
            <text [attr.x]="n.x + 10" [attr.y]="n.y + 39" class="sub">{{ clip(n.author.displayName, 12) }} · r{{ n.revision }} · ♥{{ n.likeCount }}</text>
          </a>
        }
      </svg>
    </div>
    @if (placed().length === 1) {
      <p class="sf-muted note">아직 게시된 파생작이 없습니다. Clone한 프로젝트를 게시하면 여기에 이어집니다.</p>
    }
    @if (network().clones.length) {
      <div class="table-wrap">
        <table class="sf-table">
          <thead>
            <tr>
              <th scope="col">시각</th>
              <th scope="col">누가</th>
              <th scope="col">범위</th>
              <th scope="col">기준 리비전</th>
              <th scope="col">파생작</th>
            </tr>
          </thead>
          <tbody>
            @for (c of network().clones; track $index) {
              <tr>
                <td class="time">{{ c.createdAt | date: 'yyyy-MM-dd HH:mm' }}</td>
                <td><a [routerLink]="['/users', c.user.id]">{{ c.user.displayName }}</a></td>
                <td>{{ modeLabel(c.mode) }}</td>
                <td>r{{ c.revision }}</td>
                <td>
                  @if (c.derivedWorkId) {
                    <a [routerLink]="['/works', c.derivedWorkId]">보기</a>
                  } @else {
                    <span class="sf-muted">작업 중</span>
                  }
                </td>
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
      margin-bottom: 12px;
      font-size: 14px;
    }
    .tree {
      overflow-x: auto;
      padding-bottom: 8px;
    }
    .node {
      fill: var(--sf-surface-2);
      stroke: var(--sf-border);
    }
    .node.current {
      stroke: var(--sf-emphasis);
      stroke-width: 2;
    }
    .title {
      font-size: 13px;
      font-weight: 600;
      fill: var(--sf-text);
    }
    .sub {
      font-size: 11px;
      fill: var(--sf-text-muted);
    }
    .note {
      font-size: 13px;
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
  `,
})
export class CloneNetworkComponent {
  private readonly router = inject(Router);
  readonly network = input.required<CloneNetwork>();
  protected readonly nodeW = NODE_W;
  protected readonly nodeH = NODE_H;

  /** 가로로 깊이, 세로로 잎 순서대로 놓는 단순 트리 배치 */
  protected readonly placed = computed<Placed[]>(() => {
    const nodes = this.network().nodes;
    const children = new Map<number, NetworkNode[]>();
    for (const n of nodes) {
      if (n.parentId != null) children.set(n.parentId, [...(children.get(n.parentId) ?? []), n]);
    }
    const out: Placed[] = [];
    let row = 0;
    const walk = (n: NetworkNode, depth: number): number => {
      const kids = children.get(n.workId) ?? [];
      let y: number;
      if (!kids.length) {
        y = row++ * (NODE_H + GAP_Y);
      } else {
        const ys = kids.map((k) => walk(k, depth + 1));
        y = (ys[0] + ys[ys.length - 1]) / 2;
      }
      out.push({ ...n, x: depth * (NODE_W + GAP_X), y });
      return y;
    };
    const root = nodes.find((n) => n.workId === this.network().rootId);
    if (root) walk(root, 0);
    return out;
  });

  protected readonly edges = computed(() => {
    const byId = new Map(this.placed().map((n) => [n.workId, n]));
    return this.placed()
      .filter((n) => n.parentId != null && byId.has(n.parentId))
      .map((n) => {
        const p = byId.get(n.parentId as number) as Placed;
        const x1 = p.x + NODE_W;
        const y1 = p.y + NODE_H / 2;
        const x2 = n.x;
        const y2 = n.y + NODE_H / 2;
        const mid = (x1 + x2) / 2;
        return { key: n.workId, path: `M ${x1} ${y1} C ${mid} ${y1}, ${mid} ${y2}, ${x2} ${y2}` };
      });
  });

  protected readonly size = computed(() => {
    const nodes = this.placed();
    return {
      w: Math.max(...nodes.map((n) => n.x + NODE_W), NODE_W) + 2,
      h: Math.max(...nodes.map((n) => n.y + NODE_H), NODE_H) + 2,
    };
  });

  /** 새로고침 없이 이동합니다. */
  protected open(event: Event, workId: number): void {
    event.preventDefault();
    this.router.navigate(['/works', workId]);
  }

  protected clip(s: string, max = 20): string {
    return s.length > max ? `${s.slice(0, max - 1)}…` : s;
  }

  protected modeLabel(m: CloneMode): string {
    return labelOf(CLONE_MODES, m);
  }
}

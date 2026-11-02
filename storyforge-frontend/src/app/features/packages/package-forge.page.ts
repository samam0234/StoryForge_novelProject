import { CdkDrag, CdkDragDrop, CdkDragHandle, CdkDropList, moveItemInArray } from '@angular/cdk/drag-drop';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { CommunityApi } from '../../core/api/community-api.service';
import { errorMessage } from '../../core/api/storyforge-api.service';
import {
  GENRES,
  ITEM_ROLES,
  ItemRole,
  LINK_TYPES,
  LinkType,
  PACKAGE_TYPES,
  PackageType,
  VISIBILITY_OPTIONS,
  Visibility,
  WorkCard,
  WorkDetail,
} from '../../core/models/community';
import { PackageMapComponent } from '../../shared/components/package-map/package-map.component';

/**
 * 패키지포지: 내가 게시한 작품을 하나의 세계로 묶습니다.
 * workId가 없으면 새로 만들고, 있으면 구성, 독서 순서, 연결 지도를 편집합니다.
 */
@Component({
  selector: 'sf-package-forge-page',
  imports: [
    CdkDrag,
    CdkDragHandle,
    CdkDropList,
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    PackageMapComponent,
  ],
  template: `
    <div class="sf-page">
      <h1>패키지포지</h1>
      <p class="sf-muted">단독 작품(롱품, 숏품)을 묶어 연결된 세계를 만듭니다. 내 작품은 바로 들어가고, 다른 작가의 작품은 그 작가가 승인하면 들어옵니다.</p>

      <section class="panel">
        <h2>유형</h2>
        <div class="types" role="radiogroup" aria-label="패키지 유형">
          @for (t of types; track t.value) {
            <button type="button" class="type" role="radio" [attr.aria-checked]="packageType === t.value" [class.on]="packageType === t.value" (click)="packageType = t.value">
              <mat-icon>{{ t.icon }}</mat-icon>
              <strong>{{ t.label }}</strong>
              <span>{{ t.hint }}</span>
            </button>
          }
        </div>
        <div class="sf-form body">
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>패키지 제목</mat-label>
            <input matInput name="title" [(ngModel)]="title" maxlength="200" required />
          </mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>소개</mat-label>
            <textarea matInput name="synopsis" rows="2" [(ngModel)]="synopsis" maxlength="2000"></textarea>
          </mat-form-field>
          <div class="two">
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>장르</mat-label>
              <mat-select name="genre" [(ngModel)]="genre">
                <mat-option value="">정하지 않음</mat-option>
                @for (g of genres; track g) {
                  <mat-option [value]="g">{{ g }}</mat-option>
                }
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>공개 범위</mat-label>
              <mat-select name="visibility" [(ngModel)]="visibility">
                @for (v of visibilities; track v.value) {
                  <mat-option [value]="v.value">{{ v.label }}</mat-option>
                }
              </mat-select>
            </mat-form-field>
          </div>
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>태그 (쉼표로 구분)</mat-label>
            <input matInput name="tags" [(ngModel)]="tags" maxlength="300" />
          </mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>공용 세계관 노트</mat-label>
            <textarea matInput name="worldNote" rows="4" [(ngModel)]="worldNote" maxlength="20000" placeholder="작품들이 공유하는 규칙, 연표, 장소"></textarea>
          </mat-form-field>
        </div>
        @if (!detail()) {
          <h2>묶을 내 작품</h2>
          @if (!mine().length) {
            <div class="sf-empty">
              게시한 작품이 있어야 패키지를 만들 수 있습니다.
              <a routerLink="/create">창작하기</a>
            </div>
          }
          <ul class="picks">
            @for (w of mine(); track w.id) {
              <li>
                <mat-checkbox [checked]="picked().has(w.id)" (change)="togglePick(w.id)">
                  {{ w.title }} <span class="sf-muted">({{ w.kind === 'LONG' ? '롱품' : '숏품' }}, {{ w.chapterCount }}장)</span>
                </mat-checkbox>
              </li>
            }
          </ul>
          <button mat-flat-button [disabled]="busy() || !title.trim() || !picked().size" (click)="create()"><mat-icon>hub</mat-icon> 패키지 만들기</button>
        } @else {
          <button mat-stroked-button [disabled]="busy() || !title.trim()" (click)="saveMeta()"><mat-icon>save</mat-icon> 정보 저장</button>
        }
      </section>

      @if (detail(); as d) {
        <section class="panel">
          <div class="sf-row">
            <h2>독서 순서</h2>
            <span class="sf-spacer"></span>
            <a mat-button [routerLink]="['/works', d.card.id]">패키지 보기 <mat-icon iconPositionEnd>open_in_new</mat-icon></a>
          </div>
          <ol class="items" cdkDropList (cdkDropListDropped)="drop($event)">
            @for (i of items(); track i.id) {
              <li cdkDrag>
                <button mat-icon-button cdkDragHandle aria-label="끌어서 순서 바꾸기"><mat-icon>drag_indicator</mat-icon></button>
                <div class="item-main">
                  <a [routerLink]="['/works', i.work.id]">{{ i.work.title }}</a>
                  <span class="sf-muted by">{{ i.work.author.displayName }}</span>
                  @if (i.status === 'PENDING') {
                    <span class="sf-badge pending">승인 대기</span>
                  }
                </div>
                <mat-form-field appearance="outline" subscriptSizing="dynamic" class="role">
                  <mat-label>역할</mat-label>
                  <mat-select [ngModel]="i.role" (ngModelChange)="setRole(i.id, $event, i.note)" [name]="'role' + i.id">
                    @for (r of roles; track r.value) {
                      <mat-option [value]="r.value">{{ r.label }}</mat-option>
                    }
                  </mat-select>
                </mat-form-field>
                <button mat-icon-button aria-label="패키지에서 빼기" (click)="removeItem(i.id)"><mat-icon>close</mat-icon></button>
              </li>
            }
          </ol>

          <h3>작품 추가</h3>
          <div class="add">
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>내 작품</mat-label>
              <mat-select name="addMine" [(ngModel)]="addMine">
                @for (w of addable(); track w.id) {
                  <mat-option [value]="w.id">{{ w.title }}</mat-option>
                }
              </mat-select>
            </mat-form-field>
            <button mat-stroked-button [disabled]="!addMine" (click)="addItem(addMine!)">추가</button>
          </div>
          <form class="add" (ngSubmit)="searchOthers()">
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>다른 작가의 작품 검색</mat-label>
              <input matInput name="otherQ" [(ngModel)]="otherQ" maxlength="100" />
            </mat-form-field>
            <button mat-stroked-button type="submit">검색</button>
          </form>
          <ul class="results">
            @for (w of others(); track w.id) {
              <li class="sf-row">
                <a [routerLink]="['/works', w.id]">{{ w.title }}</a>
                <span class="sf-muted">{{ w.author.displayName }}</span>
                <span class="sf-spacer"></span>
                <button mat-button (click)="addItem(w.id)">편입 요청</button>
              </li>
            }
          </ul>
        </section>

        <section class="panel">
          <h2>연결 지도</h2>
          <sf-package-map [items]="items()" [links]="d.packageView?.links ?? []" />
          <h3>연결 추가</h3>
          <div class="links">
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>어느 작품에서</mat-label>
              <mat-select name="linkFrom" [(ngModel)]="linkFrom">
                @for (i of items(); track i.id) {
                  <mat-option [value]="i.id">{{ i.work.title }}</mat-option>
                }
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>어느 작품으로</mat-label>
              <mat-select name="linkTo" [(ngModel)]="linkTo">
                @for (i of items(); track i.id) {
                  <mat-option [value]="i.id">{{ i.work.title }}</mat-option>
                }
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>관계</mat-label>
              <mat-select name="linkType" [(ngModel)]="linkType">
                @for (l of linkTypes; track l.value) {
                  <mat-option [value]="l.value">{{ l.label }}</mat-option>
                }
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>설명 (선택)</mat-label>
              <input matInput name="linkLabel" [(ngModel)]="linkLabel" maxlength="200" placeholder="예: 3장 사건의 뒷이야기" />
            </mat-form-field>
            <button mat-stroked-button [disabled]="!linkFrom || !linkTo || linkFrom === linkTo" (click)="addLink()">연결</button>
          </div>
          <ul class="results">
            @for (l of d.packageView?.links ?? []; track l.id) {
              <li class="sf-row">
                <span>{{ itemTitle(l.fromItemId) }} → {{ itemTitle(l.toItemId) }}: {{ l.label || linkLabelOf(l.type) }}</span>
                <span class="sf-spacer"></span>
                <button mat-icon-button aria-label="연결 삭제" (click)="removeLink(l.id)"><mat-icon>close</mat-icon></button>
              </li>
            }
          </ul>
        </section>
      }
    </div>
  `,
  styles: `
    h1 {
      margin: 0 0 4px;
      font-size: 24px;
    }
    h2 {
      font-size: 17px;
      margin: 0 0 10px;
    }
    h3 {
      font-size: 14px;
      margin: 16px 0 8px;
    }
    .panel {
      margin-top: 16px;
      padding: 16px;
      border: 1px solid var(--sf-border);
      border-radius: var(--sf-radius);
      background: var(--sf-surface);
    }
    .types {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
      gap: 8px;
      margin-bottom: 16px;
    }
    .type {
      display: flex;
      flex-direction: column;
      gap: 4px;
      align-items: flex-start;
      text-align: left;
      padding: 10px;
      font: inherit;
      color: var(--sf-text);
      border: 1px solid var(--sf-border);
      border-radius: 8px;
      background: transparent;
      cursor: pointer;
    }
    .type span {
      font-size: 12px;
      color: var(--sf-text-muted);
    }
    .type.on {
      border-color: var(--sf-emphasis);
      background: color-mix(in srgb, var(--sf-secondary) 12%, transparent);
    }
    .body {
      gap: 12px;
      margin-bottom: 16px;
    }
    .two {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 12px;
    }
    .picks,
    .items,
    .results {
      list-style: none;
      margin: 0 0 12px;
      padding: 0;
    }
    .items li {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 6px;
      border: 1px solid var(--sf-border);
      border-radius: 8px;
      background: var(--sf-surface);
      margin-bottom: 6px;
    }
    .item-main {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      align-items: center;
    }
    .by {
      font-size: 12px;
    }
    .pending {
      background: color-mix(in srgb, var(--sf-accent) 25%, transparent);
    }
    .role {
      width: 130px;
    }
    .add {
      display: flex;
      gap: 8px;
      align-items: center;
      margin-bottom: 8px;
    }
    .add mat-form-field {
      flex: 1;
    }
    .links {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
      gap: 8px;
      align-items: center;
    }
  `,
})
export class PackageForgePage {
  private readonly api = inject(CommunityApi);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);

  readonly workId = input<string>();

  protected readonly types = PACKAGE_TYPES;
  protected readonly genres = GENRES;
  protected readonly visibilities = VISIBILITY_OPTIONS;
  protected readonly roles = ITEM_ROLES;
  protected readonly linkTypes = LINK_TYPES;

  protected readonly detail = signal<WorkDetail | null>(null);
  protected readonly mine = signal<WorkCard[]>([]);
  protected readonly others = signal<WorkCard[]>([]);
  protected readonly picked = signal(new Set<number>());
  protected readonly busy = signal(false);
  protected readonly items = computed(() => this.detail()?.packageView?.items ?? []);
  protected readonly addable = computed(() => {
    const inside = new Set(this.items().map((i) => i.work.id));
    return this.mine().filter((w) => !inside.has(w.id));
  });

  protected packageType: PackageType = 'UNIVERSE';
  protected title = '';
  protected synopsis = '';
  protected genre = '';
  protected tags = '';
  protected worldNote = '';
  protected visibility: Visibility = 'PUBLIC';
  protected addMine: number | null = null;
  protected otherQ = '';
  protected linkFrom: number | null = null;
  protected linkTo: number | null = null;
  protected linkType: LinkType = 'SAME_WORLD';
  protected linkLabel = '';

  constructor() {
    this.api.myWorks().subscribe((list) => this.mine.set(list.filter((w) => w.kind !== 'PACKAGE')));
    effect(() => {
      const id = this.workId();
      if (id) this.reload(Number(id), true);
      else this.detail.set(null);
    });
  }

  private reload(id: number, fillForm = false): void {
    this.api.work(id).subscribe({
      next: (d) => {
        this.detail.set(d);
        if (!fillForm) return;
        this.title = d.card.title;
        this.synopsis = d.card.synopsis ?? '';
        this.genre = d.card.genre ?? '';
        this.tags = d.card.tags.join(', ');
        this.visibility = d.card.visibility;
        this.packageType = d.card.packageType ?? 'UNIVERSE';
        this.worldNote = d.packageView?.worldNote ?? '';
      },
      error: (e) => this.snack.open(errorMessage(e), '닫기', { duration: 4000 }),
    });
  }

  protected togglePick(id: number): void {
    this.picked.update((s) => {
      const next = new Set(s);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  protected create(): void {
    this.run(
      this.api.createPackage({
        title: this.title.trim(),
        synopsis: this.synopsis.trim() || null,
        genre: this.genre || null,
        tags: this.tags.trim() || null,
        packageType: this.packageType,
        worldNote: this.worldNote.trim() || null,
        visibility: this.visibility,
        memberWorkIds: [...this.picked()],
      }),
      (card) => this.router.navigate(['/package-forge', card.id]),
    );
  }

  protected saveMeta(): void {
    const d = this.detail();
    if (!d) return;
    this.run(
      this.api.updateWork(d.card.id, {
        title: this.title.trim(),
        synopsis: this.synopsis.trim() || null,
        genre: this.genre || null,
        tags: this.tags.trim() || null,
        visibility: this.visibility,
        clonePolicy: d.card.clonePolicy,
        downloadable: d.downloadable,
        packageType: this.packageType,
        worldNote: this.worldNote.trim() || null,
      }),
      () => {
        this.snack.open('저장했습니다.', undefined, { duration: 2000 });
        this.reload(d.card.id);
      },
    );
  }

  protected drop(event: CdkDragDrop<unknown>): void {
    const d = this.detail();
    if (!d?.packageView || event.previousIndex === event.currentIndex) return;
    const next = [...d.packageView.items];
    moveItemInArray(next, event.previousIndex, event.currentIndex);
    this.detail.set({ ...d, packageView: { ...d.packageView, items: next } });
    this.run(this.api.reorderPackage(d.card.id, next.map((i) => i.id)), () => this.reload(d.card.id));
  }

  protected setRole(itemId: number, role: ItemRole, note: string | null): void {
    const d = this.detail();
    if (d) this.run(this.api.updatePackageItem(d.card.id, itemId, { role, note }), () => this.reload(d.card.id));
  }

  protected addItem(memberWorkId: number): void {
    const d = this.detail();
    if (!d) return;
    this.run(this.api.addPackageItem(d.card.id, { memberWorkId, role: 'MAIN', note: null }), (item) => {
      this.addMine = null;
      this.others.update((list) => list.filter((w) => w.id !== memberWorkId));
      if (item.status === 'PENDING') this.snack.open('작가에게 편입을 요청했습니다. 승인 전에는 독자에게 보이지 않습니다.', '닫기', { duration: 4000 });
      this.reload(d.card.id);
    });
  }

  protected removeItem(itemId: number): void {
    const d = this.detail();
    if (d && confirm('이 작품을 패키지에서 뺄까요?')) this.run(this.api.removePackageItem(d.card.id, itemId), () => this.reload(d.card.id));
  }

  protected searchOthers(): void {
    const q = this.otherQ.trim();
    if (!q) return;
    const me = new Set([...this.mine().map((w) => w.id), ...this.items().map((i) => i.work.id)]);
    this.api.board({ kind: 'LONG', q, size: 10 }).subscribe((long) => {
      this.api.board({ kind: 'SHORT', q, size: 10 }).subscribe((short) => {
        this.others.set([...long.items, ...short.items].filter((w) => !me.has(w.id)));
      });
    });
  }

  protected addLink(): void {
    const d = this.detail();
    if (!d || !this.linkFrom || !this.linkTo) return;
    this.run(
      this.api.addPackageLink(d.card.id, { fromItemId: this.linkFrom, toItemId: this.linkTo, type: this.linkType, label: this.linkLabel.trim() || null }),
      () => {
        this.linkLabel = '';
        this.reload(d.card.id);
      },
    );
  }

  protected removeLink(linkId: number): void {
    const d = this.detail();
    if (d) this.run(this.api.removePackageLink(d.card.id, linkId), () => this.reload(d.card.id));
  }

  protected itemTitle(itemId: number): string {
    return this.items().find((i) => i.id === itemId)?.work.title ?? '';
  }

  protected linkLabelOf(type: LinkType): string {
    return LINK_TYPES.find((l) => l.value === type)?.label ?? '';
  }

  private run<T>(request: Observable<T>, done: (r: T) => void): void {
    this.busy.set(true);
    request.subscribe({
      next: (r) => {
        this.busy.set(false);
        done(r);
      },
      error: (e) => {
        this.busy.set(false);
        this.snack.open(errorMessage(e), '닫기', { duration: 4000 });
      },
    });
  }
}

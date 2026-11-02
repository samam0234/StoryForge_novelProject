import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { UserSummary } from '../../../core/models/community';

/** 유저 아바타와 이름. 프로필로 이동합니다. */
@Component({
  selector: 'sf-user-chip',
  imports: [RouterLink],
  template: `
    <a class="chip" [routerLink]="['/users', user().id]" (click)="$event.stopPropagation()">
      <span class="avatar" [style.background]="color()" aria-hidden="true">{{ initial() }}</span>
      <span class="name">{{ user().displayName }}</span>
    </a>
  `,
  styles: `
    .chip {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      text-decoration: none;
      font-size: 13px;
      color: var(--sf-text);
      min-width: 0;
    }
    .chip:hover .name {
      text-decoration: underline;
    }
    .avatar {
      width: 22px;
      height: 22px;
      flex: none;
      display: grid;
      place-items: center;
      border-radius: 50%;
      color: #fff;
      font-size: 12px;
      font-weight: 700;
    }
    .name {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  `,
})
export class UserChipComponent {
  readonly user = input.required<UserSummary>();
  protected readonly initial = computed(() => Array.from(this.user().displayName)[0] ?? '?');
  protected readonly color = computed(() => this.user().avatarColor ?? 'var(--sf-primary)');
}

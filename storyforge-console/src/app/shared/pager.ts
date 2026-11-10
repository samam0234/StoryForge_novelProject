import { Component, input, output } from "@angular/core";

@Component({
  selector: "sf-pager",
  template: ` <div class="pager">
    <span>전체 {{ total() }}건 · {{ page() + 1 }} 페이지</span>
    <div>
      <button
        class="quiet"
        [disabled]="busy() || page() === 0"
        (click)="change.emit(page() - 1)"
      >
        이전
      </button>
      <button
        class="quiet"
        [disabled]="busy() || (page() + 1) * size() >= total()"
        (click)="change.emit(page() + 1)"
      >
        다음
      </button>
    </div>
  </div>`,
})
export class Pager {
  readonly total = input(0);
  readonly page = input(0);
  readonly size = input(20);
  readonly busy = input(false);
  readonly change = output<number>();
}

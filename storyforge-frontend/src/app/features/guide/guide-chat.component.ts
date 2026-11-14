import { Component, ElementRef, Injector, afterNextRender, inject, output, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
import { CommunityApi } from '../../core/api/community-api.service';
import { LiveChannel } from '../../core/api/live-channel.service';
import { LlmNoticeComponent } from './llm-notice.component';
import { errorMessage } from '../../core/api/storyforge-api.service';
import { GuideAction } from '../../core/models/community';

interface Message {
  from: 'me' | 'guide';
  text: string;
  actions: GuideAction[];
  error?: boolean;
}

const SHORTCUTS = ['/도움말', '/상태', '/게시', '/클론', '/패키지', '/콜라보'];

/**
 * AI 가이드챗. 명령어를 입력하면 안내하고, 버튼으로 해당 화면이나 Codicomperchat으로 이동합니다.
 * 답변은 글자로만 보여 주고, 이동 버튼은 서버가 준 목록만 씁니다. 대화는 저장하지 않습니다.
 */
@Component({
  selector: 'sf-guide-chat',
  imports: [FormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, LlmNoticeComponent],
  template: `
    <header class="sf-row head">
      <mat-icon aria-hidden="true">support_agent</mat-icon>
      <strong>AI 가이드</strong>
      <span class="sf-muted">{{ live.connected() ? '실시간 연결' : '일반 연결' }}</span>
      <span class="sf-spacer"></span>
      <button mat-icon-button aria-label="가이드 닫기" (click)="closed.emit()"><mat-icon>close</mat-icon></button>
    </header>
    <sf-llm-notice />
    <div #log class="log" role="log" aria-live="polite" aria-label="가이드 대화">
      @for (m of messages(); track $index) {
        <div class="msg" [class.me]="m.from === 'me'" [class.err]="m.error">
          <p>{{ m.text }}</p>
          @if (m.actions.length) {
            <div class="actions">
              @for (a of m.actions; track a.label) {
                <button mat-stroked-button (click)="act(a)">
                  @if (a.kind === 'external') {
                    <mat-icon>open_in_new</mat-icon>
                  }
                  {{ a.label }}
                </button>
              }
            </div>
          }
        </div>
      }
      @if (busy()) {
        <div class="msg"><p class="sf-muted">답변을 만드는 중입니다…</p></div>
      }
    </div>
    <div class="shortcuts" aria-label="자주 쓰는 명령어">
      @for (s of shortcuts; track s) {
        <button type="button" (click)="send(s)" [disabled]="busy()">{{ s }}</button>
      }
    </div>
    <form class="input" (ngSubmit)="send(text)">
      <mat-form-field appearance="outline" subscriptSizing="dynamic" class="sf-full">
        <mat-label>명령어 또는 질문</mat-label>
        <input matInput name="message" [(ngModel)]="text" maxlength="2000" autocomplete="off" />
        <button matSuffix mat-icon-button type="submit" aria-label="보내기" [disabled]="busy() || !text.trim()"><mat-icon>send</mat-icon></button>
      </mat-form-field>
    </form>
  `,
  styles: `
    :host {
      display: flex;
      flex-direction: column;
      height: 100%;
      background: var(--sf-surface);
    }
    .head {
      padding: 4px 8px 4px 16px;
      border-bottom: 1px solid var(--sf-border);
    }
    .log {
      flex: 1;
      min-height: 0;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
      gap: 8px;
      padding: 12px;
    }
    .msg {
      flex: none;
      max-width: 90%;
      align-self: flex-start;
      padding: 8px 10px;
      border-radius: 10px;
      background: var(--sf-surface-2);
      font-size: 14px;
    }
    .msg p {
      margin: 0;
      white-space: pre-wrap;
      overflow-wrap: anywhere;
    }
    .msg.me {
      align-self: flex-end;
      background: color-mix(in srgb, var(--sf-secondary) 22%, transparent);
    }
    .msg.err {
      color: var(--sf-severity-high);
    }
    .actions {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      margin-top: 8px;
    }
    .shortcuts {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
      padding: 0 12px 8px;
    }
    .shortcuts button {
      padding: 2px 8px;
      font: inherit;
      font-size: 12px;
      color: var(--sf-text);
      border: 1px solid var(--sf-border);
      border-radius: 999px;
      background: transparent;
      cursor: pointer;
    }
    .input {
      padding: 0 12px 12px;
    }
  `,
})
export class GuideChatComponent {
  private readonly api = inject(CommunityApi);
  protected readonly live = inject(LiveChannel);
  private readonly router = inject(Router);
  private readonly injector = inject(Injector);
  private readonly log = viewChild.required<ElementRef<HTMLElement>>('log');

  readonly closed = output<void>();

  protected readonly shortcuts = SHORTCUTS;
  protected readonly busy = signal(false);
  protected readonly messages = signal<Message[]>([
    { from: 'guide', text: '무엇을 도와드릴까요? /도움말로 명령어를 볼 수 있습니다.', actions: [] },
  ]);
  protected text = '';

  protected send(raw: string): void {
    const message = raw.trim();
    if (!message || this.busy()) return;
    this.text = '';
    this.push({ from: 'me', text: message, actions: [] });
    this.busy.set(true);
    void this.deliver(message);
  }

  private async deliver(message: string): Promise<void> {
    const opened = await this.live.whenOpen(700);
    if (!opened) {
      this.byHttp(message);
      return;
    }
    try {
      const reply = await this.live.guide(message);
      this.busy.set(false);
      this.push({ from: 'guide', text: reply.reply, actions: reply.actions ?? [] });
    } catch {
      this.byHttp(message);
    }
  }

  private byHttp(message: string): void {
    this.api.guide(message).subscribe({
      next: (r) => {
        this.busy.set(false);
        this.push({ from: 'guide', text: r.reply, actions: r.actions });
      },
      error: (e) => {
        this.busy.set(false);
        this.push({ from: 'guide', text: errorMessage(e), actions: [], error: true });
      },
    });
  }

  protected act(a: GuideAction): void {
    if (a.kind === 'route') {
      if (a.target.startsWith('/')) this.router.navigateByUrl(a.target);
      return;
    }
    if (/^https?:\/\//.test(a.target) && confirm(`외부 사이트로 이동합니다.\n${a.target}`)) {
      window.open(a.target, '_blank', 'noopener,noreferrer');
    }
  }

  private push(m: Message): void {
    this.messages.update((list) => [...list, m]);
    afterNextRender(
      () => {
        const el = this.log().nativeElement;
        el.scrollTop = el.scrollHeight;
      },
      { injector: this.injector },
    );
  }
}

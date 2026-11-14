import { Injectable, inject } from '@angular/core';
import { AuthService } from '../auth/auth.service';
import { GuideReply } from '../models/community';

/** 챕터 생성 SSE와 별도로, 가이드와 장면 개척만 주고받는 웹소켓입니다. */
export interface ScoutDirection {
  title: string;
  causalLink: string;
  nextAction: string;
  avoid: string;
}

interface Waiter {
  resolve: (value: GuideReply & { directions?: ScoutDirection[] }) => void;
  reject: (error: Error) => void;
  timer: ReturnType<typeof setTimeout>;
}

@Injectable({ providedIn: 'root' })
export class LiveChannel {
  private readonly auth = inject(AuthService);
  private socket: WebSocket | null = null;
  private seq = 0;
  private readonly pending = new Map<string, Waiter>();

  isOpen(): boolean {
    return typeof WebSocket !== 'undefined' && this.socket?.readyState === WebSocket.OPEN;
  }

  /** 로그인 토큰이 있을 때만 백엔드 `/api/v1/ws`로 연결합니다. FastAPI 주소는 쓰지 않습니다. */
  connect(): void {
    const token = this.auth.token();
    if (!token || typeof WebSocket === 'undefined') return;
    if (this.socket && (this.socket.readyState === WebSocket.OPEN || this.socket.readyState === WebSocket.CONNECTING)) return;
    try {
      const url = `${location.origin.replace(/^http/, 'ws')}/api/v1/ws?access_token=${encodeURIComponent(token)}`;
      const socket = new WebSocket(url);
      this.socket = socket;
      socket.addEventListener('message', (event) => this.onMessage(String(event.data)));
      socket.addEventListener('close', () => {
        this.failAll(new Error('실시간 연결이 닫혔습니다.'));
        if (this.socket === socket) this.socket = null;
      });
    } catch {
      this.socket = null;
    }
  }

  guide(message: string): Promise<GuideReply> {
    return this.request({ op: 'guide', message }).then((body) => ({ reply: body.reply, actions: body.actions ?? [] }));
  }

  scout(projectId: number): Promise<{ directions: ScoutDirection[] }> {
    return this.request({ op: 'scout', projectId }).then((body) => ({ directions: body.directions ?? [] }));
  }

  private request(body: object): Promise<GuideReply & { directions?: ScoutDirection[] }> {
    if (!this.isOpen() || !this.socket) return Promise.reject(new Error('실시간 연결이 없습니다.'));
    const id = String(++this.seq);
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        this.pending.delete(id);
        reject(new Error('응답 시간이 초과되었습니다.'));
      }, 20000);
      this.pending.set(id, { resolve, reject, timer });
      this.socket?.send(JSON.stringify({ ...body, id }));
    });
  }

  private onMessage(raw: string): void {
    let body: { id?: string; op?: string; message?: string; reply?: string; actions?: GuideReply['actions']; directions?: ScoutDirection[] };
    try {
      body = JSON.parse(raw) as typeof body;
    } catch {
      return;
    }
    const wait = body.id ? this.pending.get(body.id) : undefined;
    if (!wait || !body.id) return;
    clearTimeout(wait.timer);
    this.pending.delete(body.id);
    if (body.op === 'error') wait.reject(new Error(body.message || '요청을 처리하지 못했습니다.'));
    else wait.resolve(body as GuideReply & { directions?: ScoutDirection[] });
  }

  private failAll(error: Error): void {
    for (const wait of this.pending.values()) {
      clearTimeout(wait.timer);
      wait.reject(error);
    }
    this.pending.clear();
  }
}

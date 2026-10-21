import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Chapter } from '../models/models';

export type ChapterStreamEvent =
  | { type: 'token'; text: string }
  | { type: 'done'; chapter: Chapter }
  | { type: 'error'; message: string };

/**
 * 챕터 생성 SSE 수신 (기획서 §7.2). POST 요청이라 EventSource 대신 fetch 스트림을 읽습니다.
 * 백엔드 이벤트: token {text} / done ChapterResponse / error {message}
 */
@Injectable({ providedIn: 'root' })
export class ChapterStream {
  generate(projectId: number, chapterNumber: number): Observable<ChapterStreamEvent> {
    const url = `${environment.apiBaseUrl}/projects/${projectId}/chapters/${chapterNumber}/generate`;
    return new Observable<ChapterStreamEvent>((sub) => {
      const ctrl = new AbortController();
      (async () => {
        const res = await fetch(url, {
          method: 'POST',
          headers: { Accept: 'text/event-stream, application/problem+json' },
          signal: ctrl.signal,
        });
        if (!res.ok || !res.body) {
          const body = await res.json().catch(() => null);
          sub.next({ type: 'error', message: body?.detail ?? `요청에 실패했습니다 (${res.status})` });
          sub.complete();
          return;
        }
        const reader = res.body.pipeThrough(new TextDecoderStream()).getReader();
        let buffer = '';
        for (;;) {
          const { value, done } = await reader.read();
          if (done) break;
          buffer += value;
          const blocks = buffer.split(/\r?\n\r?\n/);
          buffer = blocks.pop() ?? '';
          for (const block of blocks) {
            const event = parseBlock(block);
            if (event) sub.next(event);
          }
        }
        sub.complete();
      })().catch((e) => {
        if (!ctrl.signal.aborted) {
          sub.next({ type: 'error', message: '스트리밍 연결이 끊어졌습니다.' });
          sub.complete();
        }
        console.warn(e);
      });
      return () => ctrl.abort();
    });
  }
}

function parseBlock(block: string): ChapterStreamEvent | null {
  let event = 'message';
  const data: string[] = [];
  for (const line of block.split(/\r?\n/)) {
    const m = /^(\w+):\s?(.*)$/.exec(line);
    if (!m) continue;
    if (m[1] === 'event') event = m[2];
    else if (m[1] === 'data') data.push(m[2]);
  }
  if (!data.length) return null;
  const payload = JSON.parse(data.join('\n'));
  switch (event) {
    case 'token':
      return { type: 'token', text: payload.text ?? '' };
    case 'done':
      return { type: 'done', chapter: payload as Chapter };
    case 'error':
      return { type: 'error', message: payload.message ?? '챕터 생성에 실패했습니다.' };
    default:
      return null;
  }
}

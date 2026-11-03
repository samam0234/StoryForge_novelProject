import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { firstValueFrom, toArray } from 'rxjs';
import { vi } from 'vitest';
import { AuthService } from '../auth/auth.service';
import { ChapterStream, ChapterStreamEvent } from './chapter-stream.service';

function streamResponse(chunks: string[], init: ResponseInit = { status: 200 }): Response {
  const encoder = new TextEncoder();
  const body = new ReadableStream<Uint8Array>({
    start(controller) {
      chunks.forEach((c) => controller.enqueue(encoder.encode(c)));
      controller.close();
    },
  });
  return new Response(body, init);
}

describe('ChapterStream', () => {
  let stream: ChapterStream;
  let auth: AuthService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideRouter([])] });
    stream = TestBed.inject(ChapterStream);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => vi.unstubAllGlobals());

  const collect = (): Promise<ChapterStreamEvent[]> => firstValueFrom(stream.generate(1, 1).pipe(toArray()));

  it('Spring SSE 형식을 token, done 이벤트로 변환한다', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(streamResponse([
      'event:token\ndata:{"text":"첫"}\n\n',
      'event:token\ndata:{"text":" 문단"}\n\n',
      'event:done\ndata:{"id":7,"number":1}\n\n',
    ])));
    const events = await collect();
    expect(events.map((e) => e.type)).toEqual(['token', 'token', 'done']);
    expect(events[1]).toEqual({ type: 'token', text: ' 문단' });
  });

  it('이벤트가 청크 경계에서 잘려도 합쳐서 읽는다', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(streamResponse([
      'event:tok',
      'en\ndata:{"te',
      'xt":"잘린"}\r\n\r\nevent:error\ndata:{"message":"실패"}\n\n',
    ])));
    const events = await collect();
    expect(events).toEqual([
      { type: 'token', text: '잘린' },
      { type: 'error', message: '실패' },
    ]);
  });

  it('HTTP 오류는 ProblemDetail의 detail을 error 이벤트로 돌려준다', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ detail: '지금 생성할 수 있는 챕터는 1장입니다.' }), { status: 409 }),
    ));
    expect(await collect()).toEqual([{ type: 'error', message: '지금 생성할 수 있는 챕터는 1장입니다.' }]);
  });

  it('401이면 세션을 만료시키고 이벤트 없이 끝낸다', async () => {
    const expire = vi.spyOn(auth, 'expire').mockImplementation(() => undefined);
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('', { status: 401 })));
    expect(await collect()).toEqual([]);
    expect(expire).toHaveBeenCalled();
  });

  it('연결이 끊어지면 error 이벤트를 보낸다', async () => {
    vi.spyOn(console, 'warn').mockImplementation(() => undefined);
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('network')));
    const events = await collect();
    expect(events[0].type).toBe('error');
  });

  it('요청에 토큰을 직접 붙인다 (fetch는 인터셉터를 거치지 않음)', async () => {
    vi.spyOn(auth, 'token').mockReturnValue('abc');
    const fetchMock = vi.fn().mockResolvedValue(streamResponse([]));
    vi.stubGlobal('fetch', fetchMock);
    await collect();
    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBe('Bearer abc');
  });
});

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface LlmNotice {
  provider: string;
  external: boolean;
  notice: string | null;
}

/** 외부 언어 모델 전송 안내. 신고·차단은 community-api가 담당합니다. */
@Injectable({ providedIn: 'root' })
export class ModerationApi {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  llmNotice(): Observable<LlmNotice> {
    return this.http.get<LlmNotice>(`${this.base}/llm-notice`);
  }
}

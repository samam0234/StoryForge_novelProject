import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type ReportTarget = 'POST' | 'WORK' | 'COMMENT' | 'USER';
export type ReportReason = 'SPAM' | 'ABUSE' | 'ILLEGAL' | 'COPYRIGHT' | 'OTHER';

export interface ReportView {
  id: number;
  reporterId: number;
  reporterName: string;
  targetType: ReportTarget;
  targetId: number;
  reason: ReportReason;
  detail: string | null;
  status: 'OPEN' | 'DISMISSED' | 'HIDDEN';
  createdAt: string;
}

export interface LlmNotice {
  provider: string;
  external: boolean;
  notice: string | null;
}

@Injectable({ providedIn: 'root' })
export class ModerationApi {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  llmNotice(): Observable<LlmNotice> {
    return this.http.get<LlmNotice>(`${this.base}/llm-notice`);
  }

  report(body: { targetType: ReportTarget; targetId: number; reason: ReportReason; detail?: string }): Observable<ReportView> {
    return this.http.post<ReportView>(`${this.base}/reports`, body);
  }

  block(userId: number): Observable<void> {
    return this.http.post<void>(`${this.base}/blocks`, { userId });
  }

  unblock(userId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/blocks/${userId}`);
  }

  openReports(): Observable<ReportView[]> {
    return this.http.get<ReportView[]>(`${this.base}/admin/reports`);
  }

  dismiss(reportId: number): Observable<ReportView> {
    return this.http.post<ReportView>(`${this.base}/admin/reports/${reportId}/dismiss`, null);
  }

  hide(reportId: number): Observable<ReportView> {
    return this.http.post<ReportView>(`${this.base}/admin/reports/${reportId}/hide`, null);
  }
}

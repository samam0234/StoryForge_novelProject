import { HttpClient, HttpErrorResponse } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";

export interface Page<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}
export interface User {
  id: number;
  email: string;
  displayName: string;
  admin: boolean;
}
export interface Member extends User {
  suspended: boolean;
  suspendedReason: string | null;
  createdAt: string;
}
export type Action = "HIDE" | "RESTORE" | "DISMISS" | "SUSPEND" | "UNSUSPEND";
export interface Report {
  id: number;
  targetType: string;
  targetId: number;
  reason: string;
  detail: string | null;
  status: string;
  reporter: { displayName: string };
  openReports: number;
  resolutionNote: string | null;
  createdAt: string;
  target: {
    title: string;
    excerpt: string | null;
    hidden: boolean;
    authorSuspended: boolean;
    exists: boolean;
    author: { displayName: string } | null;
  };
}
export interface Audit {
  id: number;
  actorId: number;
  actorName: string;
  action: string;
  targetType: string;
  targetId: number;
  reportId: number | null;
  note: string;
  createdAt: string;
}
export interface Overview {
  users: number;
  suspendedUsers: number;
  projects: number;
  works: number;
  openReports: number;
  pipeline: Record<string, number>;
  capturedAt: string;
}

@Injectable({ providedIn: "root" })
export class ConsoleApi {
  private readonly http = inject(HttpClient);
  overview() {
    return this.http.get<Overview>("/api/v1/admin/overview");
  }
  reports(status: string, page: number) {
    return this.http.get<Page<Report>>("/api/v1/admin/reports", {
      params: { status, page, size: 20 },
    });
  }
  decide(ids: number[], action: Action, note: string) {
    return this.http.post<{ requested: number; processed: number }>(
      "/api/v1/admin/reports/batch",
      { reportIds: ids, action, note },
    );
  }
  users(q: string, status: string, page: number) {
    const params: Record<string, string | number> = { q, page, size: 20 };
    if (status) params["suspended"] = status;
    return this.http.get<Page<Member>>("/api/v1/admin/users", { params });
  }
  suspend(id: number, suspended: boolean, note: string) {
    return this.http.put<Member>(`/api/v1/admin/users/${id}/suspension`, {
      suspended,
      note,
    });
  }
  audit(page: number, actorId: string) {
    const params: Record<string, string | number> = { page, size: 20 };
    if (actorId) params["actorId"] = actorId;
    return this.http.get<Page<Audit>>("/api/v1/admin/audit", { params });
  }
}

export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0)
      return "서버에 연결할 수 없습니다. 연결 상태를 확인하고 다시 시도하세요.";
    return (
      error.error?.detail || "요청을 처리하지 못했습니다. 다시 시도하세요."
    );
  }
  return error instanceof Error ? error.message : "요청을 처리하지 못했습니다.";
}

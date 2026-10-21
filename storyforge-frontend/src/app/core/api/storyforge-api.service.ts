import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Chapter,
  Character,
  Outline,
  OutlineChapter,
  Project,
  ProjectStatusInfo,
  StoryBible,
} from '../models/models';

/** Spring Boot `/api/v1` 래퍼. 화면 코드는 HttpClient를 직접 쓰지 않습니다. */
@Injectable({ providedIn: 'root' })
export class StoryforgeApi {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  // Projects
  listProjects(): Observable<Project[]> {
    return this.http.get<Project[]>(`${this.base}/projects`);
  }
  createProject(body: { title: string; genre: string | null }): Observable<Project> {
    return this.http.post<Project>(`${this.base}/projects`, body);
  }
  getProject(id: number): Observable<Project> {
    return this.http.get<Project>(`${this.base}/projects/${id}`);
  }
  getStatus(id: number): Observable<ProjectStatusInfo> {
    return this.http.get<ProjectStatusInfo>(`${this.base}/projects/${id}/status`);
  }
  deleteProject(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/projects/${id}`);
  }

  // Story Bible
  getBible(projectId: number): Observable<StoryBible | null> {
    return this.http.get<StoryBible>(`${this.base}/projects/${projectId}/bible`).pipe(nullOn404());
  }
  updateBible(
    projectId: number,
    body: { worldSetting: string | null; theme: string | null; tone: string | null; characters: Character[] },
  ): Observable<StoryBible> {
    return this.http.put<StoryBible>(`${this.base}/projects/${projectId}/bible`, body);
  }
  generateBible(projectId: number, prompt: string): Observable<StoryBible> {
    return this.http.post<StoryBible>(`${this.base}/projects/${projectId}/bible/generate`, { prompt });
  }
  confirmBible(projectId: number): Observable<void> {
    return this.http.put<void>(`${this.base}/projects/${projectId}/bible/confirm`, null);
  }

  // Outline
  getOutline(projectId: number): Observable<Outline | null> {
    return this.http.get<Outline>(`${this.base}/projects/${projectId}/outline`).pipe(nullOn404());
  }
  updateOutline(projectId: number, chapters: OutlineChapter[]): Observable<Outline> {
    return this.http.put<Outline>(`${this.base}/projects/${projectId}/outline`, { chapters });
  }
  generateOutline(projectId: number, chapterCount: number): Observable<Outline> {
    return this.http.post<Outline>(`${this.base}/projects/${projectId}/outline/generate`, { chapterCount });
  }
  confirmOutline(projectId: number): Observable<void> {
    return this.http.put<void>(`${this.base}/projects/${projectId}/outline/confirm`, null);
  }

  // Chapters (생성은 ChapterStream 사용)
  listChapters(projectId: number): Observable<Chapter[]> {
    return this.http.get<Chapter[]>(`${this.base}/projects/${projectId}/chapters`);
  }
  updateChapter(id: number, body: { title: string | null; content: string | null }): Observable<Chapter> {
    return this.http.put<Chapter>(`${this.base}/chapters/${id}`, body);
  }
  confirmChapter(id: number): Observable<Chapter> {
    return this.http.put<Chapter>(`${this.base}/chapters/${id}/confirm`, null);
  }
}

function nullOn404<T>() {
  return catchError<T, Observable<T | null>>((e: unknown) =>
    e instanceof HttpErrorResponse && e.status === 404 ? of(null) : throwError(() => e),
  );
}

/** 백엔드 ProblemDetail의 detail을 사용자 메시지로 꺼냅니다. */
export function errorMessage(e: unknown): string {
  if (e instanceof HttpErrorResponse) {
    if (e.status === 0) return '서버에 연결할 수 없습니다.';
    return e.error?.detail ?? `요청에 실패했습니다 (${e.status})`;
  }
  return '알 수 없는 오류가 발생했습니다.';
}

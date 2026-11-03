import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Achievement,
  ActivityEntry,
  CloneMode,
  CloneNetwork,
  CommentView,
  Contributions,
  GuideReply,
  IssueBoard,
  ItemRole,
  LikeResponse,
  LinkType,
  PackageItemView,
  PackageLinkView,
  PackageRequest,
  PackageType,
  PageResponse,
  PostCard,
  PostCategory,
  PostDetail,
  Profile,
  PublishBody,
  ShareChannel,
  ShareEntry,
  ShareGraph,
  UpdateWorkBody,
  UpstreamCompare,
  Visibility,
  WorkCard,
  WorkChapter,
  WorkCommit,
  WorkDetail,
  WorkGraph,
  WorkKind,
  Collaborator,
  CommitTarget,
} from '../models/community';
import { ExportFormat, User } from '../models/models';

/** 게시판, 작품, Clone, 공유, 패키지포지, 라운지, 프로필, 가이드챗 API */
@Injectable({ providedIn: 'root' })
export class CommunityApi {
  private readonly http = inject(HttpClient);
  private readonly base = environment.apiBaseUrl;

  // 게시판·작품
  board(q: { kind: WorkKind; genre?: string; q?: string; sort?: string; page?: number; size?: number }): Observable<PageResponse<WorkCard>> {
    return this.http.get<PageResponse<WorkCard>>(`${this.base}/works`, { params: params(q) });
  }
  work(id: number): Observable<WorkDetail> {
    return this.http.get<WorkDetail>(`${this.base}/works/${id}`);
  }
  workChapter(id: number, number: number): Observable<WorkChapter> {
    return this.http.get<WorkChapter>(`${this.base}/works/${id}/chapters/${number}`);
  }
  publish(projectId: number, body: PublishBody): Observable<WorkCard> {
    return this.http.post<WorkCard>(`${this.base}/projects/${projectId}/publish`, body);
  }
  updateWork(id: number, body: UpdateWorkBody): Observable<WorkCard> {
    return this.http.put<WorkCard>(`${this.base}/works/${id}`, body);
  }
  editWorkChapter(id: number, number: number, body: { title: string | null; content: string; message: string | null }): Observable<WorkChapter> {
    return this.http.put<WorkChapter>(`${this.base}/works/${id}/chapters/${number}`, body);
  }
  syncWork(id: number, message: string | null): Observable<WorkCard> {
    return this.http.post<WorkCard>(`${this.base}/works/${id}/sync`, { message });
  }
  deleteWork(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/works/${id}`);
  }
  exportWork(id: number, format: ExportFormat): Observable<Blob> {
    return this.http.get(`${this.base}/works/${id}/export`, { params: { format }, responseType: 'blob' });
  }
  like(id: number, liked: boolean): Observable<LikeResponse> {
    const url = `${this.base}/works/${id}/like`;
    return liked ? this.http.put<LikeResponse>(url, null) : this.http.delete<LikeResponse>(url);
  }

  // 작업 그래프
  workGraph(id: number): Observable<WorkGraph> {
    return this.http.get<WorkGraph>(`${this.base}/works/${id}/activity`);
  }
  projectGraph(projectId: number): Observable<WorkGraph> {
    return this.http.get<WorkGraph>(`${this.base}/projects/${projectId}/activity`);
  }

  // Clone
  cloneWork(id: number, body: { mode: CloneMode; title: string | null }): Observable<{ projectId: number; mode: CloneMode; revision: number }> {
    return this.http.post<{ projectId: number; mode: CloneMode; revision: number }>(`${this.base}/works/${id}/clone`, body);
  }
  network(id: number): Observable<CloneNetwork> {
    return this.http.get<CloneNetwork>(`${this.base}/works/${id}/network`);
  }
  upstream(projectId: number): Observable<UpstreamCompare> {
    return this.http.get<UpstreamCompare>(`${this.base}/projects/${projectId}/upstream`);
  }

  // 공유
  share(id: number, body: { channel: ShareChannel; target: string | null }): Observable<{ share: ShareEntry; token: string }> {
    return this.http.post<{ share: ShareEntry; token: string }>(`${this.base}/works/${id}/shares`, body);
  }
  shares(id: number): Observable<ShareGraph> {
    return this.http.get<ShareGraph>(`${this.base}/works/${id}/shares`);
  }
  visitShare(token: string): Observable<void> {
    return this.http.post<void>(`${this.base}/shares/${encodeURIComponent(token)}/visit`, null);
  }

  // 댓글
  workComments(id: number): Observable<CommentView[]> {
    return this.http.get<CommentView[]>(`${this.base}/works/${id}/comments`);
  }
  addWorkComment(id: number, body: { content: string; parentId: number | null }): Observable<CommentView> {
    return this.http.post<CommentView>(`${this.base}/works/${id}/comments`, body);
  }
  postComments(id: number): Observable<CommentView[]> {
    return this.http.get<CommentView[]>(`${this.base}/posts/${id}/comments`);
  }
  addPostComment(id: number, body: { content: string; parentId: number | null }): Observable<CommentView> {
    return this.http.post<CommentView>(`${this.base}/posts/${id}/comments`, body);
  }
  editComment(id: number, content: string): Observable<CommentView> {
    return this.http.put<CommentView>(`${this.base}/comments/${id}`, { content });
  }
  deleteComment(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/comments/${id}`);
  }

  // 패키지포지
  createPackage(body: {
    title: string;
    synopsis: string | null;
    genre: string | null;
    tags: string | null;
    packageType: PackageType;
    worldNote: string | null;
    visibility: Visibility;
    memberWorkIds: number[];
  }): Observable<WorkCard> {
    return this.http.post<WorkCard>(`${this.base}/packages`, body);
  }
  addPackageItem(id: number, body: { memberWorkId: number; role: ItemRole; note: string | null }): Observable<PackageItemView> {
    return this.http.post<PackageItemView>(`${this.base}/packages/${id}/items`, body);
  }
  updatePackageItem(id: number, itemId: number, body: { role: ItemRole; note: string | null }): Observable<PackageItemView> {
    return this.http.put<PackageItemView>(`${this.base}/packages/${id}/items/${itemId}`, body);
  }
  removePackageItem(id: number, itemId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/packages/${id}/items/${itemId}`);
  }
  reorderPackage(id: number, itemIds: number[]): Observable<void> {
    return this.http.put<void>(`${this.base}/packages/${id}/order`, { itemIds });
  }
  addPackageLink(id: number, body: { fromItemId: number; toItemId: number; type: LinkType; label: string | null }): Observable<PackageLinkView> {
    return this.http.post<PackageLinkView>(`${this.base}/packages/${id}/links`, body);
  }
  removePackageLink(id: number, linkId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/packages/${id}/links/${linkId}`);
  }
  packageRequests(): Observable<PackageRequest[]> {
    return this.http.get<PackageRequest[]>(`${this.base}/me/package-requests`);
  }
  decidePackageRequest(itemId: number, approve: boolean): Observable<void> {
    return this.http.put<void>(`${this.base}/me/package-requests/${itemId}`, { approve });
  }

  // 라운지
  posts(q: { category?: PostCategory; q?: string; page?: number; size?: number }): Observable<PageResponse<PostCard>> {
    return this.http.get<PageResponse<PostCard>>(`${this.base}/posts`, { params: params(q) });
  }
  post(id: number): Observable<PostDetail> {
    return this.http.get<PostDetail>(`${this.base}/posts/${id}`);
  }
  createPost(body: { category: PostCategory; title: string; content: string }): Observable<PostCard> {
    return this.http.post<PostCard>(`${this.base}/posts`, body);
  }
  updatePost(id: number, body: { category: PostCategory; title: string; content: string }): Observable<PostCard> {
    return this.http.put<PostCard>(`${this.base}/posts/${id}`, body);
  }
  deletePost(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/posts/${id}`);
  }
  likePost(id: number, liked: boolean): Observable<LikeResponse> {
    const url = `${this.base}/posts/${id}/like`;
    return liked ? this.http.put<LikeResponse>(url, null) : this.http.delete<LikeResponse>(url);
  }

  // 이슈
  issues(): Observable<IssueBoard> {
    return this.http.get<IssueBoard>(`${this.base}/issues`);
  }

  // 프로필·계정
  profile(userId: number): Observable<Profile> {
    return this.http.get<Profile>(`${this.base}/users/${userId}`);
  }
  userWorks(userId: number): Observable<WorkCard[]> {
    return this.http.get<WorkCard[]>(`${this.base}/users/${userId}/works`);
  }
  contributions(userId: number): Observable<Contributions> {
    return this.http.get<Contributions>(`${this.base}/users/${userId}/contributions`);
  }
  userActivity(userId: number, q: { date?: string; page?: number; size?: number }): Observable<PageResponse<ActivityEntry>> {
    return this.http.get<PageResponse<ActivityEntry>>(`${this.base}/users/${userId}/activity`, { params: params(q) });
  }
  achievements(userId: number): Observable<Achievement[]> {
    return this.http.get<Achievement[]>(`${this.base}/users/${userId}/achievements`);
  }
  myWorks(): Observable<WorkCard[]> {
    return this.http.get<WorkCard[]>(`${this.base}/me/works`);
  }
  updateProfile(body: { displayName: string; bio: string | null; website: string | null; avatarColor: string | null }): Observable<User> {
    return this.http.put<User>(`${this.base}/me/profile`, body);
  }
  changePassword(body: { currentPassword: string; newPassword: string }): Observable<void> {
    return this.http.put<void>(`${this.base}/me/password`, body);
  }
  deleteAccount(password: string): Observable<void> {
    return this.http.post<void>(`${this.base}/me/delete`, { password });
  }

  // 가이드챗
  guide(message: string): Observable<GuideReply> {
    return this.http.post<GuideReply>(`${this.base}/guide/chat`, { message });
  }

  invitations(): Observable<Collaborator[]> {
    return this.http.get<Collaborator[]>(`${this.base}/me/invitations`);
  }
  acceptInvitation(id: number): Observable<Collaborator> {
    return this.http.post<Collaborator>(`${this.base}/me/invitations/${id}/accept`, null);
  }
  declineInvitation(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/me/invitations/${id}`);
  }
  collaborators(projectId: number): Observable<Collaborator[]> {
    return this.http.get<Collaborator[]>(`${this.base}/projects/${projectId}/collaborators`);
  }
  invite(projectId: number, email: string): Observable<Collaborator> {
    return this.http.post<Collaborator>(`${this.base}/projects/${projectId}/collaborators`, { email });
  }
  removeCollaborator(projectId: number, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/projects/${projectId}/collaborators/${userId}`);
  }
  commits(projectId: number): Observable<WorkCommit[]> {
    return this.http.get<WorkCommit[]>(`${this.base}/projects/${projectId}/commits`);
  }
  commit(projectId: number, commitId: number): Observable<WorkCommit> {
    return this.http.get<WorkCommit>(`${this.base}/projects/${projectId}/commits/${commitId}`);
  }
  addCommit(projectId: number, body: {
    message: string;
    target: CommitTarget;
    chapterNumber?: number | null;
    startOffset?: number | null;
    endOffset?: number | null;
    quote?: string | null;
  }): Observable<WorkCommit> {
    return this.http.post<WorkCommit>(`${this.base}/projects/${projectId}/commits`, body);
  }
}

/** 값이 있는 항목만 쿼리로 보냅니다. */
function params(q: object): HttpParams {
  let p = new HttpParams();
  for (const [key, value] of Object.entries(q)) {
    if (value !== undefined && value !== null && value !== '') p = p.set(key, String(value));
  }
  return p;
}

/** Blob을 파일로 저장합니다 (프로젝트·작품 내보내기 공통). */
export function saveBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

/** 사용자 입력을 파일 이름으로 쓸 수 있게 바꿉니다. */
export function safeFilename(title: string, ext: string): string {
  return `${title.replace(/[\\/:*?"<>|]/g, '_').trim() || 'storyforge'}.${ext}`;
}

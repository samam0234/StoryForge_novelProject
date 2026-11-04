// 커뮤니티 API 계약 (백엔드 CommunityDtos, ActivityDtos). docs/plan/community.md

import { BibleSnapshot, OutlineChapter } from './models';

export type WorkKind = 'LONG' | 'SHORT' | 'PACKAGE';
export type Visibility = 'PUBLIC' | 'UNLISTED' | 'PRIVATE';
export type ClonePolicy = 'OPEN' | 'WORLD_ONLY' | 'CLOSED';
export type CloneMode = 'WORLD' | 'PLOT' | 'FULL';
export type PackageType = 'UNIVERSE' | 'OMNIBUS' | 'ANTHOLOGY' | 'SAGA' | 'CROSSOVER' | 'SPINOFF' | 'CUSTOM';
export type ItemRole = 'MAIN' | 'SIDE' | 'PREQUEL' | 'SEQUEL' | 'SPINOFF' | 'INTERLUDE';
export type LinkType = 'CONTINUES' | 'SAME_WORLD' | 'SHARED_CHARACTER' | 'SAME_EVENT' | 'REFERENCES';
export type ShareChannel = 'LINK' | 'X' | 'FACEBOOK' | 'KAKAO' | 'BLOG' | 'EMAIL' | 'COMMUNITY' | 'OTHER';
export type PostCategory = 'FREE' | 'TIP' | 'QUESTION' | 'RECRUIT' | 'SHOWCASE';
export type Actor = 'MANUAL' | 'AGENT';

export const KIND_LABEL: Record<WorkKind, string> = { LONG: '롱품', SHORT: '숏품', PACKAGE: '패키지포지' };
export const VISIBILITY_OPTIONS: { value: Visibility; label: string; hint: string }[] = [
  { value: 'PUBLIC', label: '공개', hint: '게시판과 프로필에 보입니다' },
  { value: 'UNLISTED', label: '링크 공개', hint: '링크를 아는 사람만 볼 수 있습니다' },
  { value: 'PRIVATE', label: '비공개', hint: '나만 볼 수 있습니다' },
];
export const CLONE_POLICY_OPTIONS: { value: ClonePolicy; label: string; hint: string }[] = [
  { value: 'OPEN', label: '모두 허용', hint: '세계관, 플롯, 전체 원고를 가져갈 수 있습니다' },
  { value: 'WORLD_ONLY', label: '세계관만', hint: 'Story Bible만 가져갈 수 있습니다' },
  { value: 'CLOSED', label: '허용 안 함', hint: '설정집도 공개하지 않습니다' },
];
export const CLONE_MODES: { value: CloneMode; label: string; icon: string; hint: string; start: string }[] = [
  { value: 'WORLD', label: '세계관', icon: 'public', hint: 'Story Bible만 가져와 내 이야기를 새로 씁니다.', start: 'Bible 작성 단계에서 시작' },
  { value: 'PLOT', label: '플롯', icon: 'account_tree', hint: 'Bible과 Outline을 가져와 줄거리를 고쳐 씁니다.', start: 'Outline 작성 단계에서 시작' },
  { value: 'FULL', label: '전체 원고', icon: 'menu_book', hint: '게시된 원고까지 가져와 이어 쓰거나 고쳐 씁니다.', start: '챕터 검토 단계에서 시작 (앞 장부터 다시 승인)' },
];
export const PACKAGE_TYPES: { value: PackageType; label: string; icon: string; hint: string }[] = [
  { value: 'UNIVERSE', label: '유니버스', icon: 'public', hint: '같은 세계관을 공유하는 작품들' },
  { value: 'OMNIBUS', label: '옴니버스', icon: 'view_module', hint: '독립된 이야기를 하나의 주제로 엮음' },
  { value: 'ANTHOLOGY', label: '앤솔러지', icon: 'collections_bookmark', hint: '여러 작가의 작품을 모은 선집' },
  { value: 'SAGA', label: '사가', icon: 'timeline', hint: '시간 순서로 이어지는 연대기' },
  { value: 'CROSSOVER', label: '크로스오버', icon: 'merge', hint: '다른 세계의 인물이 만나는 이야기' },
  { value: 'SPINOFF', label: '스핀오프', icon: 'call_split', hint: '원작과 외전 묶음' },
  { value: 'CUSTOM', label: '커스텀', icon: 'tune', hint: '직접 정하는 방식' },
];
export const ITEM_ROLES: { value: ItemRole; label: string }[] = [
  { value: 'MAIN', label: '본편' },
  { value: 'SIDE', label: '외전' },
  { value: 'PREQUEL', label: '프리퀄' },
  { value: 'SEQUEL', label: '속편' },
  { value: 'SPINOFF', label: '스핀오프' },
  { value: 'INTERLUDE', label: '막간' },
];
export const LINK_TYPES: { value: LinkType; label: string }[] = [
  { value: 'CONTINUES', label: '이어짐' },
  { value: 'SAME_WORLD', label: '같은 세계' },
  { value: 'SHARED_CHARACTER', label: '인물 공유' },
  { value: 'SAME_EVENT', label: '같은 사건' },
  { value: 'REFERENCES', label: '언급' },
];
export const SHARE_CHANNELS: { value: ShareChannel; label: string }[] = [
  { value: 'LINK', label: '링크 복사' },
  { value: 'X', label: 'X' },
  { value: 'FACEBOOK', label: 'Facebook' },
  { value: 'KAKAO', label: '카카오톡' },
  { value: 'BLOG', label: '블로그' },
  { value: 'EMAIL', label: '이메일' },
  { value: 'COMMUNITY', label: '커뮤니티' },
  { value: 'OTHER', label: '기타' },
];
export const POST_CATEGORIES: { value: PostCategory; label: string }[] = [
  { value: 'FREE', label: '자유' },
  { value: 'TIP', label: '창작 팁' },
  { value: 'QUESTION', label: '질문' },
  { value: 'RECRUIT', label: '콜라보 모집' },
  { value: 'SHOWCASE', label: '작품 소개' },
];
export const GENRES = [
  '판타지', '현대판타지', '로맨스', '로맨스판타지', '무협', 'SF', '미스터리', '스릴러', '호러', '드라마',
  '역사', '대체역사', '디스토피아', '아포칼립스', '일상·힐링', '성장', '청춘', '느와르', '스팀펑크', '신화',
];

export function labelOf<T extends string>(options: { value: T; label: string }[], value: T | null | undefined): string {
  return options.find((o) => o.value === value)?.label ?? '';
}

export interface UserSummary {
  id: number;
  displayName: string;
  avatarColor: string | null;
}

export interface PageResponse<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}

export interface WorkCard {
  id: number;
  kind: WorkKind;
  packageType: PackageType | null;
  title: string;
  synopsis: string | null;
  genre: string | null;
  tags: string[];
  author: UserSummary;
  visibility: Visibility;
  clonePolicy: ClonePolicy;
  revision: number;
  chapterCount: number;
  charCount: number;
  likeCount: number;
  commentCount: number;
  cloneCount: number;
  shareCount: number;
  viewCount: number;
  parentWorkId: number | null;
  parentTitle: string | null;
  /** 관리자가 숨김. 작가와 관리자에게만 보입니다. */
  hidden: boolean;
  publishedAt: string;
  updatedAt: string;
}

export interface ChapterEntry {
  number: number;
  title: string | null;
  chars: number;
}

export interface RevisionEntry {
  revision: number;
  message: string | null;
  chapterCount: number;
  charCount: number;
  createdAt: string;
}

export interface PackageItemView {
  id: number;
  position: number;
  role: ItemRole;
  note: string | null;
  status: 'PENDING' | 'APPROVED';
  work: WorkCard;
}

export interface PackageLinkView {
  id: number;
  fromItemId: number;
  toItemId: number;
  type: LinkType;
  label: string | null;
}

export interface PackageCharacter {
  name: string;
  crossover: boolean;
  appearances: { workId: number; workTitle: string; description: string | null }[];
}

export interface PackageView {
  worldNote: string | null;
  items: PackageItemView[];
  links: PackageLinkView[];
  characters: PackageCharacter[];
}

export interface WorkDetail {
  card: WorkCard;
  owned: boolean;
  liked: boolean;
  downloadable: boolean;
  projectId: number | null;
  cloneModes: CloneMode[];
  chapters: ChapterEntry[];
  bible: BibleSnapshot | null;
  outline: OutlineChapter[] | null;
  revisions: RevisionEntry[];
  packageView: PackageView | null;
  inPackages: WorkCard[];
}

export interface WorkChapter {
  number: number;
  title: string | null;
  content: string | null;
  previous: number | null;
  next: number | null;
}

export interface PublishBody {
  kind: WorkKind;
  title: string;
  synopsis: string | null;
  genre: string | null;
  tags: string | null;
  visibility: Visibility;
  clonePolicy: ClonePolicy;
  downloadable: boolean;
}

export interface UpdateWorkBody extends Omit<PublishBody, 'kind'> {
  packageType?: PackageType | null;
  worldNote?: string | null;
}

export interface LikeResponse {
  liked: boolean;
  likeCount: number;
}

export interface NetworkNode {
  workId: number;
  parentId: number | null;
  title: string;
  author: UserSummary;
  revision: number;
  likeCount: number;
  cloneCount: number;
  current: boolean;
}

export interface CloneEntry {
  user: UserSummary;
  mode: CloneMode;
  revision: number;
  createdAt: string;
  derivedWorkId: number | null;
}

export interface CloneNetwork {
  rootId: number;
  nodes: NetworkNode[];
  world: number;
  plot: number;
  full: number;
  clones: CloneEntry[];
}

export interface UpstreamChapter {
  number: number;
  upstreamTitle: string | null;
  localTitle: string | null;
  state: 'SAME' | 'CHANGED' | 'ONLY_UPSTREAM' | 'ONLY_LOCAL';
  changedChars: number;
  upstreamContent: string | null;
  localContent: string | null;
}

export interface UpstreamCompare {
  workId: number | null;
  title: string;
  mode: CloneMode;
  clonedRevision: number;
  currentRevision: number;
  available: boolean;
  chapters: UpstreamChapter[];
}

export interface ShareEntry {
  id: number;
  user: UserSummary;
  channel: ShareChannel;
  target: string | null;
  visitCount: number;
  createdAt: string;
}

export interface ShareGraph {
  total: number;
  visits: number;
  items: ShareEntry[];
}

export interface PackageRequest {
  itemId: number;
  packageId: number;
  packageTitle: string;
  requester: UserSummary;
  work: WorkCard;
  createdAt: string;
}

export interface PostCard {
  id: number;
  category: PostCategory;
  title: string;
  author: UserSummary;
  likeCount: number;
  commentCount: number;
  viewCount: number;
  createdAt: string;
}

export interface PostDetail {
  card: PostCard;
  content: string;
  owned: boolean;
  liked: boolean;
  hidden: boolean;
  updatedAt: string;
}

export interface CommentView {
  id: number;
  parentId: number | null;
  author: UserSummary;
  content: string;
  hidden: boolean;
  canEdit: boolean;
  canDelete: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Celebration {
  id: number;
  metric: string;
  threshold: number;
  message: string;
  work: WorkCard;
  createdAt: string;
}

export interface IssueBoard {
  trending: WorkCard[];
  celebrations: Celebration[];
  fresh: WorkCard[];
}

export interface Profile {
  user: UserSummary;
  bio: string | null;
  website: string | null;
  joinedAt: string;
  self: boolean;
  blockedByMe: boolean;
  suspended: boolean;
  stats: { works: number; likesReceived: number; clonesReceived: number; posts: number; comments: number; achievements: number };
}

export interface ActivityEntry {
  id: number;
  type: string;
  actor: Actor;
  summary: string;
  changedChars: number;
  createdAt: string;
  day: string;
  workId: number | null;
  workTitle: string | null;
  postId: number | null;
  postTitle: string | null;
  projectId: number | null;
  projectTitle: string | null;
}

export interface GraphDay {
  date: string;
  manual: number;
  agent: number;
  manualChars: number;
  agentChars: number;
}

export interface WorkGraph {
  manual: { events: number; chars: number };
  agent: { events: number; chars: number };
  days: GraphDay[];
  events: ActivityEntry[];
}

export interface ContributionDay {
  date: string;
  count: number;
  level: number;
}

export interface Contributions {
  from: string;
  to: string;
  total: number;
  currentStreak: number;
  longestStreak: number;
  days: ContributionDay[];
}

export type CommitTarget = 'BIBLE' | 'OUTLINE' | 'CHAPTER';

export interface Collaborator {
  id: number;
  projectId: number;
  projectTitle: string;
  user: UserSummary;
  status: 'PENDING' | 'ACCEPTED';
  createdAt: string;
}

export interface WorkCommit {
  id: number;
  projectId: number;
  author: UserSummary;
  parentId: number | null;
  message: string;
  target: CommitTarget;
  chapterNumber: number | null;
  startOffset: number | null;
  endOffset: number | null;
  quote: string | null;
  createdAt: string;
}

export interface Achievement {
  code: string;
  name: string;
  description: string;
  icon: string;
  tier: 'BRONZE' | 'SILVER' | 'GOLD' | 'PLATINUM';
  progress: number;
  target: number;
  earnedAt: string | null;
}

export interface GuideAction {
  label: string;
  kind: 'route' | 'external';
  target: string;
}

export interface GuideReply {
  reply: string;
  actions: GuideAction[];
}

// 신고·관리
export type ReportTargetType = 'WORK' | 'POST' | 'COMMENT' | 'USER';
export type ReportReason = 'SPAM' | 'ABUSE' | 'SEXUAL' | 'VIOLENCE' | 'COPYRIGHT' | 'PERSONAL_INFO' | 'OTHER';
export type ReportStatus = 'OPEN' | 'RESOLVED' | 'DISMISSED';
export type ReportAction = 'HIDE' | 'RESTORE' | 'DISMISS' | 'SUSPEND' | 'UNSUSPEND';

export const REPORT_REASONS: { value: ReportReason; label: string }[] = [
  { value: 'SPAM', label: '도배·광고' },
  { value: 'ABUSE', label: '욕설·괴롭힘·혐오' },
  { value: 'SEXUAL', label: '부적절한 성적 내용' },
  { value: 'VIOLENCE', label: '불법·폭력 조장' },
  { value: 'COPYRIGHT', label: '저작권 침해' },
  { value: 'PERSONAL_INFO', label: '개인정보 노출' },
  { value: 'OTHER', label: '기타' },
];

export interface ReportView {
  id: number;
  targetType: ReportTargetType;
  targetId: number;
  reason: ReportReason;
  detail: string | null;
  status: ReportStatus;
  reporter: UserSummary;
  openReports: number;
  target: {
    title: string;
    excerpt: string | null;
    link: string | null;
    author: UserSummary | null;
    hidden: boolean;
    authorSuspended: boolean;
    exists: boolean;
  };
  resolutionNote: string | null;
  createdAt: string;
  resolvedAt: string | null;
}

export interface NotificationView {
  id: number;
  type: string;
  message: string;
  link: string | null;
  read: boolean;
  createdAt: string;
}

export interface PackageReportView {
  issues: { type: string; description: string; severity: string; workTitles: string[] }[];
  score: number;
  skippedTitles: string[];
  createdAt: string;
}


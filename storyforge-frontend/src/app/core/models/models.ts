// 기획서 v1.2 §10 엔티티와 백엔드 DTO 기준

/** 프로젝트 파이프라인 상태 (기획서 §7.2). 이름과 순서를 바꾸지 않습니다. */
export type ProjectStatus =
  | 'BIBLE_DRAFT'
  | 'BIBLE_CONFIRMED'
  | 'OUTLINE_DRAFT'
  | 'OUTLINE_CONFIRMED'
  | 'CHAPTER_GENERATING'
  | 'CHAPTER_REVIEW'
  | 'COMPLETED';

export const PIPELINE_STAGES: { status: ProjectStatus; label: string }[] = [
  { status: 'BIBLE_DRAFT', label: 'Bible 작성' },
  { status: 'BIBLE_CONFIRMED', label: 'Bible 확정' },
  { status: 'OUTLINE_DRAFT', label: 'Outline 작성' },
  { status: 'OUTLINE_CONFIRMED', label: 'Outline 확정' },
  { status: 'CHAPTER_GENERATING', label: '챕터 생성' },
  { status: 'CHAPTER_REVIEW', label: '챕터 검토' },
  { status: 'COMPLETED', label: '완료' },
];

export function stageIndex(status: ProjectStatus): number {
  return PIPELINE_STAGES.findIndex((s) => s.status === status);
}

export function stageLabel(status: ProjectStatus): string {
  return PIPELINE_STAGES[stageIndex(status)]?.label ?? status;
}

export type ExportFormat = 'md' | 'txt' | 'docx' | 'pdf' | 'html' | 'epub' | 'odt' | 'rtf';

export type ProjectForm = 'LONG' | 'SHORT';

export const EXPORT_FORMATS: { value: ExportFormat; label: string }[] = [
  { value: 'md', label: 'Markdown (.md)' },
  { value: 'docx', label: 'Word (.docx)' },
  { value: 'pdf', label: 'PDF (.pdf)' },
  { value: 'txt', label: '텍스트 (.txt)' },
  { value: 'html', label: '웹 문서 (.html)' },
  { value: 'epub', label: '전자책 (.epub)' },
  { value: 'odt', label: 'OpenDocument (.odt)' },
  { value: 'rtf', label: '서식 있는 텍스트 (.rtf)' },
];

/** 백엔드 UpdateProjectRequest가 허용하는 모델과 같게 유지합니다. */
export const MODEL_OPTIONS: { value: string; label: string }[] = [
  { value: 'claude-opus-5-5', label: 'Claude Opus 5.5' },
  { value: 'claude-sonnet-5-5', label: 'Claude Sonnet 5.5' },
  { value: 'claude-haiku-5-5', label: 'Claude Haiku 5.5' },
];

export interface User {
  id: number;
  email: string;
  displayName: string;
  bio?: string | null;
  website?: string | null;
  avatarColor?: string | null;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  user: User;
}

export interface Project {
  id: number;
  title: string;
  genre: string | null;
  status: ProjectStatus;
  model: string | null;
  defaultTone: string | null;
  exportFormat: ExportFormat;
  totalChapters: number;
  confirmedChapters: number;
  form: ProjectForm;
  /** 게시한 작품 */
  workId: number | null;
  /** Clone 출처. 원본이 지워지면 ID만 비고 제목은 남습니다. */
  clonedFromWorkId: number | null;
  clonedFromTitle: string | null;
  cloneMode: string | null;
  manualChars: number;
  agentChars: number;
  createdAt: string;
  updatedAt: string;
  /** OWNER 또는 함께 쓰는 COLLABORATOR */
  role?: 'OWNER' | 'COLLABORATOR';
}

export interface ProjectStatusInfo {
  projectId: number;
  status: ProjectStatus;
  totalChapters: number;
  confirmedChapters: number;
  nextChapterNumber: number | null;
}

export interface Character {
  id?: number | null;
  name: string;
  description: string | null;
  personality: string | null;
  relationships: string | null;
  currentState?: Record<string, unknown> | null;
}

export interface StoryBible {
  projectId: number;
  worldSetting: string | null;
  theme: string | null;
  tone: string | null;
  version: number;
  updatedAt: string;
  characters: Character[];
}

export interface OutlineChapter {
  order: number;
  title: string;
  beats: string[];
  characters: string[];
  goals: string | null;
  continuityPoints: string[];
}

export interface Outline {
  projectId: number;
  chapters: OutlineChapter[];
  updatedAt: string;
}

export interface ContinuityIssue {
  type: string;
  description: string;
  severity: 'low' | 'medium' | 'high' | string;
}

export interface ContinuityReport {
  issues: ContinuityIssue[];
  score: number;
  createdAt: string;
}

export type ChapterStatus = 'DRAFT' | 'CONFIRMED';

export interface Chapter {
  id: number;
  projectId: number;
  number: number;
  title: string | null;
  content: string | null;
  status: ChapterStatus;
  summary: string | null;
  generationContext: Record<string, unknown> | null;
  createdAt: string;
  continuityReport: ContinuityReport | null;
}

export interface ChapterVersion {
  version: number;
  source: 'GENERATED' | 'EDITED' | 'CLONED';
  title: string | null;
  content: string | null;
  createdAt: string;
}

export interface BibleSnapshot {
  worldSetting: string | null;
  theme: string | null;
  tone: string | null;
  characters: Omit<Character, 'id' | 'currentState'>[];
}

export interface BibleVersion {
  version: number;
  createdAt: string;
  snapshot: BibleSnapshot;
}

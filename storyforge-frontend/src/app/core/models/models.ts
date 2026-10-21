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

export interface Project {
  id: number;
  title: string;
  genre: string | null;
  status: ProjectStatus;
  totalChapters: number;
  confirmedChapters: number;
  createdAt: string;
  updatedAt: string;
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

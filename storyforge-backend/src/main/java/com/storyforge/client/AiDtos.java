package com.storyforge.client;

import com.storyforge.domain.chapter.ContinuityIssue;
import com.storyforge.domain.outline.OutlineChapter;

import java.util.List;
import java.util.Map;

/**
 * Spring Boot ↔ FastAPI 내부 계약. storyforge-ai/app/models/schemas.py와 필드를 맞춥니다.
 */
public final class AiDtos {

    private AiDtos() {
    }

    public record ProjectInfo(String title, String genre, String model, String defaultTone) {
    }

    public record CharacterInfo(String name, String description, String personality, String relationships,
                                Map<String, Object> currentState) {
    }

    public record BibleInfo(String worldSetting, String theme, String tone, List<CharacterInfo> characters) {
    }

    // POST /internal/generate/bible
    public record BibleGenerateRequest(ProjectInfo project, String prompt) {
    }

    public record BibleGenerateResponse(String worldSetting, String theme, String tone,
                                        List<CharacterInfo> characters) {
    }

    // POST /internal/generate/outline
    public record OutlineGenerateRequest(ProjectInfo project, BibleInfo bible, int chapterCount) {
    }

    public record OutlineGenerateResponse(List<OutlineChapter> chapters) {
    }

    // POST /internal/generate/chapter (SSE)
    public record PreviousChapter(int number, String title, String summary) {
    }

    /** 컨텍스트 패키지 (기획서 §6.3, §7.2): Bible + Outline + 이전 챕터 요약 + 캐릭터 상태 */
    public record ChapterContext(ProjectInfo project, BibleInfo bible, int chapterNumber,
                                 OutlineChapter outlineChapter, List<PreviousChapter> previousChapters) {
    }

    public record ContinuityResult(List<ContinuityIssue> issues, int score) {
    }

    // POST /internal/generate/review
    public record ChapterReviewRequest(ChapterContext context, String title, String content) {
    }

    public record ChapterReviewResult(String summary, ContinuityResult report) {
    }

    // POST /internal/guide/chat
    public record GuideContext(String displayName, int projectCount, long publishedWorks, Long projectId,
                               String projectTitle, String projectStatus, Integer nextChapter) {
    }

    public record GuideChatRequest(String message, GuideContext context) {
    }

    /** kind: route(앱 안 이동), collab(외부 연결, 백엔드가 external로 바꿈), external */
    public record GuideAction(String label, String kind, String target) {
    }

    public record GuideChatResponse(String reply, List<GuideAction> actions) {
    }

    /** 스트림 마지막 "done" 이벤트 본문 */
    public record ChapterGenerateResult(String title, String content, String summary, ContinuityResult report) {
    }
}

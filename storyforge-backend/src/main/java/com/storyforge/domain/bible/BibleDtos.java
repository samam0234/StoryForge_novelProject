package com.storyforge.domain.bible;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class BibleDtos {

    private BibleDtos() {
    }

    public record CharacterDto(
            Long id,
            @NotBlank @Size(max = 100) String name,
            String description, String personality, String relationships,
            Map<String, Object> currentState) {

        public static CharacterDto from(Character c) {
            return new CharacterDto(c.getId(), c.getName(), c.getDescription(), c.getPersonality(),
                    c.getRelationships(), c.getCurrentState());
        }

        public Character toEntity() {
            return new Character(name, description, personality, relationships, currentState);
        }
    }

    public record BibleResponse(
            Long projectId, String worldSetting, String theme, String tone,
            int version, Instant updatedAt, List<CharacterDto> characters) {

        public static BibleResponse from(StoryBible b) {
            return new BibleResponse(b.getProject().getId(), b.getWorldSetting(), b.getTheme(), b.getTone(),
                    b.getVersion(), b.getUpdatedAt(),
                    b.getCharacters().stream().map(CharacterDto::from).toList());
        }
    }

    /** 버전 이력에 저장하는 스냅샷 */
    public record BibleSnapshot(String worldSetting, String theme, String tone, List<CharacterSnapshot> characters) {

        public static BibleSnapshot from(StoryBible b) {
            return new BibleSnapshot(b.getWorldSetting(), b.getTheme(), b.getTone(),
                    b.getCharacters().stream()
                            .map(c -> new CharacterSnapshot(c.getName(), c.getDescription(), c.getPersonality(), c.getRelationships()))
                            .toList());
        }
    }

    public record CharacterSnapshot(String name, String description, String personality, String relationships) {
    }

    public record BibleVersionResponse(int version, Instant createdAt, BibleSnapshot snapshot) {
    }

    /** PUT /api/v1/projects/{id}/bible */
    public record UpdateBibleRequest(
            String worldSetting, String theme, String tone,
            @Valid List<CharacterDto> characters) {
    }

    /** POST /api/v1/projects/{id}/bible/generate — 사용자 프롬프트 */
    public record GenerateBibleRequest(@NotBlank @Size(max = 4000) String prompt) {
    }
}

package com.storyforge.domain.bible;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "character")
public class Character {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bible_id", nullable = false)
    private StoryBible bible;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String personality;

    @Column(columnDefinition = "text")
    private String relationships;

    /** 챕터 진행에 따른 캐릭터 현재 상태 (기획서 §10: currentState JSON) */
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> currentState;

    protected Character() {
    }

    public Character(String name, String description, String personality, String relationships,
                     Map<String, Object> currentState) {
        this.name = name;
        this.description = description;
        this.personality = personality;
        this.relationships = relationships;
        this.currentState = currentState;
    }

    void attachTo(StoryBible bible) {
        this.bible = bible;
    }

    public void updateState(Map<String, Object> currentState) {
        this.currentState = currentState;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getPersonality() { return personality; }
    public String getRelationships() { return relationships; }
    public Map<String, Object> getCurrentState() { return currentState; }
}

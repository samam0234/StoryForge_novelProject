package com.storyforge.domain.bible;

import com.storyforge.domain.project.Project;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "story_bible")
public class StoryBible {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private Project project;

    @Column(columnDefinition = "text")
    private String worldSetting;

    @Column(columnDefinition = "text")
    private String theme;

    @Column(columnDefinition = "text")
    private String tone;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "bible", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Character> characters = new ArrayList<>();

    protected StoryBible() {
    }

    public StoryBible(Project project) {
        this.project = project;
        this.version = 0;
        this.updatedAt = Instant.now();
    }

    /** 내용과 캐릭터 목록을 통째로 교체하고 버전을 올립니다. */
    public void replace(String worldSetting, String theme, String tone, List<Character> newCharacters) {
        this.worldSetting = worldSetting;
        this.theme = theme;
        this.tone = tone;
        this.characters.clear();
        for (Character c : newCharacters) {
            c.attachTo(this);
            this.characters.add(c);
        }
        this.version++;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public String getWorldSetting() { return worldSetting; }
    public String getTheme() { return theme; }
    public String getTone() { return tone; }
    public int getVersion() { return version; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<Character> getCharacters() { return characters; }
}

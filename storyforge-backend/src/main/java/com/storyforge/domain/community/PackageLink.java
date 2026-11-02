package com.storyforge.domain.community;

import jakarta.persistence.*;

import java.time.Instant;

/** 패키지 안 작품 사이의 연결 (세계관 지도의 선) */
@Entity
@Table(name = "package_link")
public class PackageLink {

    public enum Type { CONTINUES, SAME_WORLD, SHARED_CHARACTER, SAME_EVENT, REFERENCES }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long packageWorkId;

    @Column(nullable = false)
    private Long fromItemId;

    @Column(nullable = false)
    private Long toItemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type linkType;

    private String label;

    @Column(nullable = false)
    private Instant createdAt;

    protected PackageLink() {
    }

    public PackageLink(Long packageWorkId, Long fromItemId, Long toItemId, Type linkType, String label) {
        this.packageWorkId = packageWorkId;
        this.fromItemId = fromItemId;
        this.toItemId = toItemId;
        this.linkType = linkType;
        this.label = label;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getFromItemId() { return fromItemId; }
    public Long getToItemId() { return toItemId; }
    public Type getLinkType() { return linkType; }
    public String getLabel() { return label; }
}

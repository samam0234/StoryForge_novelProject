package com.storyforge.domain.moderation;

import jakarta.persistence.*;

import java.time.Instant;

/** 사용자 차단: blocker에게 blocked의 글이 보이지 않고, blocked는 blocker의 글에 댓글을 달 수 없습니다. */
@Entity
@Table(name = "user_block")
public class UserBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long blockerId;

    @Column(nullable = false)
    private Long blockedId;

    @Column(nullable = false)
    private Instant createdAt;

    protected UserBlock() {
    }

    public UserBlock(Long blockerId, Long blockedId) {
        this.blockerId = blockerId;
        this.blockedId = blockedId;
        this.createdAt = Instant.now();
    }

    public Long getBlockedId() { return blockedId; }
    public Instant getCreatedAt() { return createdAt; }
}

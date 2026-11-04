package com.storyforge.domain.moderation;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

/** 사용자가 다른 사용자를 보지 않기로 한 관계. 양쪽 목록에서 서로 빠집니다. */
@Entity
@Table(name = "user_block")
public class UserBlock {

    @EmbeddedId
    private UserBlockKey id;

    @Column(nullable = false)
    private Instant createdAt;

    protected UserBlock() {
    }

    public UserBlock(Long blockerId, Long blockedId) {
        this.id = new UserBlockKey(blockerId, blockedId);
        this.createdAt = Instant.now();
    }

    public UserBlockKey getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

package com.storyforge.service.community;

import com.storyforge.domain.moderation.UserBlockKey;
import com.storyforge.domain.moderation.UserBlockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 차단은 쌍방입니다. 내가 막았거나 상대가 나를 막으면 목록과 글에서 서로 빠집니다. */
@Service
@Transactional(readOnly = true)
public class BlockQuery {

    private final UserBlockRepository blocks;

    public BlockQuery(UserBlockRepository blocks) {
        this.blocks = blocks;
    }

    public boolean separated(Long viewerId, Long otherId) {
        if (viewerId == null || otherId == null || viewerId.equals(otherId)) {
            return false;
        }
        return blocks.existsById(new UserBlockKey(viewerId, otherId))
                || blocks.existsById(new UserBlockKey(otherId, viewerId));
    }

    public boolean blockedByViewer(Long viewerId, Long otherId) {
        if (viewerId == null || otherId == null) {
            return false;
        }
        return blocks.existsById(new UserBlockKey(viewerId, otherId));
    }

    /** 목록 쿼리용. 익명이면 빈 목록입니다. */
    public Collection<Long> excludedAuthorIds(Long viewerId) {
        if (viewerId == null) {
            return List.of();
        }
        Set<Long> ids = new HashSet<>(blocks.findBlockedIds(viewerId));
        ids.addAll(blocks.findBlockerIds(viewerId));
        return ids;
    }
}

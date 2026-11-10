package com.storyforge.service.moderation;

import com.storyforge.domain.user.AppUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** 호출자의 조치와 같은 트랜잭션에서 기록합니다. */
@Component
public class AdminAudit {
    private final JdbcTemplate jdbc;

    public AdminAudit(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void record(AppUser actor, String action, String type, Long targetId, Long reportId, String note) {
        jdbc.update("""
                INSERT INTO admin_audit (actor_id, actor_name, action, target_type, target_id, report_id, note)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, actor.getId(), actor.getDisplayName(), action, type, targetId, reportId,
                note == null ? "" : note.strip());
    }
}

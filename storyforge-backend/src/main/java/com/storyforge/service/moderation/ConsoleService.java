package com.storyforge.service.moderation;

import com.storyforge.domain.community.CommunityDtos.PageResponse;
import com.storyforge.domain.moderation.*;
import com.storyforge.domain.moderation.ConsoleDtos.*;
import com.storyforge.domain.moderation.ModerationDtos.DecideRequest;
import com.storyforge.domain.notification.Notification;
import com.storyforge.domain.user.AppUser;
import com.storyforge.domain.user.AppUserRepository;
import com.storyforge.exception.InvalidStateException;
import com.storyforge.exception.NotFoundException;
import com.storyforge.service.auth.AccountPolicy;
import com.storyforge.service.notification.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ConsoleService {
    private final AccountPolicy policy;
    private final AppUserRepository users;
    private final ContentReportRepository reports;
    private final ModerationService moderation;
    private final AdminAudit audit;
    private final NotificationService notifications;
    private final JdbcTemplate jdbc;

    public ConsoleService(AccountPolicy policy, AppUserRepository users, ContentReportRepository reports,
                          ModerationService moderation, AdminAudit audit, NotificationService notifications,
                          JdbcTemplate jdbc) {
        this.policy = policy;
        this.users = users;
        this.reports = reports;
        this.moderation = moderation;
        this.audit = audit;
        this.notifications = notifications;
        this.jdbc = jdbc;
    }

    public Overview overview(Long actor) {
        policy.requireAdmin(actor);
        Map<String, Long> pipeline = new LinkedHashMap<>();
        jdbc.query("SELECT status, count(*) AS total FROM project GROUP BY status ORDER BY status",
                (org.springframework.jdbc.core.RowCallbackHandler) row -> pipeline.put(row.getString("status"), row.getLong("total")));
        return new Overview(count("SELECT count(*) FROM app_user"),
                count("SELECT count(*) FROM app_user WHERE suspended_at IS NOT NULL"),
                count("SELECT count(*) FROM project"), count("SELECT count(*) FROM work"),
                count("SELECT count(*) FROM content_report WHERE status = 'OPEN'"), pipeline, Instant.now());
    }

    public PageResponse<UserView> users(String query, Boolean suspended, int page, int size, Long actor) {
        policy.requireAdmin(actor);
        String q = query.strip().toLowerCase(Locale.ROOT);
        // LIKE의 와일드카드는 검색 문자열로 처리합니다.
        String pattern = "%" + q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        Specification<AppUser> spec = (root, ignored, cb) -> {
            var name = cb.or(cb.like(cb.lower(root.get("email")), pattern, '\\'),
                    cb.like(cb.lower(root.get("displayName")), pattern, '\\'));
            if (suspended == null) return name;
            return cb.and(name, suspended ? cb.isNotNull(root.get("suspendedAt")) : cb.isNull(root.get("suspendedAt")));
        };
        var result = users.findAll(spec, PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 50),
                Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        return new PageResponse<>(result.stream().map(this::view).toList(), result.getTotalElements(), result.getNumber(), result.getSize());
    }

    @Transactional
    public UserView decideUser(Long id, UserDecision request, Long actor) {
        policy.requireAdmin(actor);
        reports.lockOperations();
        AppUser user = users.findById(id).orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        if (policy.isAdmin(user)) throw new InvalidStateException("관리자 계정은 정지할 수 없습니다.");
        if (user.isSuspended() == request.suspended()) throw new InvalidStateException("대상 상태가 변경되었습니다. 새로고침하세요.");
        if (request.suspended()) user.suspend(request.note().strip());
        else user.unsuspend();
        audit.record(users.findById(actor).orElseThrow(), request.suspended() ? "SUSPEND" : "UNSUSPEND", "USER", id, null, request.note());
        notifications.send(id, Notification.Type.MODERATION,
                (request.suspended() ? "계정 이용이 정지되었습니다. " : "계정 이용 정지가 풀렸습니다. ") + request.note().strip(), null);
        return view(user);
    }

    @Transactional
    public BatchResult decideBatch(BatchDecision request, Long actor) {
        policy.requireAdmin(actor);
        reports.lockOperations();
        List<Long> ids = request.reportIds().stream().distinct().sorted().toList();
        List<ContentReport> selected = ids.stream().map(id -> reports.findById(id)
                .orElseThrow(() -> new NotFoundException("신고를 찾을 수 없습니다: " + id))).toList();
        if (request.action() == ModerationDtos.Action.HIDE || request.action() == ModerationDtos.Action.DISMISS
                || request.action() == ModerationDtos.Action.SUSPEND) {
            if (selected.stream().anyMatch(r -> r.getStatus() != ContentReport.Status.OPEN)) {
                throw new InvalidStateException("이미 처리된 신고가 포함되어 있습니다. 목록을 새로고침하세요.");
            }
        }
        // 처리 중인 같은 대상은 기존 조치가 모든 대기 신고를 닫으므로 한 번만 조치합니다.
        Set<String> targets = new HashSet<>();
        int processed = 0;
        for (ContentReport report : selected) {
            String key = report.getTargetType() + ":" + report.getTargetId();
            if (!targets.add(key)) continue;
            moderation.decide(report.getId(), new DecideRequest(request.action(), request.note()), actor);
            processed++;
        }
        return new BatchResult(ids.size(), processed);
    }

    public PageResponse<AuditView> audit(Long actorId, int page, int size, Long actor) {
        policy.requireAdmin(actor);
        int boundedSize = Math.clamp(size, 1, 50);
        int boundedPage = Math.clamp(page, 0, 1_000_000);
        String where = actorId == null ? "" : " WHERE actor_id = ?";
        List<Object> args = new ArrayList<>();
        if (actorId != null) args.add(actorId);
        Long total = jdbc.queryForObject("SELECT count(*) FROM admin_audit" + where, Long.class, args.toArray());
        args.add(boundedSize);
        args.add((long) boundedPage * boundedSize);
        List<AuditView> items = jdbc.query("SELECT * FROM admin_audit" + where + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
                (rs, row) -> new AuditView(rs.getLong("id"), rs.getLong("actor_id"), rs.getString("actor_name"),
                        rs.getString("action"), rs.getString("target_type"), rs.getLong("target_id"),
                        rs.getObject("report_id", Long.class), rs.getString("note"), rs.getTimestamp("created_at").toInstant()), args.toArray());
        return new PageResponse<>(items, total == null ? 0 : total, boundedPage, boundedSize);
    }

    private long count(String sql) { return Objects.requireNonNull(jdbc.queryForObject(sql, Long.class)); }
    private UserView view(AppUser user) {
        return new UserView(user.getId(), user.getEmail(), user.getDisplayName(), policy.isAdmin(user),
                user.isSuspended(), user.getSuspendedReason(), user.getCreatedAt());
    }
}

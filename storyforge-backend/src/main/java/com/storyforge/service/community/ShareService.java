package com.storyforge.service.community;

import com.storyforge.domain.activity.ActivityType;
import com.storyforge.domain.community.CommunityDtos.*;
import com.storyforge.domain.community.IssueEvent;
import com.storyforge.domain.community.Work;
import com.storyforge.domain.community.WorkRepository;
import com.storyforge.domain.community.WorkShare;
import com.storyforge.domain.community.WorkShareRepository;
import com.storyforge.domain.user.AppUser;
import com.storyforge.domain.user.AppUserRepository;
import com.storyforge.exception.InvalidStateException;
import com.storyforge.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** 공유 기록과 공유 그래프. 누가 어디에 공유했는지는 공개 정보입니다 (입력 화면에서 안내). */
@Service
@Transactional(readOnly = true)
public class ShareService {

    private final WorkService workService;
    private final WorkRepository works;
    private final WorkShareRepository shares;
    private final AppUserRepository users;
    private final ActivityService activity;
    private final IssueService issues;

    public ShareService(WorkService workService, WorkRepository works, WorkShareRepository shares, AppUserRepository users,
                        ActivityService activity, IssueService issues) {
        this.workService = workService;
        this.works = works;
        this.shares = shares;
        this.users = users;
        this.activity = activity;
        this.issues = issues;
    }

    @Transactional
    public ShareCreated share(Long workId, ShareRequest request, Long userId) {
        Work work = workService.loadVisible(workId, userId);
        if (work.getVisibility() == Work.Visibility.PRIVATE) {
            throw new InvalidStateException("비공개 작품은 공유할 수 없습니다.");
        }
        AppUser user = users.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        String token = UUID.randomUUID().toString().replace("-", "");
        WorkShare share = shares.save(new WorkShare(workId, user, request.channel(), WorkService.blankToNull(request.target()), token));
        works.addShare(workId);
        issues.reached(workId, IssueEvent.Metric.TOTAL_SHARES, works.shareCount(workId));
        activity.recordWork(userId, null, work, ActivityType.WORK_SHARED, "작품 공유: " + channelLabel(request.channel()), 0);
        return new ShareCreated(ShareEntry.from(share), token);
    }

    private static String channelLabel(WorkShare.Channel channel) {
        return switch (channel) {
            case LINK -> "링크";
            case X -> "X";
            case FACEBOOK -> "Facebook";
            case KAKAO -> "카카오톡";
            case BLOG -> "블로그";
            case EMAIL -> "이메일";
            case COMMUNITY -> "커뮤니티";
            case OTHER -> "기타";
        };
    }

    public ShareGraph graph(Long workId, Long viewerId) {
        workService.loadVisible(workId, viewerId);
        List<WorkShare> list = shares.findTop500ByWorkIdOrderByCreatedAtDesc(workId);
        return new ShareGraph(list.size(), list.stream().mapToInt(s -> s.getVisitCount()).sum(),
                list.stream().map(ShareEntry::from).toList());
    }

    /** 공유 링크로 들어온 방문. 익명도 셉니다 (요청 제한은 IP 기준). */
    @Transactional
    public void visit(String token) {
        if (shares.addVisit(token) == 0) {
            throw new NotFoundException("공유 링크를 찾을 수 없습니다.");
        }
    }
}

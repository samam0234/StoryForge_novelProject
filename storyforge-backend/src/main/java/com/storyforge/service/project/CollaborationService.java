package com.storyforge.service.project;

import com.storyforge.domain.activity.ActivityType;
import com.storyforge.domain.community.CommunityDtos.UserSummary;
import com.storyforge.domain.project.CollabDtos.CollaboratorView;
import com.storyforge.domain.project.CollabDtos.CommitRequest;
import com.storyforge.domain.project.CollabDtos.CommitView;
import com.storyforge.domain.project.Project;
import com.storyforge.domain.project.ProjectCollaborator;
import com.storyforge.domain.project.ProjectCollaboratorRepository;
import com.storyforge.domain.project.WorkCommit;
import com.storyforge.domain.project.WorkCommitRepository;
import com.storyforge.domain.user.AppUser;
import com.storyforge.domain.user.AppUserRepository;
import com.storyforge.exception.InvalidStateException;
import com.storyforge.exception.NotFoundException;
import com.storyforge.service.community.ActivityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 함께 창작. 초대는 주인만, 원고와 작업 커밋은 수락한 사람만 봅니다.
 * 상태 머신은 건드리지 않습니다.
 */
@Service
@Transactional(readOnly = true)
public class CollaborationService {

    private final ProjectService projects;
    private final ProjectCollaboratorRepository members;
    private final WorkCommitRepository commits;
    private final AppUserRepository users;
    private final ActivityService activity;

    public CollaborationService(ProjectService projects, ProjectCollaboratorRepository members,
                                WorkCommitRepository commits, AppUserRepository users, ActivityService activity) {
        this.projects = projects;
        this.members = members;
        this.commits = commits;
        this.users = users;
        this.activity = activity;
    }

    public List<CollaboratorView> listMembers(Long projectId) {
        Project project = projects.load(projectId);
        List<ProjectCollaborator> rows = members.findByProjectIdOrderByCreatedAtAsc(projectId);
        Map<Long, AppUser> people = loadUsers(rows.stream().map(ProjectCollaborator::getUserId).toList());
        return rows.stream().map(m -> view(m, project.getTitle(), people.get(m.getUserId()))).toList();
    }

    @Transactional
    public CollaboratorView invite(Long projectId, Long ownerId, String email) {
        Project project = projects.load(projectId);
        if (!ownerId.equals(project.getOwnerId())) {
            throw new InvalidStateException("함께 쓸 사람을 초대할 수 있는 사람은 주인뿐입니다.");
        }
        AppUser invitee = users.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new NotFoundException("그 이메일로 가입한 사용자가 없습니다."));
        if (invitee.getId().equals(ownerId)) {
            throw new InvalidStateException("자기 자신을 초대할 수 없습니다.");
        }
        if (members.findByProjectIdAndUserId(projectId, invitee.getId()).isPresent()) {
            throw new InvalidStateException("이미 초대했거나 함께 쓰고 있는 사용자입니다.");
        }
        ProjectCollaborator row = members.save(new ProjectCollaborator(projectId, invitee.getId(), ownerId));
        return view(row, project.getTitle(), invitee);
    }

    @Transactional
    public void remove(Long projectId, Long ownerId, Long userId) {
        Project project = projects.load(projectId);
        if (!ownerId.equals(project.getOwnerId())) {
            throw new InvalidStateException("초대를 거절할 수 있는 사람은 주인뿐입니다.");
        }
        ProjectCollaborator row = members.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new NotFoundException("초대 기록을 찾을 수 없습니다."));
        members.delete(row);
    }

    public List<CollaboratorView> myInvitations(Long userId) {
        List<ProjectCollaborator> rows = members.findByUserIdOrderByCreatedAtDesc(userId);
        Map<Long, Project> byId = rows.stream().map(ProjectCollaborator::getProjectId).distinct()
                .map(projects::load)
                .collect(Collectors.toMap(Project::getId, Function.identity()));
        AppUser me = users.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        return rows.stream().map(row -> view(row, byId.get(row.getProjectId()).getTitle(), me)).toList();
    }

    @Transactional
    public CollaboratorView accept(Long invitationId, Long userId) {
        ProjectCollaborator row = members.findById(Objects.requireNonNull(invitationId))
                .orElseThrow(() -> new NotFoundException("초대를 찾을 수 없습니다."));
        if (!userId.equals(row.getUserId())) {
            throw new NotFoundException("초대를 찾을 수 없습니다.");
        }
        if (row.getStatus() == ProjectCollaborator.Status.PENDING) {
            row.accept();
        }
        Project project = projects.load(row.getProjectId());
        AppUser me = users.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        return view(row, project.getTitle(), me);
    }

    @Transactional
    public void decline(Long invitationId, Long userId) {
        ProjectCollaborator row = members.findById(Objects.requireNonNull(invitationId))
                .orElseThrow(() -> new NotFoundException("초대를 찾을 수 없습니다."));
        if (!userId.equals(row.getUserId()) || row.getStatus() != ProjectCollaborator.Status.PENDING) {
            throw new NotFoundException("초대를 찾을 수 없습니다.");
        }
        members.delete(row);
    }

    public List<CommitView> listCommits(Long projectId) {
        projects.load(projectId);
        List<WorkCommit> rows = commits.findByProjectIdOrderByCreatedAtDesc(projectId);
        Map<Long, AppUser> people = loadUsers(rows.stream().map(WorkCommit::getAuthorId).distinct().toList());
        return rows.stream().map(c -> commitView(c, people.get(c.getAuthorId()))).toList();
    }

    public CommitView getCommit(Long projectId, Long commitId) {
        projects.load(projectId);
        WorkCommit row = commits.findByIdAndProjectId(commitId, projectId)
                .orElseThrow(() -> new NotFoundException("작업 커밋을 찾을 수 없습니다."));
        return commitView(row, users.findById(row.getAuthorId()).orElse(null));
    }

    @Transactional
    public CommitView commit(Long projectId, Long authorId, CommitRequest request) {
        Project project = projects.load(projectId);
        WorkCommit.Target target = WorkCommit.Target.valueOf(request.target());
        Integer chapter = request.chapterNumber();
        if (target == WorkCommit.Target.CHAPTER && (chapter == null || chapter < 1)) {
            throw new InvalidStateException("챕터 작업은 장 번호가 필요합니다.");
        }
        if (target != WorkCommit.Target.CHAPTER) {
            chapter = null;
        }
        String quote = blankToNull(request.quote());
        Integer start = request.startOffset();
        Integer end = request.endOffset();
        if (quote == null) {
            start = null;
            end = null;
        } else if (start != null || end != null) {
            if (start == null || end == null || start < 0 || end < start || end > quote.length()) {
                throw new InvalidStateException("표시 구간이 인용문 밖입니다.");
            }
        }
        Long parent = commits.findFirstByProjectIdOrderByCreatedAtDesc(projectId).map(WorkCommit::getId).orElse(null);
        WorkCommit saved = commits.save(new WorkCommit(projectId, authorId, parent, request.message().strip(),
                target, chapter, start, end, quote));
        int chars = quote == null ? 0 : (end != null && start != null ? end - start : quote.length());
        activity.recordFor(authorId, project, ActivityType.WORK_COMMITTED, "작업 커밋", chars);
        return commitView(saved, users.findById(Objects.requireNonNull(authorId)).orElse(null));
    }

    private Map<Long, AppUser> loadUsers(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return users.findAllById(ids).stream().collect(Collectors.toMap(AppUser::getId, Function.identity()));
    }

    private static CollaboratorView view(ProjectCollaborator row, String title, AppUser user) {
        return new CollaboratorView(row.getId(), row.getProjectId(), title,
                user == null ? new UserSummary(row.getUserId(), "알 수 없음", null) : UserSummary.from(user),
                row.getStatus().name(), row.getCreatedAt());
    }

    private static CommitView commitView(WorkCommit row, AppUser author) {
        return new CommitView(row.getId(), row.getProjectId(),
                author == null ? new UserSummary(row.getAuthorId(), "알 수 없음", null) : UserSummary.from(author),
                row.getParentId(), row.getMessage(), row.getTarget().name(), row.getChapterNumber(),
                row.getStartOffset(), row.getEndOffset(), row.getQuote(), row.getCreatedAt());
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}

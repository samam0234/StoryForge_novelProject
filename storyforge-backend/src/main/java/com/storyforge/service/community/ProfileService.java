package com.storyforge.service.community;

import com.storyforge.domain.community.*;
import com.storyforge.domain.community.CommunityDtos.*;
import com.storyforge.domain.moderation.UserBlockRepository;
import com.storyforge.domain.user.AppUser;
import com.storyforge.domain.user.AppUserRepository;
import com.storyforge.domain.user.AuthDtos.UserResponse;
import com.storyforge.exception.InvalidStateException;
import com.storyforge.service.auth.AccountPolicy;
import com.storyforge.exception.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** 유저 프로필과 계정 설정. 공개 프로필에는 이메일을 넣지 않습니다. */
@Service
@Transactional(readOnly = true)
public class ProfileService {

    private static final int BCRYPT_MAX_BYTES = 72;

    private final AppUserRepository users;
    private final WorkRepository works;
    private final PostRepository posts;
    private final CommentRepository comments;
    private final AchievementService achievements;
    private final PasswordEncoder passwordEncoder;
    private final AccountPolicy policy;
    private final UserBlockRepository blocks;
    private final ContentPolicy contentPolicy;

    public ProfileService(AppUserRepository users, WorkRepository works, PostRepository posts, CommentRepository comments,
                          AchievementService achievements, PasswordEncoder passwordEncoder,
                          AccountPolicy policy, UserBlockRepository blocks, ContentPolicy contentPolicy) {
        this.contentPolicy = contentPolicy;
        this.policy = policy;
        this.blocks = blocks;
        this.users = users;
        this.works = works;
        this.posts = posts;
        this.comments = comments;
        this.achievements = achievements;
        this.passwordEncoder = passwordEncoder;
    }

    public Profile profile(Long userId, Long viewerId) {
        AppUser user = load(userId);
        ProfileStats stats = new ProfileStats(works.countByAuthorIdAndVisibilityAndHiddenAtIsNull(userId, Work.Visibility.PUBLIC),
                works.sumLikes(userId), works.sumClones(userId), posts.countByAuthorId(userId),
                comments.countByAuthorId(userId), achievements.earnedCount(userId));
        return new Profile(UserSummary.from(user), user.getBio(), user.getWebsite(), user.getCreatedAt(),
                userId.equals(viewerId),
                viewerId != null && blocks.existsByBlockerIdAndBlockedId(viewerId, userId), user.isSuspended(), stats);
    }

    public void requireExists(Long userId) {
        load(userId);
    }

    @Transactional
    public UserResponse update(Long userId, UpdateProfileRequest request) {
        contentPolicy.check(request.displayName(), request.bio());
        AppUser user = load(userId);
        user.updateProfile(request.displayName().strip(), WorkService.blankToNull(request.bio()),
                WorkService.blankToNull(request.website()), WorkService.blankToNull(request.avatarColor()));
        return policy.view(user);
    }

    /** 현재 버전의 이용약관에 동의합니다. */
    @Transactional
    public UserResponse acceptTerms(Long userId) {
        AppUser user = load(userId);
        user.acceptTerms(policy.termsVersion());
        return policy.view(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        AppUser user = load(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidStateException("현재 비밀번호가 올바르지 않습니다.");
        }
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            throw new InvalidStateException("비밀번호가 너무 깁니다.");
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    /** 계정 삭제. 내 프로젝트·작품·댓글이 함께 지워지고, 다른 사람의 Clone은 남습니다. */
    @Transactional
    public void delete(Long userId, DeleteAccountRequest request) {
        AppUser user = load(userId);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidStateException("비밀번호가 올바르지 않습니다.");
        }
        users.delete(user);
    }

    private AppUser load(Long userId) {
        return users.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다: " + userId));
    }
}

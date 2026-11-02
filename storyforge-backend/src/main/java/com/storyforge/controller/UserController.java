package com.storyforge.controller;

import com.storyforge.domain.activity.ActivityDtos.AchievementView;
import com.storyforge.domain.activity.ActivityDtos.ActivityEntry;
import com.storyforge.domain.activity.ActivityDtos.Contributions;
import com.storyforge.domain.community.CommunityDtos.*;
import com.storyforge.domain.user.AuthDtos.UserResponse;
import com.storyforge.service.auth.CurrentUser;
import com.storyforge.service.community.AchievementService;
import com.storyforge.service.community.ActivityService;
import com.storyforge.service.community.IssueService;
import com.storyforge.service.community.ProfileService;
import com.storyforge.service.community.WorkService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 공개 프로필, 활동 잔디, 업적, 이슈 게시판, 계정 설정 */
@RestController
@RequestMapping("/api/v1")
public class UserController {

    private final ProfileService profiles;
    private final WorkService works;
    private final ActivityService activity;
    private final AchievementService achievements;
    private final IssueService issues;
    private final CurrentUser currentUser;

    public UserController(ProfileService profiles, WorkService works, ActivityService activity,
                          AchievementService achievements, IssueService issues, CurrentUser currentUser) {
        this.profiles = profiles;
        this.works = works;
        this.activity = activity;
        this.achievements = achievements;
        this.issues = issues;
        this.currentUser = currentUser;
    }

    @GetMapping("/users/{userId}")
    public Profile profile(@PathVariable Long userId) {
        return profiles.profile(userId, currentUser.idOrNull());
    }

    @GetMapping("/users/{userId}/works")
    public List<WorkCard> works(@PathVariable Long userId) {
        profiles.requireExists(userId);
        return works.publicWorksOf(userId);
    }

    @GetMapping("/users/{userId}/contributions")
    public Contributions contributions(@PathVariable Long userId) {
        profiles.requireExists(userId);
        return activity.contributions(userId);
    }

    /** 활동 내역. 본인이 아니면 공개 작품·게시글에 연결된 기록만 돌려줍니다. */
    @GetMapping("/users/{userId}/activity")
    public PageResponse<ActivityEntry> activity(@PathVariable Long userId, @RequestParam(required = false) String date,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        profiles.requireExists(userId);
        return activity.feed(userId, userId.equals(currentUser.idOrNull()), date, Math.max(page, 0), Math.clamp(size, 1, 50));
    }

    @GetMapping("/users/{userId}/achievements")
    public List<AchievementView> achievements(@PathVariable Long userId) {
        profiles.requireExists(userId);
        return achievements.list(userId);
    }

    @GetMapping("/issues")
    public IssueBoard issues() {
        return issues.board();
    }

    // ───────────── 계정 ─────────────

    @GetMapping("/me/works")
    public List<WorkCard> myWorks() {
        return works.mine(currentUser.id());
    }

    @PutMapping("/me/profile")
    public UserResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return profiles.update(currentUser.id(), request);
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        profiles.changePassword(currentUser.id(), request);
    }

    /** 본문이 필요해 DELETE 대신 POST를 씁니다. */
    @PostMapping("/me/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@Valid @RequestBody DeleteAccountRequest request) {
        profiles.delete(currentUser.id(), request);
    }
}

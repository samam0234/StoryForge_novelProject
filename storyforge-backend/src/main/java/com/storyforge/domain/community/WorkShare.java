package com.storyforge.domain.community;

import com.storyforge.domain.user.AppUser;
import jakarta.persistence.*;

import java.time.Instant;

/** 공유 기록: 누가, 어디에 공유했는지와 그 링크로 들어온 방문 수 */
@Entity
@Table(name = "work_share")
public class WorkShare {

    public enum Channel { LINK, X, FACEBOOK, KAKAO, BLOG, EMAIL, COMMUNITY, OTHER }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long workId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Channel channel;

    private String target;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private int visitCount;

    @Column(nullable = false)
    private Instant createdAt;

    protected WorkShare() {
    }

    public WorkShare(Long workId, AppUser user, Channel channel, String target, String token) {
        this.workId = workId;
        this.user = user;
        this.channel = channel;
        this.target = target;
        this.token = token;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getWorkId() { return workId; }
    public AppUser getUser() { return user; }
    public Channel getChannel() { return channel; }
    public String getTarget() { return target; }
    public String getToken() { return token; }
    public int getVisitCount() { return visitCount; }
    public Instant getCreatedAt() { return createdAt; }
}

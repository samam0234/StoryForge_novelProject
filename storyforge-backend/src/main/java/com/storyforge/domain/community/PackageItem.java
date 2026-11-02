package com.storyforge.domain.community;

import jakarta.persistence.*;

import java.time.Instant;

/** 패키지에 묶인 단독 작품. 순서(position)가 곧 추천 독서 순서입니다. */
@Entity
@Table(name = "package_item")
public class PackageItem {

    public enum Role { MAIN, SIDE, PREQUEL, SEQUEL, SPINOFF, INTERLUDE }

    public enum Status { PENDING, APPROVED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long packageWorkId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_work_id", nullable = false)
    private Work member;

    @Column(nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private Instant createdAt;

    protected PackageItem() {
    }

    public PackageItem(Long packageWorkId, Work member, int position, Role role, String note, Status status) {
        this.packageWorkId = packageWorkId;
        this.member = member;
        this.position = position;
        this.role = role;
        this.note = note;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public void update(Role role, String note) {
        this.role = role;
        this.note = note;
    }

    public void moveTo(int position) {
        this.position = position;
    }

    public void approve() {
        this.status = Status.APPROVED;
    }

    public Long getId() { return id; }
    public Long getPackageWorkId() { return packageWorkId; }
    public Work getMember() { return member; }
    public int getPosition() { return position; }
    public Role getRole() { return role; }
    public String getNote() { return note; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}

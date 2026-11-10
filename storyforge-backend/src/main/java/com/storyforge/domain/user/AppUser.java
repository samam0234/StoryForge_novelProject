package com.storyforge.domain.user;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private Instant emailVerifiedAt;

    @Column(nullable = false)
    private long credentialVersion;

    @Version
    private long rowVersion;

    @Column(nullable = false)
    private String displayName;

    private String bio;

    private String website;

    private String avatarColor;

    private Instant suspendedAt;

    private String suspendedReason;

    @SuppressWarnings("unused") // 언제 동의했는지 남기는 기록. 판단에는 약관 버전을 씁니다.
    private Instant termsAcceptedAt;

    private String termsVersion;

    @Column(nullable = false)
    private Instant createdAt;

    protected AppUser() {
    }

    public AppUser(String email, String passwordHash, String displayName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.createdAt = Instant.now();
    }

    public void updateProfile(String displayName, String bio, String website, String avatarColor) {
        this.displayName = displayName;
        this.bio = bio;
        this.website = website;
        this.avatarColor = avatarColor;
    }

    public void suspend(String reason) {
        this.suspendedAt = Instant.now();
        this.suspendedReason = reason;
    }

    public void unsuspend() {
        this.suspendedAt = null;
        this.suspendedReason = null;
    }

    public void acceptTerms(String version) {
        this.termsAcceptedAt = Instant.now();
        this.termsVersion = version;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.credentialVersion++;
    }

    public void verifyEmail() { this.emailVerifiedAt = Instant.now(); }
    public boolean isEmailVerified() { return emailVerifiedAt != null; }
    public long getCredentialVersion() { return credentialVersion; }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public boolean isSuspended() { return suspendedAt != null; }
    public String getSuspendedReason() { return suspendedReason; }
    public String getTermsVersion() { return termsVersion; }
    public String getBio() { return bio; }
    public String getWebsite() { return website; }
    public String getAvatarColor() { return avatarColor; }
    public Instant getCreatedAt() { return createdAt; }
}

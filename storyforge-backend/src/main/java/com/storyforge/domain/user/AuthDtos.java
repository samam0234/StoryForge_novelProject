package com.storyforge.domain.user;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 100) String displayName,
            @AssertTrue(message = "이용약관에 동의해야 가입할 수 있습니다.") boolean termsAccepted) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record UserResponse(Long id, String email, String displayName, String bio, String website, String avatarColor, Role role) {

        public static UserResponse from(AppUser u) {
            return new UserResponse(u.getId(), u.getEmail(), u.getDisplayName(), u.getBio(), u.getWebsite(),
                    u.getAvatarColor(), u.getRole());
        }
    }

    public record AuthResponse(String accessToken, String tokenType, Instant expiresAt, UserResponse user) {
    }
}

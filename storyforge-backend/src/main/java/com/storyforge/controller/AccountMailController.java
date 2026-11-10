package com.storyforge.controller;

import com.storyforge.service.auth.AccountMailService;
import com.storyforge.service.auth.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/mail")
public class AccountMailController {
    private final AccountMailService service;
    private final CurrentUser currentUser;
    public AccountMailController(AccountMailService service, CurrentUser currentUser) {
        this.service = service; this.currentUser = currentUser;
    }
    public record EmailRequest(@NotBlank @Email @Size(max=254) String email) { }
    public record TokenRequest(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{43}") String token) { }
    public record ResetRequest(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{43}") String token,
                               @NotBlank @Size(min=8,max=72) String password) { }
    @GetMapping("/status")
    public Map<String, Boolean> status() { return Map.of("enabled", service.enabled()); }
    @PostMapping("/reset-request")
    public Map<String, String> request(@Valid @RequestBody EmailRequest body) {
        service.requestReset(body.email());
        return Map.of("message", "가입된 사용 가능한 계정이라면 메일을 보냈습니다. 잠시 후 메일함을 확인하세요.");
    }
    @PostMapping("/verify-request")
    public Map<String, String> verifyRequest() {
        service.requestVerification(currentUser.id());
        return Map.of("message", "인증 메일을 요청했습니다. 재발송은 1분 뒤 가능합니다.");
    }
    @PostMapping("/verify")
    public void verify(@Valid @RequestBody TokenRequest body) { service.consume(body.token(), "verify", null); }
    @PostMapping("/reset")
    public void reset(@Valid @RequestBody ResetRequest body) { service.consume(body.token(), "reset", body.password()); }
}

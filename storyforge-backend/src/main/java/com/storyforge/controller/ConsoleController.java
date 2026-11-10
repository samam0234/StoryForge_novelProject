package com.storyforge.controller;

import com.storyforge.domain.community.CommunityDtos.PageResponse;
import com.storyforge.domain.moderation.ConsoleDtos.*;
import com.storyforge.service.auth.CurrentUser;
import com.storyforge.service.moderation.ConsoleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class ConsoleController {
    private final ConsoleService console;
    private final CurrentUser currentUser;

    public ConsoleController(ConsoleService console, CurrentUser currentUser) {
        this.console = console;
        this.currentUser = currentUser;
    }

    @GetMapping("/overview")
    public Overview overview() { return console.overview(currentUser.id()); }

    @GetMapping("/users")
    public PageResponse<UserView> users(@RequestParam(defaultValue = "") @Size(max = 200) String q,
            @RequestParam(required = false) Boolean suspended, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return console.users(q, suspended, page, size, currentUser.id());
    }

    @PutMapping("/users/{id}/suspension")
    public UserView decideUser(@PathVariable Long id, @Valid @RequestBody UserDecision request) {
        return console.decideUser(id, request, currentUser.id());
    }

    @PostMapping("/reports/batch")
    public BatchResult batch(@Valid @RequestBody BatchDecision request) {
        return console.decideBatch(request, currentUser.id());
    }

    @GetMapping("/audit")
    public PageResponse<AuditView> audit(@RequestParam(required = false) Long actorId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return console.audit(actorId, page, size, currentUser.id());
    }
}

package com.storyforge.controller;

import com.storyforge.domain.community.CommunityDtos.*;
import com.storyforge.service.auth.CurrentUser;
import com.storyforge.service.community.PackageContinuityService;
import com.storyforge.service.community.PackageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 패키지포지 편집. 읽기는 WorkController의 작품 상세에 포함됩니다. */
@RestController
@RequestMapping("/api/v1")
public class PackageController {

    private final PackageService packages;
    private final PackageContinuityService continuity;
    private final CurrentUser currentUser;

    public PackageController(PackageService packages, PackageContinuityService continuity, CurrentUser currentUser) {
        this.continuity = continuity;
        this.packages = packages;
        this.currentUser = currentUser;
    }

    @PostMapping("/packages")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkCard create(@Valid @RequestBody CreatePackageRequest request) {
        return packages.create(request, currentUser.id());
    }

    @PostMapping("/packages/{packageId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public PackageItemView addItem(@PathVariable Long packageId, @Valid @RequestBody AddItemRequest request) {
        return packages.addItem(packageId, request, currentUser.id());
    }

    @PutMapping("/packages/{packageId}/items/{itemId}")
    public PackageItemView updateItem(@PathVariable Long packageId, @PathVariable Long itemId,
                                      @Valid @RequestBody UpdateItemRequest request) {
        return packages.updateItem(packageId, itemId, request, currentUser.id());
    }

    @PutMapping("/packages/{packageId}/order")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@PathVariable Long packageId, @Valid @RequestBody ReorderRequest request) {
        packages.reorder(packageId, request, currentUser.id());
    }

    @DeleteMapping("/packages/{packageId}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(@PathVariable Long packageId, @PathVariable Long itemId) {
        packages.removeItem(packageId, itemId, currentUser.id());
    }

    @PostMapping("/packages/{packageId}/links")
    @ResponseStatus(HttpStatus.CREATED)
    public PackageLinkView addLink(@PathVariable Long packageId, @Valid @RequestBody AddLinkRequest request) {
        return packages.addLink(packageId, request, currentUser.id());
    }

    @DeleteMapping("/packages/{packageId}/links/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeLink(@PathVariable Long packageId, @PathVariable Long linkId) {
        packages.removeLink(packageId, linkId, currentUser.id());
    }

    /** 패키지 전체 Continuity 검사. AI를 호출하므로 생성과 같은 요청 제한을 받습니다. */
    @PostMapping("/packages/{packageId}/continuity")
    public PackageReportView checkContinuity(@PathVariable Long packageId) {
        return continuity.run(packageId, currentUser.id());
    }

    /** 최근 검사 결과. 없으면 본문이 비어 있습니다. */
    @GetMapping("/packages/{packageId}/continuity")
    public PackageReportView latestContinuity(@PathVariable Long packageId) {
        return continuity.latest(packageId, currentUser.id());
    }

    /** 내 작품을 넣고 싶다는 다른 작가의 요청 */
    @GetMapping("/me/package-requests")
    public List<PackageRequest> requests() {
        return packages.pendingFor(currentUser.id());
    }

    @PutMapping("/me/package-requests/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decide(@PathVariable Long itemId, @RequestBody DecideRequest request) {
        packages.decide(itemId, request.approve(), currentUser.id());
    }
}

package com.storyforge.service.community;

import com.storyforge.domain.activity.ActivityType;
import com.storyforge.domain.bible.BibleDtos.BibleSnapshot;
import com.storyforge.domain.bible.BibleDtos.CharacterSnapshot;
import com.storyforge.domain.community.*;
import com.storyforge.domain.community.CommunityDtos.*;
import com.storyforge.domain.moderation.UserBlockRepository;
import com.storyforge.domain.notification.Notification;
import com.storyforge.domain.user.AppUser;
import com.storyforge.domain.user.AppUserRepository;
import com.storyforge.exception.InvalidStateException;
import com.storyforge.exception.NotFoundException;
import com.storyforge.service.auth.AccountPolicy;
import com.storyforge.service.notification.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 패키지포지: 단독 작품(롱품·숏품)을 묶어 유니버스, 옴니버스 등으로 통합합니다.
 * 내 작품은 바로 들어가고, 다른 작가의 작품은 그 작가가 승인해야 독자에게 보입니다.
 */
@Service
@Transactional(readOnly = true)
public class PackageService {

    private final WorkRepository works;
    private final PackageItemRepository items;
    private final PackageLinkRepository links;
    private final AppUserRepository users;
    private final ActivityService activity;
    private final AccountPolicy policy;
    private final UserBlockRepository blocks;
    private final NotificationService notifications;

    public PackageService(WorkRepository works, PackageItemRepository items, PackageLinkRepository links,
                          AppUserRepository users, ActivityService activity, AccountPolicy policy,
                          UserBlockRepository blocks, NotificationService notifications) {
        this.policy = policy;
        this.blocks = blocks;
        this.notifications = notifications;
        this.works = works;
        this.items = items;
        this.links = links;
        this.users = users;
        this.activity = activity;
    }

    /** 내가 직접 게시한 작품이 하나 이상 있어야 패키지를 만들 수 있습니다. */
    @Transactional
    public WorkCard create(CreatePackageRequest request, Long userId) {
        policy.requireTerms(userId);
        AppUser author = users.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        List<Work> members = new ArrayList<>();
        for (Long id : new LinkedHashSet<>(request.memberWorkIds())) {
            Work member = member(id, userId);
            if (!member.getAuthor().getId().equals(userId)) {
                throw new InvalidStateException("패키지를 만들 때는 내가 게시한 작품만 고를 수 있습니다. 다른 작가의 작품은 만든 뒤 요청하세요.");
            }
            members.add(member);
        }
        Work work = new Work(author, Work.Kind.PACKAGE, request.title().strip());
        work.updateMeta(request.title().strip(), WorkService.blankToNull(request.synopsis()),
                WorkService.blankToNull(request.genre()), WorkService.blankToNull(request.tags()), request.visibility(),
                Work.ClonePolicy.CLOSED, true);
        work.updatePackage(request.packageType(), WorkService.blankToNull(request.worldNote()));
        Work saved = works.save(work);
        int position = 1;
        for (Work member : members) {
            items.save(new PackageItem(saved.getId(), member, position++, PackageItem.Role.MAIN, null, PackageItem.Status.APPROVED));
        }
        recount(saved);
        activity.recordWork(userId, null, saved, ActivityType.PACKAGE_CREATED, "패키지 생성", 0);
        return WorkCard.from(saved);
    }

    @Transactional
    public PackageItemView addItem(Long packageId, AddItemRequest request, Long userId) {
        Work pkg = owned(packageId, userId);
        if (items.existsByPackageWorkIdAndMemberId(packageId, request.memberWorkId())) {
            throw new InvalidStateException("이미 패키지에 있는 작품입니다.");
        }
        Work member = member(request.memberWorkId(), userId);
        boolean mine = member.getAuthor().getId().equals(userId);
        if (!mine && blocks.existsByBlockerIdAndBlockedId(member.getAuthor().getId(), userId)) {
            throw new InvalidStateException("이 작가에게는 편입을 요청할 수 없습니다.");
        }
        int position = items.findWithMembers(packageId).size() + 1;
        PackageItem item = items.save(new PackageItem(packageId, member, position, request.role(),
                WorkService.blankToNull(request.note()), mine ? PackageItem.Status.APPROVED : PackageItem.Status.PENDING));
        recount(pkg);
        activity.recordWork(userId, null, pkg, ActivityType.PACKAGE_UPDATED, mine ? "패키지에 작품 추가" : "패키지 편입 요청", 0);
        if (!mine) {
            notifications.notify(member.getAuthor().getId(), userId, Notification.Type.PACKAGE_REQUEST,
                    pkg.getAuthor().getDisplayName() + "님이 「" + member.getTitle() + "」을(를) 패키지 「" + pkg.getTitle() + "」에 넣고 싶어 합니다.",
                    "/me/works");
        }
        return PackageItemView.from(item);
    }

    @Transactional
    public PackageItemView updateItem(Long packageId, Long itemId, UpdateItemRequest request, Long userId) {
        owned(packageId, userId);
        PackageItem item = item(packageId, itemId);
        item.update(request.role(), WorkService.blankToNull(request.note()));
        return PackageItemView.from(item);
    }

    /** 독서 순서 변경. 패키지의 항목을 빠짐없이 보내야 합니다. */
    @Transactional
    public void reorder(Long packageId, ReorderRequest request, Long userId) {
        owned(packageId, userId);
        List<PackageItem> current = items.findWithMembers(packageId);
        Set<Long> requested = new HashSet<>(request.itemIds());
        Set<Long> existing = new HashSet<>();
        current.forEach(i -> existing.add(i.getId()));
        if (requested.size() != request.itemIds().size() || !requested.equals(existing)) {
            throw new InvalidStateException("패키지의 모든 작품을 한 번씩 보내야 합니다.");
        }
        Map<Long, Integer> order = new HashMap<>();
        for (int i = 0; i < request.itemIds().size(); i++) {
            order.put(request.itemIds().get(i), i + 1);
        }
        current.forEach(i -> i.moveTo(order.get(i.getId())));
    }

    /** 패키지 주인이나 편입된 작품의 작가가 뺄 수 있습니다. */
    @Transactional
    public void removeItem(Long packageId, Long itemId, Long userId) {
        Work pkg = works.findWithAuthor(Objects.requireNonNull(packageId))
                .filter(w -> w.getKind() == Work.Kind.PACKAGE)
                .orElseThrow(() -> new NotFoundException("패키지를 찾을 수 없습니다: " + packageId));
        PackageItem item = item(packageId, itemId);
        boolean allowed = pkg.getAuthor().getId().equals(userId) || item.getMember().getAuthor().getId().equals(userId);
        if (!allowed) {
            throw new NotFoundException("패키지를 찾을 수 없습니다: " + packageId);
        }
        items.delete(item);
        items.flush();
        recount(pkg);
    }

    @Transactional
    public PackageLinkView addLink(Long packageId, AddLinkRequest request, Long userId) {
        owned(packageId, userId);
        if (request.fromItemId().equals(request.toItemId())) {
            throw new InvalidStateException("서로 다른 작품을 연결해야 합니다.");
        }
        item(packageId, request.fromItemId());
        item(packageId, request.toItemId());
        return PackageLinkView.from(links.save(new PackageLink(packageId, request.fromItemId(), request.toItemId(),
                request.type(), WorkService.blankToNull(request.label()))));
    }

    @Transactional
    public void removeLink(Long packageId, Long linkId, Long userId) {
        owned(packageId, userId);
        links.delete(links.findByIdAndPackageWorkId(linkId, packageId)
                .orElseThrow(() -> new NotFoundException("연결을 찾을 수 없습니다: " + linkId)));
    }

    public List<PackageRequest> pendingFor(Long userId) {
        return items.findPendingFor(userId).stream().map(i -> {
            Work pkg = works.findWithAuthor(i.getPackageWorkId()).orElseThrow();
            return new PackageRequest(i.getId(), pkg.getId(), pkg.getTitle(), UserSummary.from(pkg.getAuthor()),
                    WorkCard.from(i.getMember()), i.getCreatedAt());
        }).toList();
    }

    /** 내 작품을 다른 사람의 패키지에 넣는 요청 승인·거절. 거절하면 항목을 지웁니다. */
    @Transactional
    public void decide(Long itemId, boolean approve, Long userId) {
        PackageItem item = items.findById(Objects.requireNonNull(itemId))
                .filter(i -> i.getMember().getAuthor().getId().equals(userId))
                .orElseThrow(() -> new NotFoundException("요청을 찾을 수 없습니다: " + itemId));
        Work pkg = works.findWithAuthor(item.getPackageWorkId()).orElseThrow();
        notifications.notify(pkg.getAuthor().getId(), userId, Notification.Type.PACKAGE_DECISION,
                "「" + item.getMember().getTitle() + "」의 패키지 편입 요청이 " + (approve ? "승인" : "거절") + "되었습니다.",
                "/package-forge/" + pkg.getId());
        if (approve) {
            item.approve();
        } else {
            items.delete(item);
            items.flush();
        }
        recount(pkg);
    }

    /** 독자에게 보이는 구성. 주인은 승인 대기 작품도 봅니다. */
    public PackageView view(Work pkg, Long viewerId) {
        boolean owner = pkg.getAuthor().getId().equals(viewerId);
        List<PackageItem> all = items.findWithMembers(pkg.getId());
        List<PackageItem> visible = all.stream()
                .filter(i -> owner || i.getStatus() == PackageItem.Status.APPROVED)
                .filter(i -> (i.getMember().getVisibility() != Work.Visibility.PRIVATE && !i.getMember().isHidden())
                        || i.getMember().getAuthor().getId().equals(viewerId))
                .toList();
        Set<Long> ids = new HashSet<>();
        visible.forEach(i -> ids.add(i.getId()));
        List<PackageLinkView> linkViews = links.findByPackageWorkIdOrderById(pkg.getId()).stream()
                .filter(l -> ids.contains(l.getFromItemId()) && ids.contains(l.getToItemId()))
                .map(PackageLinkView::from)
                .toList();
        return new PackageView(pkg.getWorldNote(), visible.stream().map(PackageItemView::from).toList(), linkViews, characters(visible));
    }

    /** 통합 인물 사전. 세계관을 공개한 작품의 인물만 넣습니다. */
    private static List<PackageCharacter> characters(List<PackageItem> visible) {
        Map<String, List<CharacterAppearance>> byName = new LinkedHashMap<>();
        for (PackageItem item : visible) {
            Work member = item.getMember();
            BibleSnapshot bible = member.getBibleSnapshot();
            if (item.getStatus() != PackageItem.Status.APPROVED || bible == null || bible.characters() == null
                    || member.getClonePolicy() == Work.ClonePolicy.CLOSED) {
                continue;
            }
            for (CharacterSnapshot c : bible.characters()) {
                byName.computeIfAbsent(c.name(), n -> new ArrayList<>())
                        .add(new CharacterAppearance(member.getId(), member.getTitle(), c.description()));
            }
        }
        return byName.entrySet().stream()
                .map(e -> new PackageCharacter(e.getKey(), e.getValue().size() > 1, e.getValue()))
                .sorted(Comparator.comparing((PackageCharacter c) -> c.crossover()).reversed().thenComparing(c -> c.name()))
                .toList();
    }

    private Work owned(Long packageId, Long userId) {
        return works.findWithAuthor(Objects.requireNonNull(packageId))
                .filter(w -> w.getKind() == Work.Kind.PACKAGE && w.getAuthor().getId().equals(userId))
                .orElseThrow(() -> new NotFoundException("패키지를 찾을 수 없습니다: " + packageId));
    }

    private PackageItem item(Long packageId, Long itemId) {
        return items.findByIdAndPackageWorkId(itemId, packageId)
                .orElseThrow(() -> new NotFoundException("패키지 항목을 찾을 수 없습니다: " + itemId));
    }

    /** 패키지는 패키지를 담지 못합니다. 다른 사람의 작품은 공개 작품만 고를 수 있습니다. */
    private Work member(Long workId, Long userId) {
        Work member = works.findWithAuthor(Objects.requireNonNull(workId))
                .filter(w -> w.getAuthor().getId().equals(userId) || w.isListed())
                .orElseThrow(() -> new NotFoundException("작품을 찾을 수 없습니다: " + workId));
        if (member.getKind() == Work.Kind.PACKAGE) {
            throw new InvalidStateException("패키지 안에 패키지를 넣을 수 없습니다.");
        }
        return member;
    }

    /** 카드에 보일 묶음 수치: 승인된 작품 수와 글자 수 합계 */
    private void recount(Work pkg) {
        List<PackageItem> approved = items.findWithMembers(pkg.getId()).stream()
                .filter(i -> i.getStatus() == PackageItem.Status.APPROVED)
                .toList();
        pkg.applyCounts(approved.size(), approved.stream().mapToInt(i -> i.getMember().getCharCount()).sum());
    }
}

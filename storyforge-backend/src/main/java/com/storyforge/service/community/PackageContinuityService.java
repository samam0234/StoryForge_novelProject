package com.storyforge.service.community;

import com.storyforge.client.AiDtos.CharacterInfo;
import com.storyforge.client.AiDtos.BibleInfo;
import com.storyforge.client.AiDtos.PackageContinuityRequest;
import com.storyforge.client.AiDtos.PackageContinuityResult;
import com.storyforge.client.AiDtos.PackageLinkInfo;
import com.storyforge.client.AiDtos.PackageWorkInfo;
import com.storyforge.client.AiDtos.PreviousChapter;
import com.storyforge.client.AiServiceClient;
import com.storyforge.domain.bible.BibleDtos.BibleSnapshot;
import com.storyforge.domain.community.*;
import com.storyforge.domain.community.CommunityDtos.PackageReportView;
import com.storyforge.exception.InvalidStateException;
import com.storyforge.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 패키지 전체 Continuity 검사: 묶인 작품끼리 인물, 세계 규칙, 연표가 어긋나는지 FastAPI에 맡깁니다.
 * 다른 작가가 설정을 공개하지 않은 작품(Clone 불가)은 보내지 않고 건너뜁니다. AI 호출은 트랜잭션 밖에서 합니다.
 */
@Service
public class PackageContinuityService {

    private final WorkRepository works;
    private final PackageItemRepository items;
    private final PackageLinkRepository links;
    private final WorkChapterRepository chapters;
    private final PackageReportRepository reports;
    private final AiServiceClient ai;
    private final TransactionTemplate tx;

    public PackageContinuityService(WorkRepository works, PackageItemRepository items, PackageLinkRepository links,
                                    WorkChapterRepository chapters, PackageReportRepository reports, AiServiceClient ai,
                                    TransactionTemplate tx) {
        this.works = works;
        this.items = items;
        this.links = links;
        this.chapters = chapters;
        this.reports = reports;
        this.ai = ai;
        this.tx = tx;
    }

    private record Prepared(PackageContinuityRequest request, List<String> skipped) {
    }

    public PackageReportView run(Long packageId, Long userId) {
        Prepared prepared = Objects.requireNonNull(tx.execute(s -> prepare(packageId, userId)));
        PackageContinuityResult result = ai.checkPackage(prepared.request());
        PackageReport saved = Objects.requireNonNull(tx.execute(s -> reports.save(
                new PackageReport(packageId, result.issues(), result.score(), prepared.skipped()))));
        return view(saved);
    }

    /** 최근 결과. 없으면 null입니다. */
    public PackageReportView latest(Long packageId, Long userId) {
        return tx.execute(s -> {
            owned(packageId, userId);
            return reports.findFirstByPackageWorkIdOrderByCreatedAtDescIdDesc(packageId).map(PackageContinuityService::view).orElse(null);
        });
    }

    private Prepared prepare(Long packageId, Long userId) {
        Work pkg = owned(packageId, userId);
        List<PackageItem> approved = items.findWithMembers(packageId).stream()
                .filter(i -> i.getStatus() == PackageItem.Status.APPROVED && !i.getMember().isHidden())
                .toList();
        List<PackageWorkInfo> infos = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        Map<Long, String> titles = approved.stream()
                .collect(Collectors.toMap(i -> i.getId(), i -> i.getMember().getTitle(), (a, b) -> a));
        for (PackageItem item : approved) {
            Work member = item.getMember();
            boolean mine = member.getAuthor().getId().equals(userId);
            if (!mine && member.getClonePolicy() == Work.ClonePolicy.CLOSED) {
                skipped.add(member.getTitle());
                continue;
            }
            infos.add(new PackageWorkInfo(member.getTitle(), item.getRole().name(), bibleInfo(member.getBibleSnapshot()),
                    chapters.findByWorkIdOrderByNumber(member.getId()).stream()
                            .map(c -> new PreviousChapter(c.getNumber(), c.getTitle(), c.getSummary()))
                            .toList()));
        }
        if (infos.size() < 2) {
            throw new InvalidStateException("검사하려면 설정을 볼 수 있는 작품이 두 개 이상 있어야 합니다.");
        }
        Function<Long, String> title = id -> titles.getOrDefault(id, "");
        List<PackageLinkInfo> linkInfos = links.findByPackageWorkIdOrderById(packageId).stream()
                .filter(l -> titles.containsKey(l.getFromItemId()) && titles.containsKey(l.getToItemId()))
                .map(l -> new PackageLinkInfo(title.apply(l.getFromItemId()), title.apply(l.getToItemId()),
                        l.getLinkType().name(), l.getLabel()))
                .toList();
        return new Prepared(new PackageContinuityRequest(pkg.getTitle(), pkg.getWorldNote(), infos, linkInfos), skipped);
    }

    private Work owned(Long packageId, Long userId) {
        return works.findWithAuthor(Objects.requireNonNull(packageId))
                .filter(w -> w.getKind() == Work.Kind.PACKAGE && w.getAuthor().getId().equals(userId))
                .orElseThrow(() -> new NotFoundException("패키지를 찾을 수 없습니다: " + packageId));
    }

    private static BibleInfo bibleInfo(BibleSnapshot b) {
        if (b == null) {
            return new BibleInfo(null, null, null, List.of());
        }
        return new BibleInfo(b.worldSetting(), b.theme(), b.tone(), b.characters() == null ? List.of() : b.characters().stream()
                .map(c -> new CharacterInfo(c.name(), c.description(), c.personality(), c.relationships(), null))
                .toList());
    }

    private static PackageReportView view(PackageReport r) {
        return new PackageReportView(r.getIssues(), r.getScore(), r.getSkippedTitles(), r.getCreatedAt());
    }
}

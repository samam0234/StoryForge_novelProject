package com.storyforge.domain.community;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PackageItemRepository extends JpaRepository<PackageItem, Long> {

    @Query("select i from PackageItem i join fetch i.member m join fetch m.author where i.packageWorkId = :packageId order by i.position, i.id")
    List<PackageItem> findWithMembers(@Param("packageId") Long packageId);

    Optional<PackageItem> findByIdAndPackageWorkId(Long id, Long packageWorkId);

    boolean existsByPackageWorkIdAndMemberId(Long packageWorkId, Long memberId);

    @Query("select i from PackageItem i join fetch i.member m where m.author.id = :authorId and i.status = com.storyforge.domain.community.PackageItem$Status.PENDING order by i.createdAt")
    List<PackageItem> findPendingFor(@Param("authorId") Long authorId);

    /** 이 작품이 들어 있는 승인된 패키지 ID */
    @Query("select i.packageWorkId from PackageItem i where i.member.id = :workId and i.status = com.storyforge.domain.community.PackageItem$Status.APPROVED")
    List<Long> findPackageIdsOf(@Param("workId") Long workId);
}

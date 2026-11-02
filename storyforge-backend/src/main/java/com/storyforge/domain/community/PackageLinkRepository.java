package com.storyforge.domain.community;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PackageLinkRepository extends JpaRepository<PackageLink, Long> {

    List<PackageLink> findByPackageWorkIdOrderById(Long packageWorkId);

    Optional<PackageLink> findByIdAndPackageWorkId(Long id, Long packageWorkId);
}

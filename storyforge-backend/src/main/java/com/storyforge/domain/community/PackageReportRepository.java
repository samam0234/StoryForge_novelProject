package com.storyforge.domain.community;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PackageReportRepository extends JpaRepository<PackageReport, Long> {

    Optional<PackageReport> findFirstByPackageWorkIdOrderByCreatedAtDescIdDesc(Long packageWorkId);
}

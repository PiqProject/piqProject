package piq.piqproject.domain.reports.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import piq.piqproject.domain.reports.entity.ReportEntity;
import piq.piqproject.domain.reports.enums.ReportStatus;

public interface ReportRepository extends JpaRepository<ReportEntity, Long> {
    // 특정 상태 리스트에 포함되는 신고들 조회(오래된 순)
    // SQL: SELECT * FROM reports WHERE status IN (...) ORDER BY created_at ASC
    List<ReportEntity> findAllByStatusInOrderByCreatedAtAsc(List<ReportStatus> statuses);

    // 특정 상태 리스트에 포함되는 신고들 조회(최신순)
    // SQL: SELECT * FROM reports WHERE status IN (...) ORDER BY created_at DESC
    List<ReportEntity> findAllByStatusInOrderByCreatedAtDesc(List<ReportStatus> statuses);

    // 전체 조회 (최신순)
    List<ReportEntity> findAllByOrderByCreatedAtDesc();
}

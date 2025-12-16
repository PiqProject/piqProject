package piq.piqproject.domain.search.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import piq.piqproject.domain.search.entity.SyncFailLogEntity;

import java.util.List;

public interface SyncFailLogRepository extends JpaRepository<SyncFailLogEntity, Long> {

    // 해결되지 않았고(false), 재시도 횟수가 maxRetry 미만인 로그 조회
    List<SyncFailLogEntity> findAllByIsResolvedFalseAndRetryCountLessThan(int maxRetry);
}
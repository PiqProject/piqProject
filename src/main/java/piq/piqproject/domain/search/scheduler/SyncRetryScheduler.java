package piq.piqproject.domain.search.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import piq.piqproject.domain.search.entity.SyncFailLogEntity;
import piq.piqproject.domain.search.repository.SyncFailLogRepository;
import piq.piqproject.domain.search.service.UserSearchService;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncRetryScheduler {

    private final SyncFailLogRepository syncFailLogRepository;
    private final UserSearchService userSearchService;

    // 5분마다 실행 (설정 파일로 관리 가능)
    @Scheduled(cron = "0 0/5 * * * *")
    @Transactional
    public void retryFailedSyncs() {
        // 1. 해결되지 않았고, 재시도 횟수가 5회 미만인 로그 조회
        List<SyncFailLogEntity> failedLogs = syncFailLogRepository.findAllByIsResolvedFalseAndRetryCountLessThan(5);

        if (failedLogs.isEmpty())
            return;

        log.info("UserEntitu - ElasticSearch 동기화 재시도: {}건", failedLogs.size());

        for (SyncFailLogEntity logEntity : failedLogs) {
            try {
                // 2. 재시도 수행
                userSearchService.syncUserToElasticsearch(logEntity.getUserId());

                // 3. 성공 시 해결 처리 (혹은 delete()로 삭제해도 됨)
                logEntity.resolve();
                log.info("UserEntitu - ElasticSearch 동기화 재시도 성공 - LogId: {}, UserId: {}", logEntity.getId(),
                        logEntity.getUserId());

            } catch (Exception e) {
                // 4. 또 실패 시 재시도 횟수 증가
                logEntity.incrementRetryCount();
                log.warn("UserEntitu - ElasticSearch 동기화 재시도 또 실패 - LogId: {}, 횟수: {}", logEntity.getId(),
                        logEntity.getRetryCount());
            }
        }
    }
}
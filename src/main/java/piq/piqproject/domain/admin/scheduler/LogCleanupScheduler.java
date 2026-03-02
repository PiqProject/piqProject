package piq.piqproject.domain.admin.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import piq.piqproject.domain.admin.log.repository.AdminAccessLogRepository;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogCleanupScheduler {

    private final AdminAccessLogRepository adminAccessLogRepository;

    // 보관 기간 (기본 1년 = 365일)
    // application.properties에서 설정 가능하게 하면 더 좋음 (app.log.retention-days=365)
    @Value("${app.log.retention-days-admin:365}")
    private int retentionDays;

    /**
     * 매일 새벽 5시에 실행되어 보관 기간이 지난 DB 로그를 삭제합니다.
     */
    @Scheduled(cron = "0 0 5 * * *") // 새벽 5시 0분 0초
    @Transactional
    public void cleanupOldDbLogs() {
        // 1. 삭제 기준 시간 계산 (현재 시간 - 365일)
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);

        // 2. 삭제 실행
        try {
            adminAccessLogRepository.deleteLogsOlderThan(cutoffDate);
        } catch (Exception e) {
            log.error("[DB Cleanup] Error occurred while deleting logs", e);
        }
    }
}
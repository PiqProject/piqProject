package piq.piqproject.domain.points.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import piq.piqproject.domain.points.repository.PointHistoryRepository;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointHistoryCleanupScheduler {

    private final PointHistoryRepository pointHistoryRepository;

    // 전자금융거래법상 보관 기간: 5년
    private static final int RETENTION_YEARS = 5;

    /**
     * 매일 새벽 4시에 실행되어 5년이 지난 포인트 이력을 삭제합니다.
     * Cron 표현식: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional // DELETE 쿼리 실행을 위해 필수
    public void cleanupExpiredPointHistories() {
        // 1. 기준 날짜 계산 (현재 - 5년)
        LocalDateTime cutoffDate = LocalDateTime.now().minusYears(RETENTION_YEARS);

        log.info("[Point History Cleanup] 포인트 이력 정리를 시작합니다. 기준일: {} ({}년 전)", cutoffDate, RETENTION_YEARS);

        try {
            // 2. 삭제 실행
            // (앞으로 5년간은 삭제할 데이터가 0건이므로 0.001초 만에 끝납니다 -> 자원 소모 거의 없음)
            pointHistoryRepository.deleteExpiredHistories(cutoffDate);

            log.info("[Point History Cleanup] 정리 완료.");
        } catch (Exception e) {
            log.error("[Point History Cleanup] 포인트 이력 삭제 중 오류 발생", e);
        }
    }
}
package piq.piqproject.domain.matches.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import piq.piqproject.domain.matches.entity.MatchingEntity;
import piq.piqproject.domain.matches.enums.MatchingStatus;
import piq.piqproject.domain.matches.repository.MatchingRepository;
import piq.piqproject.domain.points.enums.PointType;
import piq.piqproject.domain.points.service.PointService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MatchExpiredScheduler {

    private final MatchingRepository matchingRepository;
    private final PointService pointService;

    /**
     * 15분마다 실행 (설정 파일의 matching.expired.scheduler.cron 참고)
     * 24시간이 경과한 PENDING 상태의 매칭을 EXPIRED로 변경하고 포인트를 환불합니다.
     */
    @Scheduled(cron = "${matching.expired.scheduler.cron:0 0/15 * * * *}")
    @Transactional
    public void processExpiredMatches() {
        log.info("[BATCH_JOB_START] Starting automatic expiration and refund for unanswered matches (24h elapsed).");

        // 1. 24시간 전 시점 계산
        LocalDateTime expiredThreshold = LocalDateTime.now().minusDays(1);

        // 2. PENDING 상태이면서 24시간 이상 경과한 매칭 조회
        List<MatchingEntity> expiredMatches = matchingRepository.findByStatusAndCreatedAtBefore(
                MatchingStatus.PENDING, expiredThreshold);

        if (expiredMatches.isEmpty()) {
            log.info("[BATCH_JOB_END] No matches eligible for expiration.");
            return;
        }

        log.info("[BATCH_JOB_PROCESSING] Processing a total of {} matches for expiration.", expiredMatches.size());

        int successCount = 0;
        for (MatchingEntity match : expiredMatches) {
            try {
                // 3. 상태를 EXPIRED로 변경
                match.changeStatus(MatchingStatus.EXPIRED);

                // 4. Sender에게 포인트 환불
                if (match.getSenderUsedPoints() > 0) {
                    pointService.chargePoints(
                            match.getSender(),
                            match.getSenderUsedPoints(),
                            PointType.REFUND,
                            "미응답 매칭 자동 만료 포인트 환불");
                }
                successCount++;
            } catch (Exception e) {
                log.error("[BATCH_JOB_ERROR] Error occurred while processing match ID: {}", match.getMatchId(), e);
            }
        }

        log.info("[BATCH_JOB_END] Successfully marked {} matches as EXPIRED and completed refunds.", successCount);
    }
}

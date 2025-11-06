package piq.piqproject.domain.payments.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import piq.piqproject.domain.payments.service.PaymentCleanupService;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCleanupScheduler {

    private final PaymentCleanupService paymentCleanupService;

    /**
     * 매일 새벽 4시에 실행하여 만료된 결제를 정리합니다.
     * Cron 표현식: "초 분 시 일 월 요일"
     * "0 0 4 * * *" -> 매일 4시 0분 0초
     */
    @Scheduled(cron = "0 30 4 * * *")
    public void runDailyCleanup() {
        log.info("[BATCH_JOB_START] 24시간 이상된 READY 상태의 결제 정리 작업을 시작합니다.");

        try {
            int updatedCount = paymentCleanupService.cleanupOldReadyPayments();
            log.info("[BATCH_JOB_END] 총 {}건의 결제 정보를 EXPIRED 상태로 변경했습니다.", updatedCount);
        } catch (Exception e) {
            log.error("[BATCH_JOB_FAILED] 결제 정리 작업 중 오류가 발생했습니다.", e);
        }
    }
}
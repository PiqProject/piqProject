package piq.piqproject.domain.payments.common.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCleanupScheduler {

    private final PaymentCleanupUtil paymentCleanupService;

    /**
     * 매일 새벽 4시에 실행하여 만료된 결제를 정리합니다.
     * Cron 표현식: "초 분 시 일 월 요일"
     * "0 0 4 * * *" -> 매일 4시 0분 0초
     */
    @Scheduled(cron = "0 30 4 * * *")
    public void runDailyCleanup() {
        log.info("[BATCH_JOB_START] Starting cleanup for READY payments older than 24 hours.");

        try {
            int updatedCount = paymentCleanupService.cleanupOldReadyPayments();
            log.info("[BATCH_JOB_END] Total of {} payment records changed to EXPIRED status.", updatedCount);
        } catch (Exception e) {
            log.error("[BATCH_JOB_FAILED] Error occurred during payment cleanup job.", e);
        }
    }
}
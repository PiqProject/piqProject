package piq.piqproject.domain.notifications.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import piq.piqproject.domain.notifications.repository.NotificationRepository;

import java.time.LocalDateTime;

@Service
public class NotificationCleanupService {

    private final NotificationRepository notificationRepository;

    public NotificationCleanupService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // 매일 새벽 3시 0분 0초에 실행
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteOldNotifications() {
        // 생성된 지 14일이 지난 알림만 일괄 삭제
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(14);
        int deletedCount = notificationRepository.deleteByCreatedAtBefore(thresholdDate);

        System.out.println("오래된 알림 데이터 " + deletedCount + "건이 삭제되었습니다.");
    }
}
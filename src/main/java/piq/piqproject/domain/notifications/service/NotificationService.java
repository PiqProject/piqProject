package piq.piqproject.domain.notifications.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.extern.slf4j.Slf4j;
import piq.piqproject.domain.alarms.entity.DeviceTokenEntity;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.domain.alarms.repository.DeviceTokenRepository;
import piq.piqproject.domain.notifications.dto.NotificationResponseDto;
import piq.piqproject.domain.notifications.entity.NotificationEntity;
import piq.piqproject.domain.notifications.enums.NotificationType;
import piq.piqproject.domain.notifications.repository.NotificationRepository;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.infra.external.fcm.service.FcmService;

@Slf4j
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final FcmService fcmService;
    private final Executor taskExecutor;

    public NotificationService(
            NotificationRepository notificationRepository,
            DeviceTokenRepository deviceTokenRepository,
            FcmService fcmService,
            @Qualifier("taskExecutor") Executor taskExecutor) {
        this.notificationRepository = notificationRepository;
        this.deviceTokenRepository = deviceTokenRepository;
        this.fcmService = fcmService;
        this.taskExecutor = taskExecutor;
    }

    // ============================================================
    // 1. 알림 생성 + FCM 전송 (핵심 메서드)
    // ============================================================

    /**
     * 개인 알림을 DB에 저장하고, FCM 푸시도 함께 전송합니다.
     *
     * @param user      알림 대상 사용자
     * @param type      알림 유형
     * @param title     알림 제목
     * @param body      알림 본문
     * @param targetUrl 클릭 시 이동 경로 (nullable)
     */
    @Transactional
    public void notify(UserEntity user, NotificationType type,
            String title, String body, String targetUrl) {

        // 1. DB 저장
        NotificationEntity notification = NotificationEntity.of(user, type, title, body, targetUrl);
        notificationRepository.save(notification);

        // 2. FCM 전송 (실패해도 알림 저장은 유지되도록 헬퍼 메서드에서 try-catch 처리)
        sendPushToUser(user, title, body, targetUrl);
    }

    /**
     * 전체 공지 알림을 DB에 저장합니다.
     * 전체 사용자에게 FCM 푸시는 별도로 처리할 수 있습니다.
     */
    @Transactional
    public void notifyGlobal(NotificationType type, String title, String body, String targetUrl) {
        // 1. DB 저장 (본문 user = null로 저장되어 모든 사용자가 조회 가능)
        NotificationEntity notification = NotificationEntity.ofGlobal(type, title, body, targetUrl);
        notificationRepository.save(notification);

        // 트랜잭션 내에서 토큰 목록을 미리 조회
        List<DeviceTokenEntity> tokens = deviceTokenRepository.findAll();

        if (tokens.isEmpty()) {
            return;
        }

        // 트랜잭션 커밋 후 비동기로 FCM 전송 (응답 지연 방지)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                taskExecutor.execute(() -> {
                    log.info("[FCM-GLOBAL] 전체 알림 비동기 전송 시작. tokens={}", tokens.size());
                    for (DeviceTokenEntity token : tokens) {
                        try {
                            String deviceType = token.getDeviceType();
                            if ("WEB".equalsIgnoreCase(deviceType)) {
                                fcmService.sendWebMessageToWeb(token.getToken(), title, body,
                                        targetUrl != null ? targetUrl : "/");
                            } else {
                                fcmService.sendMessageToApp(token.getToken(), title, body,
                                        Map.of("targetUrl", targetUrl != null ? targetUrl : "/"));
                            }
                        } catch (Exception e) {
                            log.warn("FCM global push failed. token={}, error={}",
                                    token.getToken(), e.getMessage());
                        }
                    }
                    log.info("[FCM-GLOBAL] 전체 알림 비동기 전송 완료.");
                });
            }
        });
    }

    // ============================================================
    // 2. 조회 API
    // ============================================================

    /**
     * 특정 사용자의 알림 목록 조회 (개인 알림 + 전체 공지 포함)
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getNotifications(Long userId, Pageable pageable) {
        Page<NotificationEntity> notifications = notificationRepository
                .findAllByUserIdOrGlobal(userId, pageable);

        return notifications.map(NotificationResponseDto::from);
    }

    /**
     * 읽지 않은 알림 개수 (개인 알림만)
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    // ============================================================
    // 3. 상태 변경 API
    // ============================================================

    /**
     * 단건 읽음 처리
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "알림을 찾을 수 없습니다."));

        // 본인의 알림인지 검증 (전체 공지는 user=null)
        if (notification.getUser() != null && !notification.getUser().getId().equals(userId)) {
            throw new NotFoundException(ErrorCode.NOT_FOUND, "해당 알림에 접근할 수 없습니다.");
        }

        notification.markAsRead();
    }

    /**
     * 전체 읽음 처리
     */
    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsReadByUserId(userId);
    }

    // ============================================================
    // 내부 헬퍼 메서드
    // ============================================================

    private void sendPushToUser(UserEntity user, String title, String body, String targetUrl) {
        // 트랜잭션 내에서 토큰 목록을 미리 조회 (DB 쿼리는 빠름)
        List<DeviceTokenEntity> tokens = deviceTokenRepository.findByUserId(user.getId());

        if (tokens.isEmpty()) {
            return;
        }

        // 트랜잭션 커밋 후 비동기로 FCM 전송 (응답 지연 방지)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                taskExecutor.execute(() -> {
                    log.info("[FCM-USER] 개인 알림 비동기 전송 시작. userId={}, tokens={}", user.getId(), tokens.size());
                    for (DeviceTokenEntity token : tokens) {
                        try {
                            String deviceType = token.getDeviceType();
                            if ("WEB".equalsIgnoreCase(deviceType)) {
                                fcmService.sendWebMessageToWeb(token.getToken(), title, body,
                                        targetUrl != null ? targetUrl : "/");
                            } else {
                                fcmService.sendMessageToApp(token.getToken(), title, body,
                                        Map.of("targetUrl", targetUrl != null ? targetUrl : "/"));
                            }
                        } catch (Exception e) {
                            log.warn("FCM push delivery failed. userId={}, token={}, error={}",
                                    user.getId(), token.getToken(), e.getMessage());
                        }
                    }
                    log.info("[FCM-USER] fcm push delivery completed. userId={}", user.getId());
                });
            }
        });
    }
}

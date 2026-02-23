package piq.piqproject.domain.notifications.service;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final FcmService fcmService;

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

        deviceTokenRepository.findByUserId(user.getId())
                .ifPresent(token -> {
                    String deviceType = token.getDeviceType();
                    if ("WEB".equalsIgnoreCase(deviceType)) {
                        fcmService.sendWebMessageToWeb(token.getToken(), title, body,
                                targetUrl != null ? targetUrl : "/");
                    } else {
                        fcmService.sendMessageToApp(token.getToken(), title, body,
                                Map.of("targetUrl", targetUrl != null ? targetUrl : "/"));
                    }
                });
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

        deviceTokenRepository.findAll()
                .forEach(token -> {
                    String deviceType = token.getDeviceType();
                    if ("WEB".equalsIgnoreCase(deviceType)) {
                        fcmService.sendWebMessageToWeb(token.getToken(), title, body,
                                targetUrl != null ? targetUrl : "/");
                    } else {
                        fcmService.sendMessageToApp(token.getToken(), title, body,
                                Map.of("targetUrl", targetUrl != null ? targetUrl : "/"));
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
        try {
            deviceTokenRepository.findByUserId(user.getId())
                    .ifPresent(token -> {
                        String deviceType = token.getDeviceType();
                        if ("WEB".equalsIgnoreCase(deviceType)) {
                            fcmService.sendWebMessageToWeb(token.getToken(), title, body,
                                    targetUrl != null ? targetUrl : "/");
                        } else {
                            fcmService.sendMessageToApp(token.getToken(), title, body,
                                    Map.of("targetUrl", targetUrl != null ? targetUrl : "/"));
                        }
                    });
        } catch (Exception e) {
            // FCM 실패는 알림 저장에 영향을 주지 않도록 로그만 남김
            log.warn("FCM 푸시 전송 실패. userId={}, error={}", user.getId(), e.getMessage());
        }
    }
}

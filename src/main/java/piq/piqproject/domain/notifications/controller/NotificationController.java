package piq.piqproject.domain.notifications.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import piq.piqproject.domain.notifications.dto.NotificationResponseDto;
import piq.piqproject.domain.notifications.service.NotificationService;
import piq.piqproject.domain.users.entity.UserEntity;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 알림 목록 조회 (개인 알림 + 전체 공지 포함, 최신순)
     */
    @GetMapping
    public ResponseEntity<Page<NotificationResponseDto>> getNotifications(
            @AuthenticationPrincipal UserEntity user,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<NotificationResponseDto> notifications = notificationService
                .getNotifications(user.getId(), pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 읽지 않은 알림 개수
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal UserEntity user) {
        long count = notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(count);
    }

    /**
     * 단건 읽음 처리
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<String> markAsRead(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long notificationId) {

        notificationService.markAsRead(notificationId, user.getId());
        return ResponseEntity.ok("알림을 읽음 처리했습니다.");
    }

    /**
     * 전체 읽음 처리
     */
    @PutMapping("/read-all")
    public ResponseEntity<String> markAllAsRead(@AuthenticationPrincipal UserEntity user) {
        int updatedCount = notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(updatedCount + "개의 알림을 읽음 처리했습니다.");
    }
}

package piq.piqproject.domain.notifications.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import piq.piqproject.domain.BaseEntity;
import piq.piqproject.domain.notifications.enums.NotificationType;
import piq.piqproject.domain.users.entity.UserEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notifications")
public class NotificationEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 알림을 받는 사용자.
     * 전체 공지(ANNOUNCEMENT, EVENT)의 경우 null → 모든 사용자 대상.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 500)
    private String body;

    /**
     * 알림 클릭 시 이동할 경로 (프론트 라우팅 경로).
     * 예: "/matches/123", "/profile/images"
     */
    private String targetUrl;

    /**
     * 읽음 여부. 기본값 false.
     */
    @Column(nullable = false)
    private Boolean isRead;

    @Builder
    private NotificationEntity(UserEntity user, NotificationType type,
            String title, String body, String targetUrl) {
        this.user = user;
        this.type = type;
        this.title = title;
        this.body = body;
        this.targetUrl = targetUrl;
        this.isRead = false;
    }

    // === 비즈니스 메서드 ===

    public void markAsRead() {
        this.isRead = true;
    }

    /**
     * 개인 알림 생성 (특정 사용자 대상)
     */
    public static NotificationEntity of(UserEntity user, NotificationType type,
            String title, String body, String targetUrl) {
        return NotificationEntity.builder()
                .user(user)
                .type(type)
                .title(title)
                .body(body)
                .targetUrl(targetUrl)
                .build();
    }

    /**
     * 전체 공지 알림 생성 (user = null)
     */
    public static NotificationEntity ofGlobal(NotificationType type,
            String title, String body, String targetUrl) {
        return NotificationEntity.builder()
                .user(null)
                .type(type)
                .title(title)
                .body(body)
                .targetUrl(targetUrl)
                .build();
    }
}

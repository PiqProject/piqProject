package piq.piqproject.domain.notifications.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import piq.piqproject.domain.notifications.entity.NotificationEntity;
import piq.piqproject.domain.notifications.enums.NotificationType;

@Getter
@Builder
public class NotificationResponseDto {

    private Long id;
    private NotificationType type;
    private String title;
    private String body;
    private String targetUrl;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationResponseDto from(NotificationEntity entity) {
        return NotificationResponseDto.builder()
                .id(entity.getId())
                .type(entity.getType())
                .title(entity.getTitle())
                .body(entity.getBody())
                .targetUrl(entity.getTargetUrl())
                .isRead(entity.getIsRead())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

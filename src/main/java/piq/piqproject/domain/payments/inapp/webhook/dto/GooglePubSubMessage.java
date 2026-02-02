package piq.piqproject.domain.payments.inapp.webhook.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Google Cloud Pub/Sub Push 메시지 DTO
 * Google Play Real-time Developer Notifications (RTDN)
 */
@Getter
@NoArgsConstructor
public class GooglePubSubMessage {
    private Message message;
    private String subscription;

    @Getter
    @NoArgsConstructor
    public static class Message {
        private String messageId;
        private String publishTime;
        private String data; // Base64 인코딩된 JSON 데이터
    }
}

package piq.piqproject.domain.payments.inapp.webhook.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import piq.piqproject.domain.payments.inapp.webhook.dto.AppleNotificationRequest;
import piq.piqproject.domain.payments.inapp.webhook.dto.GooglePubSubMessage;
import piq.piqproject.domain.payments.inapp.webhook.service.AppleWebhookService;
import piq.piqproject.domain.payments.inapp.webhook.service.GoogleWebhookService;

/**
 * 외부 결제 플랫폼 웹훅 수신 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final GoogleWebhookService googleWebhookService;
    private final AppleWebhookService appleWebhookService;

    /**
     * Google Cloud Pub/Sub Push 엔드포인트
     * Google Play Real-time Developer Notifications (RTDN)
     */
    @PostMapping("/google")
    public ResponseEntity<Void> handleGoogleNotification(@RequestBody GooglePubSubMessage message) {
        log.info("Google 웹훅 수신: messageId={}", message.getMessage().getMessageId());

        try {
            googleWebhookService.processNotification(message);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Google 웹훅 처리 실패", e);
            // Pub/Sub는 200을 받지 못하면 재시도하므로, 일시적 오류는 500 반환
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Apple App Store Server Notifications V2 엔드포인트
     */
    @PostMapping("/apple")
    public ResponseEntity<Void> handleAppleNotification(@RequestBody AppleNotificationRequest request) {
        log.info("Apple 웹훅 수신");

        try {
            appleWebhookService.processNotification(request.getSignedPayload());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Apple 웹훅 처리 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

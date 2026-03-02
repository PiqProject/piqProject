package piq.piqproject.domain.payments.inapp.webhook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import piq.piqproject.domain.payments.common.enums.PaymentType;
import piq.piqproject.domain.payments.inapp.service.RefundService;
import piq.piqproject.domain.payments.inapp.webhook.dto.GoogleDeveloperNotification;
import piq.piqproject.domain.payments.inapp.webhook.dto.GooglePubSubMessage;
import piq.piqproject.infra.external.google.service.GooglePlayClientService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Google Real-time Developer Notifications (RTDN) 처리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleWebhookService {

    private final GooglePlayClientService googlePlayClientService;
    private final RefundService refundService;
    private final ObjectMapper objectMapper;

    // 알림 타입 상수
    private static final int NOTIFICATION_TYPE_CANCELED = 2; // 구매 취소 (환불)

    /**
     * Google Pub/Sub 메시지 처리
     */
    public void processNotification(GooglePubSubMessage pubSubMessage) {
        try {
            // 1. Base64 디코딩
            String decodedData = new String(
                    Base64.getDecoder().decode(pubSubMessage.getMessage().getData()),
                    StandardCharsets.UTF_8);
            log.info("Google RTDN received: {}", decodedData);

            // 2. JSON 파싱
            GoogleDeveloperNotification notification = objectMapper.readValue(
                    decodedData, GoogleDeveloperNotification.class);

            // 3. 일회성 구매 알림인 경우에만 처리
            if (notification.getOneTimePurchaseNotification() == null) {
                log.info("Not a one-time purchase notification, ignoring");
                return;
            }

            GoogleDeveloperNotification.OneTimePurchaseNotification purchaseNotification = notification
                    .getOneTimePurchaseNotification();

            int notificationType = purchaseNotification.getNotificationType();
            log.info("Google notification type: {}", notificationType);

            // 4. 환불(취소) 알림인 경우 처리
            if (notificationType == NOTIFICATION_TYPE_CANCELED) {
                handleRefund(purchaseNotification);
            }

        } catch (Exception e) {
            log.error("Error processing Google RTDN", e);
            throw new RuntimeException("Google 웹훅 처리 실패", e);
        }
    }

    /**
     * 환불 처리
     */
    private void handleRefund(GoogleDeveloperNotification.OneTimePurchaseNotification notification) {
        String purchaseToken = notification.getPurchaseToken();
        String sku = notification.getSku();

        log.info("Starting Google refund process: sku={}", sku);

        try {
            // Google API로 상세 정보 조회하여 orderId 획득
            var purchase = googlePlayClientService.getProductPurchase(sku, purchaseToken);
            String orderId = purchase.getOrderId();

            log.info("Google refund - orderId: {}", orderId);

            // RefundService로 환불 처리 위임
            refundService.processRefund(orderId, PaymentType.GOOGLE);

        } catch (Exception e) {
            log.error("Google refund process failed: sku={}", sku, e);
            throw e;
        }
    }
}

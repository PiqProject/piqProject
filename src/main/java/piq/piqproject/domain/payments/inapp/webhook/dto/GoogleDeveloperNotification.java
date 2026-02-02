package piq.piqproject.domain.payments.inapp.webhook.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Google RTDN 알림 데이터 (Base64 디코딩 후)
 */
@Getter
@NoArgsConstructor
public class GoogleDeveloperNotification {
    private String version;
    private String packageName;
    private long eventTimeMillis;
    private OneTimePurchaseNotification oneTimePurchaseNotification;

    @Getter
    @NoArgsConstructor
    public static class OneTimePurchaseNotification {
        private String version;
        /**
         * 알림 타입:
         * 1: ONE_TIME_PRODUCT_PURCHASED - 구매 완료
         * 2: ONE_TIME_PRODUCT_CANCELED - 구매 취소 (환불)
         * 3: ONE_TIME_PRODUCT_PENDING - 구매 보류
         */
        private int notificationType;
        private String purchaseToken;
        private String sku;
    }
}

package piq.piqproject.domain.payments.inapp.webhook.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Apple App Store Server Notifications V2 요청 DTO
 */
@Getter
@NoArgsConstructor
public class AppleNotificationRequest {
    private String signedPayload; // JWS 형식의 서명된 페이로드
}

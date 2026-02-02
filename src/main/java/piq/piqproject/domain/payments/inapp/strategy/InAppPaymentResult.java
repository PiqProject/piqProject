package piq.piqproject.domain.payments.inapp.strategy;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InAppPaymentResult {
    private final boolean success;
    private final String transactionId; // Google orderId 또는 Apple original_transaction_id
    private final String merchantUid; // 내부 주문번호
    private final int pointsGranted; // 지급된 포인트
    private final String message; // 결과 메시지
}

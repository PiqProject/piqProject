package piq.piqproject.domain.payments.dto.Response;

import lombok.Getter;
import piq.piqproject.domain.payments.entity.PaymentEntity;
import piq.piqproject.domain.payments.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class PaymentHistoryResponseDto {

    private final String merchantUid;
    private final BigDecimal amount;
    private final PaymentStatus status;
    private final LocalDateTime paidAt; // 결제일시 또는 생성일시

    // PaymentEntity를 DTO로 변환하는 생성자
    public PaymentHistoryResponseDto(PaymentEntity payment) {
        this.merchantUid = payment.getMerchantUid();
        this.amount = payment.getAmount();
        this.status = payment.getStatus();
        this.paidAt = payment.getCreatedAt(); // 예시로 생성일시를 사용
    }
}
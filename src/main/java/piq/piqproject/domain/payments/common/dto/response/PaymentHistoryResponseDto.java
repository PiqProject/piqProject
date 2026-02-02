package piq.piqproject.domain.payments.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import piq.piqproject.domain.payments.common.entity.PaymentEntity;
import piq.piqproject.domain.payments.common.enums.PaymentStatus;
import piq.piqproject.domain.payments.common.enums.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class PaymentHistoryResponseDto {
    private Long id;
    private String merchantUid;
    private String transactionId;
    private PaymentType type;
    private long productId;
    private BigDecimal amount;
    private PaymentStatus status;
    private LocalDateTime paidAt; // 결제일시 또는 생성일시

    // PaymentEntity를 DTO로 변환하는 생성자
    public static PaymentHistoryResponseDto from(PaymentEntity entity) {
        return PaymentHistoryResponseDto.builder()
                .id(entity.getId())
                .merchantUid(entity.getMerchantUid())
                .transactionId(entity.getTransactionId())
                .type(entity.getType()) // 결제 수단 정보
                .productId(entity.getProduct().getId())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .paidAt(entity.getCreatedAt())
                .build();
    }
}
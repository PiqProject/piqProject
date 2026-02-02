package piq.piqproject.domain.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import piq.piqproject.domain.payments.common.entity.PaymentEntity;
import piq.piqproject.domain.payments.common.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class AdminPaymentHistoryResponseDto {
    private Long id;
    private String merchantUid; // 우리 서버 주문번호
    private String impUid; // 포트원 거래번호 (CS 조회용)
    private Long productId;
    private BigDecimal amount;
    private PaymentStatus status; // PAID, READY, CANCELLED, FAILED
    private LocalDateTime createdAt;

    public static AdminPaymentHistoryResponseDto from(PaymentEntity entity) {
        return AdminPaymentHistoryResponseDto.builder()
                .id(entity.getId())
                .merchantUid(entity.getMerchantUid())
                .impUid(entity.getTransactionId())
                .productId(entity.getProduct().getId())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
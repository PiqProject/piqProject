package piq.piqproject.domain.payments.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
    READY("준비"), // 결제 준비 상태 (사전 검증 완료)
    PAID("완료"), // 결제 완료 상태 (사후 검증 완료)
    FAILED("실패"), // 결제 실패 상태
    CANCELLED("취소"), // 결제 취소 상태
    EXPIRED("만료"), // 결제 만료 상태
    REFUNDED("환불"); // 환불 처리 상태 (스토어에서 환불 완료)

    private final String description;
}
package piq.piqproject.domain.payments.inapp.strategy;

import piq.piqproject.domain.payments.common.enums.PaymentType;
import piq.piqproject.domain.users.entity.UserEntity;

public interface InAppPaymentStrategy {
    /**
     * 인앱 결개를 검증하고 처리합니다.
     * 
     * @param request 플랫폼별 결제 요청 정보 (GooglePaymentRequestDto 또는
     *                ApplePaymentRequestDto)
     * @param user    결제를 요청한 사용자
     * @return 처리 결과
     */
    InAppPaymentResult verifyAndProcessPayment(Object request, UserEntity user);

    /**
     * 이 전략이 처리할 수 있는 결제 타입을 반환합니다.
     */
    PaymentType getPaymentType();
}

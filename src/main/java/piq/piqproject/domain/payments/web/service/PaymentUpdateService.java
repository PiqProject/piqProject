package piq.piqproject.domain.payments.web.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import piq.piqproject.domain.payments.common.entity.PaymentEntity;
import piq.piqproject.domain.payments.common.enums.PaymentStatus;
import piq.piqproject.domain.payments.common.repository.PaymentRepository;
import piq.piqproject.domain.points.enums.PointType;
import piq.piqproject.domain.points.service.PointService;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.InternalServerException;

@Service
@RequiredArgsConstructor
public class PaymentUpdateService {
    private final PaymentRepository paymentRepository;
    private final PointService pointService;

    // 실제 DB 업데이트만 짧고 굵게 처리
    @Transactional
    public void updateSuccess(String merchantUid, String paymentId, BigDecimal actualAmount)
            throws InternalServerException {
        PaymentEntity payment = paymentRepository.findByMerchantUidWithLock(merchantUid)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "결제 정보 없음"));

        // 상태 검증 (락 획득 후 멱등성 보장: 이미 결제 완료된 건이면 중복 처리 방지)
        if (payment.getStatus() != PaymentStatus.READY) {
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "이미 처리되었거나 유효하지 않은 결제 상태입니다.");
        }

        // 금액 검증 로직을 여기서 수행 (트랜잭션 안에서 최종 확인)
        if (payment.getAmount().compareTo(actualAmount) != 0) {
            throw new InternalServerException(ErrorCode.INVALID_PAYMENT_AMOUNT, "금액 불일치");
        }

        payment.completePayment(paymentId);
        pointService.chargePoints(payment.getUser(), payment.getProduct().getPoint(), PointType.CHARGE, "포인트 충전");
    }

    @Transactional
    public void updateFailure(String merchantUid) {
        PaymentEntity payment = paymentRepository.findByMerchantUidWithLock(merchantUid)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "결제 정보     없음"));
        payment.failPayment();
    }
}

package piq.piqproject.domain.payments.inapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.domain.payments.common.entity.PaymentEntity;
import piq.piqproject.domain.payments.common.enums.PaymentStatus;
import piq.piqproject.domain.payments.common.enums.PaymentType;
import piq.piqproject.domain.payments.common.repository.PaymentRepository;
import piq.piqproject.domain.points.service.PointService;
import piq.piqproject.domain.users.entity.UserEntity;

/**
 * 환불 처리 서비스
 * 스토어에서 환불 알림을 받으면 포인트를 회수하고 결제 상태를 변경합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final PaymentRepository paymentRepository;
    private final PointService pointService;

    /**
     * 환불 처리
     * 1. transactionId로 결제 레코드 조회
     * 2. 이미 환불 처리된 건인지 확인 (멱등성)
     * 3. 포인트 회수 (부족하면 0으로 고정)
     * 4. 결제 상태를 REFUNDED로 변경
     */
    @Transactional
    public void processRefund(String transactionId, PaymentType type) {
        log.info("환불 처리 시작: transactionId={}, type={}", transactionId, type);

        // 1. 결제 레코드 조회
        PaymentEntity payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> {
                    log.warn("환불 처리 실패 - 결제 레코드 없음: transactionId={}", transactionId);
                    return new NotFoundException(ErrorCode.NOT_FOUND, "결제 정보를 찾을 수 없습니다: " + transactionId);
                });

        // 2. 이미 환불 처리된 건인지 확인 (멱등성 보장)
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            log.info("이미 환불 처리된 건입니다: transactionId={}", transactionId);
            return;
        }

        // 결제 완료 상태가 아니면 환불 처리 불가
        if (payment.getStatus() != PaymentStatus.PAID) {
            log.warn("환불 처리 불가 - 결제 상태가 PAID가 아님: status={}", payment.getStatus());
            return;
        }

        UserEntity user = payment.getUser();
        int pointToRevoke = payment.getProduct().getPoint();

        // 3. 포인트 강제 회수 처리 (음수 잔액 허용)
        pointService.forceRevokePoints(user, pointToRevoke, "인앱 결제 환불 포인트 회수 (거래ID: " + transactionId + ")");
        log.info("포인트 강제 회수 완료: userId={}, amount={}", user.getId(), pointToRevoke);

        // 4. 결제 상태를 REFUNDED로 변경
        payment.refundPayment();

        log.info("환불 처리 완료: transactionId={}, userId={}", transactionId, user.getId());
    }
}

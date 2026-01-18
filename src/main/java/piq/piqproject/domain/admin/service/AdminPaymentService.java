package piq.piqproject.domain.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.InternalServerException;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.infra.external.portone.service.PortOneClientService;
import piq.piqproject.domain.admin.dto.request.AdminRefundRequestDto;
import piq.piqproject.domain.payments.entity.PaymentEntity;
import piq.piqproject.domain.payments.enums.PaymentStatus;
import piq.piqproject.domain.payments.repository.PaymentRepository;
import piq.piqproject.domain.points.service.PointService;
import piq.piqproject.domain.users.entity.UserEntity;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPaymentService {

    private final PaymentRepository paymentRepository;
    private final PointService pointService;
    private final PortOneClientService portOneClientService;

    /**
     * 관리자 강제 환불 (결제 취소 + 포인트 안전 회수)
     * - 7일 기간 제한 무시
     * - 포인트 잔액 부족 시 0원까지만 회수 (에러 발생 안 함)
     */
    @Transactional
    public void refundPaymentByMerchantUid(AdminRefundRequestDto request) {
        String merchantUid = request.getMerchantUid();
        String cancelReason = request.getCancelReason();

        // 1. 결제 정보 조회 (merchant_uid 기준)
        PaymentEntity payment = paymentRepository.findByMerchantUid(merchantUid)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "해당 주문번호의 결제 정보를 찾을 수 없습니다."));

        // 2. 상태 검증
        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "이미 취소 처리된 결제 건입니다.");
        }
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "결제 완료 상태가 아닌 건은 환불할 수 없습니다.");
        }

        UserEntity user = payment.getUser();
        int targetPoints = payment.getProduct().getPoint();

        // 3. 포인트 안전 회수 (사용자 잔액이 부족하면 0원까지만 회수)
        pointService.revokePointsSafely(
                user,
                targetPoints,
                "관리자 강제 환불 (주문번호: " + merchantUid + ", 사유: " + cancelReason + ")");

        log.info("[Admin Refund] User: {}, Target Point to Revoke: {}, Current Point: {}",
                user.getId(), targetPoints, user.getPqPoint());

        // 4. PortOne API 호출 (PG사 결제 전액 취소)
        portOneClientService.cancelPayment(payment.getImpUid(), cancelReason, payment.getAmount());

        // 5. DB 상태 변경 (PAID -> CANCELLED)
        payment.cancelPayment();
    }
}
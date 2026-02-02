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
import piq.piqproject.domain.admin.dto.response.AdminPaymentHistoryResponseDto;
import piq.piqproject.domain.payments.common.entity.PaymentEntity;
import piq.piqproject.domain.payments.common.enums.PaymentStatus;
import piq.piqproject.domain.payments.common.repository.PaymentRepository;
import piq.piqproject.domain.points.service.PointService;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPaymentService {

    private final PaymentRepository paymentRepository;
    private final PointService pointService;
    private final PortOneClientService portOneClientService;
    private final UserRepository userRepository;

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
        portOneClientService.cancelPayment(payment.getTransactionId(), cancelReason, payment.getAmount());

        // 5. DB 상태 변경 (PAID -> CANCELLED)
        payment.cancelPayment();
    }

    /**
     * 특정 유저의 결제 이력 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public Page<AdminPaymentHistoryResponseDto> getUserPaymentHistory(Long userId, Pageable pageable) {
        // 1. 유저 존재 확인
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER, "해당 유저를 찾을 수 없습니다."));

        // 2. 결제 내역 조회 (기존 Repository 메서드 재사용)
        Page<PaymentEntity> paymentPage = paymentRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        // 3. Admin용 DTO로 변환
        return paymentPage.map(AdminPaymentHistoryResponseDto::from);
    }

    /**
     * 주문번호(merchantUid)로 결제 상세 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public AdminPaymentHistoryResponseDto getPaymentByMerchantUid(String merchantUid) {
        PaymentEntity payment = paymentRepository.findByMerchantUid(merchantUid)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "해당 주문번호의 결제 정보를 찾을 수 없습니다."));

        return AdminPaymentHistoryResponseDto.from(payment);
    }
}
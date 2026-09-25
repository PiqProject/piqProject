package piq.piqproject.domain.payments.web.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.InternalServerException;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.domain.payments.common.dto.response.PaymentHistoryResponseDto;
import piq.piqproject.domain.payments.common.entity.PaymentEntity;
import piq.piqproject.domain.payments.common.enums.PaymentType;
import piq.piqproject.domain.payments.common.repository.PaymentRepository;
import piq.piqproject.domain.payments.web.dto.request.WebPaymentCancelRequestDto;
import piq.piqproject.domain.payments.web.dto.request.WebPaymentPrepareRequestDto;
import piq.piqproject.domain.points.service.PointService;
import piq.piqproject.domain.products.entity.ProductEntity;
import piq.piqproject.domain.products.repository.ProductRepository;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.payments.common.enums.PaymentStatus;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebPaymentService {
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final PointService pointService;

    @Value("${payment.refund.limit-days:7}")
    private int refundLimitDays;

    @Transactional
    public String preparePayment(UserEntity user, WebPaymentPrepareRequestDto request) {
        log.info("Starting payment pre-registration: user={}, amount={}", user.getId(), request.getAmount());

        ProductEntity product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_PRODUCT));

        if (request.getAmount() != product.getPrice()) {
            throw new InternalServerException(ErrorCode.INVALID_PAYMENT_AMOUNT, "결제 금액이 유효하지 않습니다.");
        }

        String merchantUid = String.valueOf(UUID.randomUUID());

        PaymentEntity payment = PaymentEntity.builder()
                .merchantUid(merchantUid)
                .amount(BigDecimal.valueOf(request.getAmount()))
                .status(piq.piqproject.domain.payments.common.enums.PaymentStatus.READY)
                .type(PaymentType.PORTONE)
                .user(user)
                .product(product)
                .build();
        paymentRepository.save(payment);

        return merchantUid;
    }

    @Transactional(readOnly = true)
    public String validateCancellation(WebPaymentCancelRequestDto request, UserEntity user, int refundLimitDays) {
        PaymentEntity payment = paymentRepository.findByMerchantUid(request.getMerchantUid())
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));

        if (!payment.getUser().getId().equals(user.getId())) {
            throw new InternalServerException(ErrorCode.NOT_OWNER, "권한 없음");
        }
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "취소 불가능 상태");
        }

        payment.validateRefundableDate(refundLimitDays);

        if (user.getPqPoint() < payment.getProduct().getPoint()) {
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "이미 포인트를 사용하여 결제를 취소할 수 없습니다.");
        }
        return payment.getTransactionId();
    }

    @Transactional
    public void completeCancellation(String merchantUid, UserEntity user) {
        PaymentEntity payment = paymentRepository.findByMerchantUid(merchantUid)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));

        if (!payment.getUser().getId().equals(user.getId())) {
            throw new InternalServerException(ErrorCode.NOT_OWNER, "권한 없음");
        }
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "취소 반영 불가능 상태");
        }

        pointService.usePoints(user, payment.getProduct().getPoint(),
            "결제 취소 (주문번호: " + payment.getMerchantUid() + ")",
            "PAYMENT_REFUND:" + payment.getMerchantUid());
        payment.cancelPayment();
    }

    /**
     * 특정 사용자의 결제 내역을 페이징하여 조회합니다.
     * (readOnly = true)는 데이터 변경이 없는 조회 전용 트랜잭션임을 명시하여 성능을 최적화합니다.
     */
    @Transactional(readOnly = true)
    public Page<PaymentHistoryResponseDto> getMyPaymentHistory(UserEntity user, Pageable pageable) {
        // 1. 리포지토리를 호출하여 특정 사용자의 결제 내역을 최신순으로 페이징하여 조회
        Page<PaymentEntity> paymentPage = paymentRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        // 2. 조회된 Entity 페이지를 DTO 페이지로 변환하여 리턴
        return paymentPage.map(PaymentHistoryResponseDto::from);
    }
}
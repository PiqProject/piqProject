package piq.piqproject.domain.payments.web.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.portone.sdk.server.payment.CancelledPayment;
import io.portone.sdk.server.payment.FailedPayment;
import io.portone.sdk.server.payment.PaidPayment;
// V2 SDK Import 확인 필수
import io.portone.sdk.server.payment.Payment;
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
import piq.piqproject.domain.payments.web.dto.request.WebPaymentVerificationRequestDto;
import piq.piqproject.domain.points.service.PointService;
import piq.piqproject.domain.products.entity.ProductEntity;
import piq.piqproject.domain.products.repository.ProductRepository;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.infra.external.portone.service.PortOneClientService;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebPaymentService {
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final PointService pointService;
    private final PaymentUpdateService paymentUpdateService;
    private final PortOneClientService portOneClientService;

    @Value("${payment.refund.limit-days:7}")
    private int refundLimitDays;

    @Transactional
    public String preparePayment(UserEntity user, WebPaymentPrepareRequestDto request) {
        log.info("결제 사전 등록 시작: user={}, amount={}", user.getId(), request.getAmount());

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

    public void verifyPayment(WebPaymentVerificationRequestDto request) {
        log.info("결제 검증 시작: merchantUid={}, paymentId={}", request.getMerchantUid(), request.getPaymentId());

        // 1. 포트원 조회 (V2)
        Payment portOnePayment;
        try {
            portOnePayment = portOneClientService.getPaymentInfo(request.getPaymentId());
        } catch (Exception e) {
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "조회 실패");
        }

        // 2. Payment 타입에 따른 분기 처리 (Casting)
        if (portOnePayment instanceof PaidPayment paid) {
            // [1] 결제 완료상태
            BigDecimal actualAmount = BigDecimal.valueOf(paid.getAmount().getTotal());
            try {
                // DB 업데이트 실행
                paymentUpdateService.updateSuccess(request.getMerchantUid(), request.getPaymentId(), actualAmount);
                log.info("검증 성공: PAID");
            } catch (InternalServerException e) {
                // 중요: DB 업데이트 중 예외(금액 불일치 등)가 발생하면 포트원 결제 강제 취소
                log.error("DB 업데이트 중 오류 발생 - 포트원 결제 강제 취소 시도: {}", e.getMessage());
                portOneClientService.cancelPayment(request.getPaymentId(), "DB 업데이트 실패로 인한 자동취소: " + e.getMessage());
                throw e; // 예외를 다시 던져서 컨트롤러에서 처리하게 함
            }
        } else if (portOnePayment instanceof CancelledPayment cancelled) {
            // [2] 이미 취소된 결제
            log.warn("이미 취소된 결제입니다:{}", cancelled.getMerchantId());
            paymentUpdateService.updateFailure(request.getMerchantUid());
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "이미 취소된 결제입니다.");

        } else if (portOnePayment instanceof FailedPayment failed) {
            // [3] 결제 실패상태
            log.warn("결제 실패 상태입니다: {}", failed.getMerchantId());
            paymentUpdateService.updateFailure(request.getMerchantUid());
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "결제 실패 상태입니다.");

        } else {
            // [4] READY(대기), VIRTUAL_ACCOUNT_ISSUED(가상계좌 발급) 등
            // 즉시 결제 완료가 아닌 상태
            log.warn("결제 완료 상태가 아닙니다. 현재 타입: {}", portOnePayment.getClass().getSimpleName());
            paymentUpdateService.updateFailure(request.getMerchantUid());
            portOneClientService.cancelPayment(request.getPaymentId(), "결제 미완료");
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "결제가 완료되지 않았습니다.");
        }
    }

    @Transactional
    public void cancelPayment(WebPaymentCancelRequestDto request, UserEntity user) {
        PaymentEntity payment = paymentRepository.findByMerchantUid(request.getMerchantUid())
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));

        if (!payment.getUser().getId().equals(user.getId())) {
            throw new InternalServerException(ErrorCode.NOT_OWNER, "권한 없음");
        }
        if (payment.getStatus() != piq.piqproject.domain.payments.common.enums.PaymentStatus.PAID) {
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "취소 불가능 상태");
        }
        payment.validateRefundableDate(refundLimitDays);

        // 포인트 회수 시도
        pointService.usePoints(user, payment.getProduct().getPoint(),
                "결제 취소 (주문번호: " + payment.getMerchantUid() + ")");

        portOneClientService.cancelPayment(payment.getTransactionId(), request.getReason());

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
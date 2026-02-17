package piq.piqproject.domain.payments.web.service;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.InternalServerException;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.domain.payments.common.dto.response.PaymentHistoryResponseDto;
import piq.piqproject.domain.payments.common.entity.PaymentEntity;
import piq.piqproject.domain.payments.common.enums.PaymentStatus;
import piq.piqproject.domain.payments.common.repository.PaymentRepository;
import piq.piqproject.domain.payments.web.dto.request.WebPaymentCancelRequestDto;
import piq.piqproject.domain.payments.web.dto.request.WebPaymentPrepareRequestDto;
import piq.piqproject.domain.payments.web.dto.request.WebPaymentVerificationRequestDto;
import piq.piqproject.domain.payments.common.enums.PaymentType;
import piq.piqproject.domain.points.enums.PointType;
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
                .status(PaymentStatus.READY)
                .type(PaymentType.PORTONE)
                .user(user)
                .product(product)
                .build();
        paymentRepository.save(payment);

        portOneClientService.preparePayment(merchantUid, BigDecimal.valueOf(request.getAmount()));

        return merchantUid;
    }

    @Transactional
    public void verifyPayment(WebPaymentVerificationRequestDto request) {
        log.info("결제 검증 시작: merchantUid={}, impUid={}", request.getMerchantUid(), request.getImpUid());

        // 1. 우리 DB에서 결제 내역 조회 (비관적 락으로 중복 처리 방지)
        PaymentEntity paymentEntity = paymentRepository.findByMerchantUidWithLock(request.getMerchantUid())
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));

        // 가맹점 식별코드 불일치 또는 imp_uid 오류 등으로 조회가 안 될 경우를 대비해 try-catch
        IamportResponse<Payment> iamportResponse;
        try {
            iamportResponse = portOneClientService.getPaymentInfo(request.getImpUid());
        } catch (Exception e) {
            log.error("PortOne API 호출 중 예외 발생: {}", e.getMessage());
            // 시스템 오류 시에도 일단 취소 시도
            portOneClientService.cancelPayment(request.getImpUid(), "검증 과정 중 시스템 오류", paymentEntity.getAmount());
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "포트원 결제 조회 중 오류가 발생했습니다.");
        }

        // 2. 포트원 응답 결과 확인
        if (iamportResponse == null || iamportResponse.getResponse() == null) {
            log.error("PortOne에서 결제 정보를 찾을 수 없음: impUid={}, code={}, msg={}",
                    request.getImpUid(),
                    iamportResponse != null ? iamportResponse.getCode() : "null",
                    iamportResponse != null ? iamportResponse.getMessage() : "null");

            // 정보가 없는데 왜 취소하나 싶지만, 사용자가 "완료"라고 한다면 API 키 불일치 등의 이유로 못 찾는 것일 수 있으므로 취소 시도
            portOneClientService.cancelPayment(request.getImpUid(), "결제 정보 조회 불가로 인한 자동 취소", paymentEntity.getAmount());

            throw new NotFoundException(ErrorCode.NOT_FOUND, "포트원에서 결제 정보를 찾을 수 없습니다. 관리자에게 문의하세요.");
        }

        // 3. 결제 금액 검증
        BigDecimal expectedAmount = paymentEntity.getAmount();
        BigDecimal actualAmount = iamportResponse.getResponse().getAmount();

        if (actualAmount == null || expectedAmount.compareTo(actualAmount) != 0) {
            log.error("결제 금액 불일치: merchantUid={}, 기대금액={}, 실제금액={}",
                    request.getMerchantUid(), expectedAmount, actualAmount);
            paymentEntity.failPayment();
            portOneClientService.cancelPayment(request.getImpUid(), "결제 금액 불일치", paymentEntity.getAmount());
            throw new InternalServerException(ErrorCode.INVALID_PAYMENT_AMOUNT, "결제 금액이 일치하지 않습니다.");
        }

        // 4. 결제 상태 검증
        if ("paid".equals(iamportResponse.getResponse().getStatus())) {
            paymentEntity.completePayment(request.getImpUid());
            pointService.chargePoints(
                    paymentEntity.getUser(),
                    paymentEntity.getProduct().getPoint(),
                    PointType.CHARGE,
                    "포인트 충전 (상품ID: " + paymentEntity.getProduct().getId() + ")");
            log.info("결제 및 포인트 충전 완료: merchantUid={}", request.getMerchantUid());
        } else {
            log.warn("결제 상태가 'paid'가 아님: status={}", iamportResponse.getResponse().getStatus());
            paymentEntity.failPayment();
            portOneClientService.cancelPayment(request.getImpUid(),
                    "결제 미완료 상태 (" + iamportResponse.getResponse().getStatus() + ")", paymentEntity.getAmount());
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "결제가 완료되지 않은 상태입니다.");
        }
    }

    @Transactional
    public void cancelPayment(WebPaymentCancelRequestDto request, UserEntity user) {
        PaymentEntity payment = paymentRepository.findByMerchantUid(request.getMerchantUid())
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));

        if (!payment.getUser().getId().equals(user.getId())) {
            throw new InternalServerException(ErrorCode.NOT_OWNER, "권한 없음");
        }
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "취소 불가능 상태");
        }
        payment.validateRefundableDate(refundLimitDays);

        // 포인트 회수 시도
        pointService.usePoints(user, payment.getProduct().getPoint(),
                "결제 취소 (주문번호: " + payment.getMerchantUid() + ")");

        portOneClientService.cancelPayment(payment.getTransactionId(), request.getReason(), payment.getAmount());

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
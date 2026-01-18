package piq.piqproject.domain.payments.service;

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
import piq.piqproject.domain.payments.dto.Request.PaymentCancelRequestDto;
import piq.piqproject.domain.payments.dto.Request.PaymentPrepareRequestDto;
import piq.piqproject.domain.payments.dto.Request.PaymentVerificationRequestDto;
import piq.piqproject.domain.payments.dto.Response.PaymentHistoryResponseDto;
import piq.piqproject.domain.payments.entity.PaymentEntity;
import piq.piqproject.domain.payments.enums.PaymentStatus;
import piq.piqproject.domain.payments.repository.PaymentRepository;
import piq.piqproject.domain.points.enums.PointType;
import piq.piqproject.domain.points.service.PointService;
import piq.piqproject.domain.products.entity.ProductEntity;
import piq.piqproject.domain.products.repository.ProductRepository;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.infra.external.portone.service.PortOneClientService;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final PointService pointService;

    private final PortOneClientService portOneClientService;

    @Value("${payment.refund.limit-days:7}")
    private int refundLimitDays;

    @Transactional
    public String preparePayment(UserEntity user, PaymentPrepareRequestDto request) {
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
                .user(user)
                .product(product)
                .build();
        paymentRepository.save(payment);

        portOneClientService.preparePayment(merchantUid, BigDecimal.valueOf(request.getAmount()));

        return merchantUid;
    }

    @Transactional
    public void verifyPayment(PaymentVerificationRequestDto request) {
        PaymentEntity paymentEntity = paymentRepository.findByMerchantUidWithLock(request.getMerchantUid())
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));

        IamportResponse<Payment> iamportResponse = portOneClientService.getPaymentInfo(request.getImpUid());

        BigDecimal expectedAmount = paymentEntity.getAmount();
        BigDecimal actualAmount = iamportResponse.getResponse().getAmount();

        if (!expectedAmount.equals(actualAmount)) {
            paymentEntity.failPayment();
            portOneClientService.cancelPayment(iamportResponse.getResponse().getImpUid(), "결제 금액 불일치",
                    paymentEntity.getAmount());
            throw new InternalServerException(ErrorCode.INVALID_PAYMENT_AMOUNT, "금액 불일치");
        }

        if ("paid".equals(iamportResponse.getResponse().getStatus())) {
            paymentEntity.completePayment(request.getImpUid());
            pointService.chargePoints(
                    paymentEntity.getUser(),
                    paymentEntity.getProduct().getPoint(),
                    PointType.CHARGE,
                    "포인트 충전 (상품ID: " + paymentEntity.getProduct().getId() + ")");
        } else {
            paymentEntity.failPayment();
            portOneClientService.cancelPayment(request.getImpUid(), "결제 상태 불일치", paymentEntity.getAmount());
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "유효하지 않은 결제 상태");
        }
    }

    @Transactional
    public void cancelPayment(PaymentCancelRequestDto request, UserEntity user) {
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

        portOneClientService.cancelPayment(payment.getImpUid(), request.getReason(), payment.getAmount());

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
        return paymentPage.map(PaymentHistoryResponseDto::new);
    }
}
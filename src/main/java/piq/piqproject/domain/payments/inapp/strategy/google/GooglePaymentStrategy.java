package piq.piqproject.domain.payments.inapp.strategy.google;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.InternalServerException;
import piq.piqproject.common.error.exception.InvalidRequestException;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.domain.payments.common.entity.PaymentEntity;
import piq.piqproject.domain.payments.common.enums.PaymentStatus;
import piq.piqproject.domain.payments.common.enums.PaymentType;
import piq.piqproject.domain.payments.common.repository.PaymentRepository;
import piq.piqproject.domain.payments.inapp.dto.request.GooglePaymentRequestDto;
import piq.piqproject.domain.payments.inapp.strategy.InAppPaymentResult;
import piq.piqproject.domain.payments.inapp.strategy.InAppPaymentStrategy;
import piq.piqproject.domain.points.enums.PointType;
import piq.piqproject.domain.points.service.PointService;
import piq.piqproject.domain.products.entity.ProductEntity;
import piq.piqproject.domain.products.repository.ProductRepository;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.infra.external.google.service.GooglePlayClientService;

import com.google.api.services.androidpublisher.model.ProductPurchase;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GooglePaymentStrategy implements InAppPaymentStrategy {

    private final GooglePlayClientService googlePlayClientService;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final PointService pointService;

    @Override
    @Transactional
    public InAppPaymentResult verifyAndProcessPayment(Object request, UserEntity user) {
        if (!(request instanceof GooglePaymentRequestDto googleRequest)) {
            throw new InvalidRequestException(ErrorCode.BAD_REQUEST, "Invalid request type for GooglePaymentStrategy");
        }

        // 1. 상품 정보 조회
        ProductEntity product = productRepository.findById(googleRequest.getProductId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_PRODUCT));

        // 2. Google Play API로 구매 내역 조회
        ProductPurchase purchase = googlePlayClientService.getProductPurchase(
                googleRequest.getGoogleProductId(),
                googleRequest.getPurchaseToken());

        // 3. 구매 상태 확인 (0: purchased, 1: canceled, 2: pending)
        if (purchase.getPurchaseState() != 0) {
            log.error("Invalid Google payment state: purchaseState={}", purchase.getPurchaseState());
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "유효하지 않은 결제 상태입니다.");
        }

        String orderId = purchase.getOrderId();

        // 4. 중복 결제 확인 (transactionId가 Google orderId)
        if (paymentRepository.findByTransactionId(orderId).isPresent()) {
            log.warn("Google payment already processed: orderId={}", orderId);
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "이미 처리된 결제건입니다.");
        }

        // 5. 결제 정보 저장
        String merchantUid = "APP_G_" + UUID.randomUUID().toString();
        PaymentEntity payment = PaymentEntity.builder()
                .merchantUid(merchantUid)
                .transactionId(orderId)
                .type(PaymentType.GOOGLE)
                .amount(BigDecimal.valueOf(product.getPrice()))
                .status(PaymentStatus.PAID)
                .user(user)
                .product(product)
                .build();
        paymentRepository.save(payment);

        // 6. 포인트 지급
        pointService.chargePoints(
                user,
                product.getPoint(),
                PointType.CHARGE,
                "Google 인앱 결제 충전 (상품: " + product.getId() + ")");

        // 7. 구매 승인 (Acknowledge) - 중요: 승인하지 않으면 수일 내 자동 환불됨
        if (purchase.getAcknowledgementState() == 0) { // 0: yet to be acknowledged
            googlePlayClientService.acknowledgePurchase(
                    googleRequest.getGoogleProductId(),
                    googleRequest.getPurchaseToken());
        }

        return InAppPaymentResult.builder()
                .success(true)
                .transactionId(orderId)
                .merchantUid(merchantUid)
                .pointsGranted(product.getPoint())
                .message("Google 인앱 결제가 성공적으로 처리되었습니다.")
                .build();
    }

    @Override
    public PaymentType getPaymentType() {
        return PaymentType.GOOGLE;
    }
}

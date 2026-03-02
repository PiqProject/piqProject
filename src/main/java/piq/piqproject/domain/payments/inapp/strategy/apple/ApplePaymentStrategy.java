package piq.piqproject.domain.payments.inapp.strategy.apple;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
import piq.piqproject.domain.payments.inapp.dto.request.ApplePaymentRequestDto;
import piq.piqproject.domain.payments.inapp.strategy.InAppPaymentResult;
import piq.piqproject.domain.payments.inapp.strategy.InAppPaymentStrategy;
import piq.piqproject.domain.points.enums.PointType;
import piq.piqproject.domain.points.service.PointService;
import piq.piqproject.domain.products.entity.ProductEntity;
import piq.piqproject.domain.products.repository.ProductRepository;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.infra.external.apple.service.AppleStoreClientService;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
// application.properties의 apple.iap.enabled 값이 "true"일 때만 이 빈을 생성함
// 값이 false거나 아예 없으면(matchIfMissing = false) 빈 등록을 안 함 -> 에러 안 남
@ConditionalOnProperty(name = "apple.iap.enabled", havingValue = "true")
public class ApplePaymentStrategy implements InAppPaymentStrategy {

    private final AppleStoreClientService appleStoreClientService;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final PointService pointService;

    @Override
    @Transactional
    public InAppPaymentResult verifyAndProcessPayment(Object request, UserEntity user) {
        if (!(request instanceof ApplePaymentRequestDto appleRequest)) {
            throw new InvalidRequestException(ErrorCode.BAD_REQUEST, "Invalid request type for ApplePaymentStrategy");
        }

        // 1. 상품 정보 조회
        ProductEntity product = productRepository.findById(appleRequest.getProductId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_PRODUCT));

        // 2. Apple App Store Server API로 거래 정보 조회 (JWS 반환)
        String signedTransactionInfo = appleStoreClientService.getTransactionInfo(appleRequest.getTransactionId());

        // 3. JWS 디코딩 (서버 API 응답이므로 신뢰할 수 있다고 가정하거나, 추가적인 Root Cert 검증 가능)
        DecodedJWT decodedJWT = JWT.decode(signedTransactionInfo);

        // 필드 추출 (Apple Transaction Info fields)
        String originalTransactionId = decodedJWT.getClaim("originalTransactionId").asString();
        String productId = decodedJWT.getClaim("productId").asString();
        String transactionId = decodedJWT.getClaim("transactionId").asString();
        Long revocationDate = decodedJWT.getClaim("revocationDate").asLong(); // 환불 여부

        // 4. 검증 (상품 ID 일치 여부 및 환불 여부)
        if (!productId.equals(appleRequest.getAppleProductId())) {
            log.error("Apple product ID mismatch: expected={}, actual={}", appleRequest.getAppleProductId(), productId);
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "상품 정보가 일치하지 않습니다.");
        }

        if (revocationDate != null) {
            log.error("Apple payment is cancelled (refunded): transactionId={}", transactionId);
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "취소된 결전건입니다.");
        }

        // 5. 중복 결제 확인 (originalTransactionId 기준)
        if (paymentRepository.findByTransactionId(originalTransactionId).isPresent()) {
            log.warn("Apple payment already processed: originalTransactionId={}", originalTransactionId);
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "이미 처리된 결제건입니다.");
        }

        // 6. 결제 정보 저장
        String merchantUid = "APP_A_" + UUID.randomUUID().toString();
        PaymentEntity payment = PaymentEntity.builder()
                .merchantUid(merchantUid)
                .transactionId(originalTransactionId)
                .type(PaymentType.APPLE)
                .amount(BigDecimal.valueOf(product.getPrice()))
                .status(PaymentStatus.PAID)
                .user(user)
                .product(product)
                .build();
        paymentRepository.save(payment);

        // 7. 포인트 지급
        pointService.chargePoints(
                user,
                product.getPoint(),
                PointType.CHARGE,
                "Apple 인앱 결제 충전 (상품: " + product.getId() + ")");

        return InAppPaymentResult.builder()
                .success(true)
                .transactionId(originalTransactionId)
                .merchantUid(merchantUid)
                .pointsGranted(product.getPoint())
                .message("Apple 인앱 결제가 성공적으로 처리되었습니다.")
                .build();
    }

    @Override
    public PaymentType getPaymentType() {
        return PaymentType.APPLE;
    }
}

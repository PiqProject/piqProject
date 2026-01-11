package piq.piqproject.domain.payments.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.request.PrepareData;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;

import jakarta.annotation.PostConstruct;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final PointService pointService;
    private IamportClient iamportClient;

    @Value("${payment.refund.limit-days:7}") // 값이 없으면 기본 7일
    private int refundLimitDays;

    @Value("${portone.api.key}")
    private String restApiKey;

    @Value("${portone.api.secret}")
    private String restApiSecret;

    @PostConstruct
    public void init() {
        this.iamportClient = new IamportClient(restApiKey, restApiSecret);
    }

    @Transactional
    public String preparePayment(UserEntity user, PaymentPrepareRequestDto request) {
        // 0. shop에서 product객체 가져오기
        ProductEntity product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_PRODUCT));

        // 1. 결제될 금액 검증
        int expectedAmount = product.getPrice();
        if (request.getAmount() != expectedAmount) {
            throw new InternalServerException(ErrorCode.INVALID_PAYMENT_AMOUNT, "결제 금액이 유효하지 않습니다.");
        }

        // 2. 결제 정보 DB에 저장
        String merchantUid = String.valueOf(UUID.randomUUID());
        PaymentEntity payment = PaymentEntity.builder()
                .merchantUid(merchantUid)
                .amount(BigDecimal.valueOf(request.getAmount()))
                .status(PaymentStatus.READY) // '준비' 상태로 설정
                .user(user)
                .product(product)
                .build();
        paymentRepository.save(payment);

        // 3. 포트원 서버에 결제 정보 사전 등록
        // 결제 건을 고유하게 식별하기 위한 고유 주문번호(서버내에서 식별하는 고유번호)
        try {
            PrepareData prepareData = new PrepareData(merchantUid, BigDecimal.valueOf(request.getAmount()));
            // 포트원의 사전검증API를 호출. 이걸 호출하면 포트원 서버에 merchant_uid,amount가 등록되고, 이후 프론트에서 결제 실행시
            // 금액이 다르면 결제를 막아준다.
            iamportClient.postPrepare(prepareData);
            log.info("포트원 사전 등록 성공: merchant_uid={}", merchantUid);
            return merchantUid;
        } catch (Exception e) {
            log.error("포트원 사전 등록 실패", e);
            // DB에 저장된 Payment 정보도 롤백되어야 하므로 예외를 던집니다.
            throw new RuntimeException("결제 사전 등록에 실패했습니다.", e);
        }
    }

    @Transactional
    public void verifyPayment(PaymentVerificationRequestDto request) {
        // 1. 우리 DB에서 merchant_uid로 결제 정보(PaymentEntity)를 조회합니다.
        PaymentEntity paymentEntity = paymentRepository.findByMerchantUidWithLock(request.getMerchantUid())
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "해당 merchant_uid에 대한 결제 정보를 찾을 수 없습니다."));

        // 2. 포트원 서버를 통해 imp_uid로 실제 결제 정보를 조회합니다.
        IamportResponse<Payment> iamportResponse;
        try {
            iamportResponse = iamportClient.paymentByImpUid(request.getImpUid());
        } catch (Exception e) {
            log.error("포트원 결제 정보 조회 실패", e);
            // 조회 실패 시, 우리 DB의 결제 상태를 FAILED로 변경하고 예외를 던집니다.
            paymentEntity.failPayment();
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "포트원 결제 정보 조회에 실패했습니다.");
        }

        // 3. ★★★ 핵심 검증 로직 ★★★
        // DB에 저장된 금액과 포트원에서 조회한 실제 결제 금액을 비교합니다.
        BigDecimal expectedAmount = paymentEntity.getAmount();
        BigDecimal actualAmount = iamportResponse.getResponse().getAmount();

        if (!expectedAmount.equals(actualAmount)) {
            // 결제 금액이 일치하지 않는 경우, 비정상적인 접근으로 간주하고 결제를 취소합니다.
            log.error("결제 금액 위변조 시도 감지: merchantUid={}, expected={}, actual={}",
                    request.getMerchantUid(), expectedAmount, actualAmount);

            paymentEntity.failPayment(); // DB 상태를 '실패'로 변경

            // 포트원에 해당 결제 취소 요청을 보냅니다.
            requestCancelToPortone(iamportResponse.getResponse().getImpUid(), "결제 금액 불일치", paymentEntity.getAmount());
            throw new InternalServerException(ErrorCode.INVALID_PAYMENT_AMOUNT, "실제 결제 금액과 주문 금액이 일치하지 않습니다.");
        }

        // 4. 모든 검증을 통과한 경우, 결제 완료 처리
        // 포트원 응답에서 결제 상태가 "paid"인지 추가로 확인하면 더 안전합니다.
        if ("paid".equals(iamportResponse.getResponse().getStatus())) {
            paymentEntity.completePayment(request.getImpUid()); // DB 상태를 'PAID'로 변경하고, impUid 저장
            UserEntity user = paymentEntity.getUser();
            int pointsToAdd = paymentEntity.getProduct().getPoint(); // 상품에 정의된 포인트 지급

            pointService.chargePoints(
                    user,
                    pointsToAdd,
                    PointType.CHARGE,
                    "포인트 충전 (상품ID: " + paymentEntity.getProduct().getId() + ")");

            log.info("[Payment Success] User: {}, Amount: {}, Points: +{}",
                    user.getId(), paymentEntity.getAmount(), pointsToAdd);
        } else {
            // 결제는 됐으나, 포트원 최종 상태가 'paid'가 아닌 경우 (예: 'ready' 등)
            // 비정상 상태로 간주하고 실패 처리
            paymentEntity.failPayment();
            requestCancelToPortone(request.getImpUid(), "포트원 결제 상태 불일치", paymentEntity.getAmount());
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "포트원의 결제 상태가 유효하지 않습니다.");
        }
    }

    /**
     * 결제 취소 로직 (결제 완료 상태에서 사용자의 변심으로 인한 취소)
     * 
     * @param request 결제 취소 요청 DTO
     * @param user    현재 로그인한 사용자 정보
     */
    @Transactional
    public void cancelPayment(PaymentCancelRequestDto request, UserEntity user) {
        // 1. DB 조회
        PaymentEntity payment = paymentRepository.findByMerchantUid(request.getMerchantUid())
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "해당 merchant_uid에 대한 결제 정보를 찾을 수 없습니다."));

        // 2.검증 (소유권, 상태)
        // 2-1. 결제 소유권 검증
        if (!payment.getUser().getId().equals(user.getId())) {
            throw new InternalServerException(ErrorCode.NOT_OWNER, "해당 결제를 갖는 사용자가 아닙니다.");
        }

        // 2-2. 결제 상태 검증
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "이미 취소되었거나 결제 완료되지 않은 건은 취소할 수 없습니다.");
        }

        // 2-3. 환불 기간 검증
        payment.validateRefundableDate(refundLimitDays);

        // 3. 포인트 차감
        int pointsToRevoke = payment.getProduct().getPoint();

        try {
            // 환불로 인한 포인트 차감 (PointType.REFUND)
            // 여기서는 usePoints를 써서 차감 효과를 냄.
            pointService.usePoints(
                    user,
                    pointsToRevoke,
                    "결제 취소/환불 (주문번호: " + payment.getMerchantUid() + ")");

            log.info("[Payment Cancel] User: {}, Refund Amount: {}, Points Revoked: -{}",
                    user.getId(), payment.getAmount(), pointsToRevoke);

        } catch (Exception e) {
            log.error("[Refund Fail] 포인트 부족으로 회수 실패. User: {}", user.getId());
            throw new InternalServerException(ErrorCode.NOT_ENOUGH_POINT, "이미 포인트를 사용하여 환불할 수 없습니다.");
        }

        // 4. 포트원 결제 취소 API 호출
        requestCancelToPortone(payment.getImpUid(), request.getReason(), payment.getAmount());

        // 5. DB 상태 변경(Paid -> Cancelled)
        payment.cancelPayment();
    }

    /**
     * [내부 전용] 포트원 결제 취소 API를 직접 호출하는 로직
     * 
     * @param impUid 취소할 결제의 포트원 거래 고유번호
     * @param reason 취소 사유
     * @param amount 취소할 금액 (일반적으로 전체 금액)
     */
    private void requestCancelToPortone(String impUid, String reason, BigDecimal amount) {
        try {
            CancelData cancelData = new CancelData(impUid, false, amount);
            cancelData.setReason(reason);

            log.info("포트원 결제 취소 요청: impUid={}, reason={}", impUid, reason);
            IamportResponse<Payment> response = iamportClient.cancelPaymentByImpUid(cancelData);
            if (!"cancelled".equals(response.getResponse().getStatus())) {
                log.error("포트원 결제 취소 실패 응답: impUid={}, response={}", impUid, response);
                throw new Exception();
            }
            log.info("포트원 결제 취소 성공: impUid={}", impUid);

        } catch (Exception e) {
            log.error("포트원 결제 취소 중 예외 발생: impUid={}", impUid, e);
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "포트원측에서 결제 취소 중 오류가 발생했습니다.");
        }
    }

    /**
     * 특정 사용자의 결제 내역을 페이징하여 조회합니다.
     * (readOnly = true)는 데이터 변경이 없는 조회 전용 트랜잭션임을 명시하여 성능을 최적화합니다.
     */
    @Transactional(readOnly = true)
    public Page<PaymentHistoryResponseDto> getMyPaymentHistory(UserEntity user, Pageable pageable) {
        // 1. 리포지토리를 호출하여 특정 사용자의 결제 내역을 페이징하여 조회 (Entity 페이지)
        Page<PaymentEntity> paymentPage = paymentRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        // 2. 조회된 Entity 페이지를 DTO 페이지로 변환
        // Page 객체의 .map() 메소드를 사용하면 페이징 정보는 그대로 유지하면서 내용물만 쉽게 변환할 수 있습니다.
        return paymentPage.map(PaymentHistoryResponseDto::new);
    }
}

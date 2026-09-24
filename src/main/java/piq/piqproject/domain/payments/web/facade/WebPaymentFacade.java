package piq.piqproject.domain.payments.web.facade;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import io.portone.sdk.server.payment.CancelledPayment;
import io.portone.sdk.server.payment.FailedPayment;
import io.portone.sdk.server.payment.PaidPayment;
import io.portone.sdk.server.payment.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.error.exception.ConflictException;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.InternalServerException;
import piq.piqproject.domain.payments.common.dto.response.PaymentHistoryResponseDto;
import piq.piqproject.domain.payments.web.dto.request.WebPaymentCancelRequestDto;
import piq.piqproject.domain.payments.web.dto.request.WebPaymentPrepareRequestDto;
import piq.piqproject.domain.payments.web.dto.request.WebPaymentVerificationRequestDto;
import piq.piqproject.domain.payments.web.service.PaymentUpdateService;
import piq.piqproject.domain.payments.web.service.WebPaymentService;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.infra.external.portone.service.PortOneClientService;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebPaymentFacade {
    private final WebPaymentService webPaymentService;
    private final PaymentUpdateService paymentUpdateService;
    private final PortOneClientService portOneClientService;

    @Value("${payment.refund.limit-days:7}")
    private int refundLimitDays;

    public String preparePayment(UserEntity user, WebPaymentPrepareRequestDto request) {
        return webPaymentService.preparePayment(user, request);
    }

    public void verifyPayment(WebPaymentVerificationRequestDto request) {
        log.info("Starting payment verification: merchantUid={}, paymentId={}", request.getMerchantUid(),
                request.getPaymentId());

        Payment portOnePayment = portOneClientService.getPaymentInfo(request.getPaymentId());

        if (portOnePayment instanceof PaidPayment paid) {
            BigDecimal actualAmount = BigDecimal.valueOf(paid.getAmount().getTotal());
            try {
                paymentUpdateService.updateSuccess(request.getMerchantUid(), request.getPaymentId(), actualAmount);
                log.info("Verification successful: PAID");
            } catch (ConflictException e) {
                log.warn("Duplicate payment verification ignored: {}", e.getMessage());
                throw e;
            } catch (Exception e) {
                // 금액 불일치(InternalServerException) 및 DB 장애(DataAccessException 등) 시 자동 환불 처리
                log.error("Critical DB error during payment verification - rolling back PortOne payment: {}",
                        e.getMessage());
                try {
                    portOneClientService.cancelPayment(request.getPaymentId(), "시스템 오류로 인한 자동 환불: " + e.getMessage());
                } catch (Exception cancelEx) {
                    log.error("CRITICAL: PortOne compensating cancel failed! Manual refund required. paymentId={}",
                            request.getPaymentId(), cancelEx);
                }
                throw e;
            }

            return;
        }

        if (portOnePayment instanceof CancelledPayment cancelled) {
            log.warn("Payment is already cancelled: {}", cancelled.getMerchantId());
            paymentUpdateService.updateFailure(request.getMerchantUid());
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "이미 취소된 결제입니다.");
        }

        if (portOnePayment instanceof FailedPayment failed) {
            log.warn("Payment status is FAILED: {}", failed.getMerchantId());
            paymentUpdateService.updateFailure(request.getMerchantUid());
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "결제 실패 상태입니다.");
        }

        log.warn("Payment is not in PAID state. Current type: {}", portOnePayment.getClass().getSimpleName());
        paymentUpdateService.updateFailure(request.getMerchantUid());
        portOneClientService.cancelPayment(request.getPaymentId(), "결제 미완료");
        throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "결제가 완료되지 않았습니다.");
    }

    public void cancelPayment(WebPaymentCancelRequestDto request, UserEntity user) {
        String transactionId = webPaymentService.validateCancellation(request, user, refundLimitDays);
        portOneClientService.cancelPayment(transactionId, request.getReason());
        webPaymentService.completeCancellation(request.getMerchantUid(), user);
    }

    public Page<PaymentHistoryResponseDto> getMyPaymentHistory(UserEntity user, Pageable pageable) {
        return webPaymentService.getMyPaymentHistory(user, pageable);
    }
}

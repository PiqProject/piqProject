package piq.piqproject.infra.external.portone.service;

import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Component;

import io.portone.sdk.server.PortOneClient;
import io.portone.sdk.server.payment.Payment; // V2 Payment 객체
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.InternalServerException;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortOneClientService {

    private final PortOneClient portOneClient;

    /**
     * 포트원 결제 단건 조회 (Verify용)
     * V2 SDK를 사용하여 구현
     */
    public Payment getPaymentInfo(String paymentId) { // 변수명 impUid -> paymentId (의미상 동일)
        try {
            // V2는 paymentId(구 impUid)와 storeId가 필수
            CompletableFuture<Payment> payment = portOneClient.getPayment().getPayment(paymentId);

            log.info("PortOne V2 lookup successful: paymentId={}", paymentId);
            return payment.get();

        } catch (Exception e) {
            log.error("PortOne system error", e);
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "포트원 통신 중 오류 발생");
        }
    }

    public void cancelPayment(String paymentId, String reason) {
        try {
            // V2 SDK cancel 메서드 사용
            portOneClient.getPayment().cancelPayment(paymentId, null, null, null, reason, null, null, null, null,
                    null, null);
            log.info("PortOne payment cancellation successful: paymentId={}, reason={}", paymentId, reason);
        } catch (Exception e) {
            log.error("PortOne system error", e);
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "포트원 통신 중 오류 발생");
        }
    }
}

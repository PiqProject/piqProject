package piq.piqproject.domain.payments.inapp.webhook.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.InternalServerException;
import piq.piqproject.domain.payments.common.enums.PaymentType;
import piq.piqproject.domain.payments.inapp.service.RefundService;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.List;

/**
 * Apple App Store Server Notifications V2 처리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppleWebhookService {

    private final RefundService refundService;

    // 환불 알림 타입
    private static final String NOTIFICATION_TYPE_REFUND = "REFUND";

    /**
     * Apple 알림 처리
     * 
     * @param signedPayload JWS 형식의 서명된 페이로드
     */
    public void processNotification(String signedPayload) {
        try {
            // 1. 서명 검증 및 JWS 디코딩
            DecodedJWT decodedPayload = verifyAndDecodeJws(signedPayload);

            // 2. 페이로드에서 알림 타입 추출
            String notificationType = decodedPayload.getClaim("notificationType").asString();
            log.info("Apple 알림 수신 및 검증 완료: notificationType={}", notificationType);

            // 3. 환불 알림인 경우만 처리
            if (NOTIFICATION_TYPE_REFUND.equals(notificationType)) {
                handleRefund(decodedPayload);
            }

        } catch (Exception e) {
            log.error("Apple 웹훅 처리 중 오류", e);
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "Apple 웹훅 검증 또는 처리 실패");
        }
    }

    /**
     * JWS 서명 검증 및 디코딩
     */
    private DecodedJWT verifyAndDecodeJws(String jws) throws Exception {
        DecodedJWT decodedJWT = JWT.decode(jws);

        // 헤더에서 x5c (인증서 체인) 추출
        List<String> x5c = decodedJWT.getHeaderClaim("x5c").asList(String.class);
        if (x5c == null || x5c.isEmpty()) {
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "x5c header is missing");
        }

        // 첫 번째 인증서(리프 인증서)에서 공개키 추출
        byte[] certBytes = Base64.getDecoder().decode(x5c.get(0));
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(certBytes));

        // ECDSA (ES256) 알고리즘 설정을 위해 ECPublicKey 사용
        ECPublicKey publicKey = (ECPublicKey) cert.getPublicKey();
        Algorithm algorithm = Algorithm.ECDSA256(publicKey, null);

        // 검증 (Apple의 경우 발급자나 오디언스 검증은 필요에 따라 추가)
        JWTVerifier verifier = JWT.require(algorithm).build();
        return verifier.verify(jws);
    }

    /**
     * 환불 처리
     */
    private void handleRefund(DecodedJWT payload) throws Exception {
        // data.signedTransactionInfo 에서 거래 정보 추출
        String signedTransactionInfo = payload.getClaim("data")
                .asMap().get("signedTransactionInfo").toString();

        // 내부 서명 데이터(Transaction Info)도 동일한 방식으로 검증 및 디코딩
        DecodedJWT transactionInfo = verifyAndDecodeJws(signedTransactionInfo);
        String originalTransactionId = transactionInfo.getClaim("originalTransactionId").asString();

        log.info("Apple 환불 처리 진행: originalTransactionId={}", originalTransactionId);

        // RefundService로 환불 처리 위임
        refundService.processRefund(originalTransactionId, PaymentType.APPLE);
    }
}

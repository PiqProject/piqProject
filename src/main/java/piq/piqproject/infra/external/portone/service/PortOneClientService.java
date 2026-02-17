package piq.piqproject.infra.external.portone.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.request.PrepareData;
import com.siot.IamportRestClient.response.AccessToken;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.error.exception.CustomException;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.InternalServerException;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortOneClientService {

    @Value("${portone.api.key}")
    private String restApiKey;

    @Value("${portone.api.secret}")
    private String restApiSecret;

    private final RestClient restClient;
    private IamportClient iamportClient;

    @PostConstruct
    public void init() {
        String cleanKey = restApiKey.trim();
        String cleanSecret = restApiSecret.trim();
        this.iamportClient = new IamportClient(cleanKey, cleanSecret);

        try {
            log.info("[PortOne 인증 테스트] Key: {}***, Secret: {}***",
                    cleanKey.substring(0, 4), cleanSecret.substring(0, 4));
            IamportResponse<AccessToken> authResponse = iamportClient.getAuth();
            if (authResponse != null && authResponse.getCode() == 0) {
                log.info("=> PortOne 토큰 발급 성공! (Token exists: {})", authResponse.getResponse() != null);
            } else {
                log.error("=> PortOne 토큰 발급 실패: code={}, msg={}",
                        authResponse != null ? authResponse.getCode() : "null",
                        authResponse != null ? authResponse.getMessage() : "null");
            }
        } catch (Exception e) {
            log.error("=> PortOne 인증 테스트 중 예외 발생", e);
        }
    }

    /**
     * 포트원 결제 취소 요청
     */
    public void cancelPayment(String impUid, String reason, BigDecimal amount) {
        try {
            CancelData cancelData = new CancelData(impUid, false, amount);
            cancelData.setReason(reason);

            log.info("PortOne API 취소 요청: impUid={}, reason={}", impUid, reason);
            IamportResponse<Payment> response = iamportClient.cancelPaymentByImpUid(cancelData);

            if (response.getCode() != 0) {
                log.error("PortOne API 취소 실패: code={}, msg={}", response.getCode(), response.getMessage());
                throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR,
                        "PortOne 결제 취소 실패: " + response.getMessage());
            }

            if (!"cancelled".equals(response.getResponse().getStatus())) {
                log.error("PortOne response status != cancelled: status={}", response.getResponse().getStatus());
                throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR,
                        "결제 취소 요청은 성공했으나 response의 status가 cancelled가 아닙니다.");
            }

            log.info("PortOne API 취소 성공: impUid={}", impUid);

        } catch (Exception e) {
            log.error("PortOne 연동 중 예외 발생: impUid={}", impUid, e);
            if (e instanceof CustomException customException) {
                throw customException;
            }
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "PG사 연동 중 오류가 발생했습니다.");
        }
    }

    /**
     * 포트원 사전 등록 (Prepare)
     */
    public void preparePayment(String merchantUid, BigDecimal amount) {
        try {
            PrepareData prepareData = new PrepareData(merchantUid, amount);
            IamportResponse<com.siot.IamportRestClient.response.Prepare> response = iamportClient
                    .postPrepare(prepareData);

            if (response.getCode() != 0) {
                log.error("PortOne 사전 등록 실패: merchantUid={}, code={}, msg={}",
                        merchantUid, response.getCode(), response.getMessage());
                throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR,
                        "PortOne 결제 사전 등록 실패: " + response.getMessage());
            }

            log.info("PortOne 사전 등록 성공: merchantUid={}", merchantUid);
        } catch (Exception e) {
            log.error("PortOne 사전 등록 중 예외 발생: merchantUid={}", merchantUid, e);
            if (e instanceof InternalServerException)
                throw (InternalServerException) e;
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "결제 사전 등록 중 오류가 발생했습니다.");
        }
    }

    /**
     * 포트원 결제 단건 조회 (Verify용)
     * 테스트 채널 결제건 조회를 위해 include_sandbox=true 파라미터를 강제로 추가합니다.
     */
    public IamportResponse<Payment> getPaymentInfo(String impUid) {
        try {
            // 액세스 토큰 획득
            IamportResponse<AccessToken> authResponse = iamportClient.getAuth();
            if (authResponse == null || authResponse.getResponse() == null) {
                throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "PortOne 토큰 발급 실패");
            }
            String accessToken = authResponse.getResponse().getToken();

            // include_sandbox=true 파라미터를 포함하여 직접 API 호출
            return restClient.get()
                    .uri("https://api.iamport.kr/payments/{impUid}?include_sandbox=true", impUid)
                    .header("Authorization", accessToken)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<IamportResponse<Payment>>() {
                    });
        } catch (Exception e) {
            log.error("PortOne 결제 정보 조회 실패: impUid={}", impUid, e);
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "포트원 결제 정보 조회 실패: " + e.getMessage());
        }
    }
}

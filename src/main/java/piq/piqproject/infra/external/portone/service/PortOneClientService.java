package piq.piqproject.infra.external.portone.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.request.PrepareData;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.error.exception.CustomException;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.InternalServerException;

@Slf4j
@Component
public class PortOneClientService {

    @Value("${portone.api.key}")
    private String restApiKey;

    @Value("${portone.api.secret}")
    private String restApiSecret;

    private IamportClient iamportClient;

    @PostConstruct
    public void init() {
        this.iamportClient = new IamportClient(restApiKey, restApiSecret);
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

            // TODO: 결제 취소실패가 되면 다시 시도하는 로직 필요해 보인다.
            if (response.getCode() != 0) {
                log.error("PortOne API 취소 실패: code={}, msg={}", response.getCode(), response.getMessage());
                throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR,
                        "PortOne 결제 취소 실패: " + response.getMessage());
            }

            // 확실한 검증을 위해 status 확인
            if (!"cancelled".equals(response.getResponse().getStatus())) {
                log.error("PortOne response status != cancelled: status={}", response.getResponse().getStatus());
                throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR,
                        "결제 취소 요청은 성공했으나 response의 status가 cancelled가 아닙니다.");
            }

            log.info("PortOne API 취소 성공: impUid={}", impUid);

        } catch (Exception e) {
            log.error("PortOne 연동 중 예외 발생: impUid={}", impUid, e);
            // 이미 CustomException이라면 그대로 던지고, 아니면 감싸서 던짐
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
            iamportClient.postPrepare(prepareData);
            log.info("PortOne 사전 등록 성공: merchantUid={}", merchantUid);
        } catch (Exception e) {
            log.error("PortOne 사전 등록 실패: merchantUid={}", merchantUid, e);
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "결제 사전 등록에 실패했습니다.");
        }
    }

    /**
     * 포트원 결제 단건 조회 (Verify용)
     */
    public IamportResponse<Payment> getPaymentInfo(String impUid) {
        try {
            return iamportClient.paymentByImpUid(impUid);
        } catch (Exception e) {
            log.error("PortOne 결제 정보 조회 실패: impUid={}", impUid, e);
            throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR, "포트원 결제 정보 조회 실패");
        }
    }
}

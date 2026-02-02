package piq.piqproject.domain.payments.web.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WebPaymentCancelRequestDto {
    private String merchantUid; // 취소할 주문의 우리 시스템 주문번호
    private String reason; // 취소 사유
}
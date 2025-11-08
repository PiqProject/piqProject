package piq.piqproject.domain.payments.dto.Request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentVerificationRequestDto {
    private String impUid; // 포트원 거래 고유번호
    private String merchantUid; // 우리 시스템의 주문번호
}
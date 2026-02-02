package piq.piqproject.domain.payments.web.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 프론트엔드에서 결제 검증을 위해 보낼 데이터
@Getter
@Setter
@NoArgsConstructor
public class WebPaymentPrepareRequestDto {
    private Long productId; // 상품 ID
    private int amount; // 결제 금액
}
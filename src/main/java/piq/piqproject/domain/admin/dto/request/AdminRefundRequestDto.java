package piq.piqproject.domain.admin.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminRefundRequestDto {

    @NotNull(message = "주문번호(merchant_uid)는 필수입니다.")
    private String merchantUid; // 서버에서 생성했던 고유 주문 번호

    @NotNull(message = "환불 사유는 필수입니다.")
    private String cancelReason; // 관리자가 입력하는 사유
}
package piq.piqproject.domain.payments.inapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ApplePaymentRequestDto {
    @NotNull(message = "상품 ID는 필수입니다.")
    private Long productId;

    @NotBlank(message = "Apple Transaction ID는 필수입니다.")
    private String transactionId;

    @NotBlank(message = "Apple Product ID는 필수입니다.")
    private String appleProductId;
}

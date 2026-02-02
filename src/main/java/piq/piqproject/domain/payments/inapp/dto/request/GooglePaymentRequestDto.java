package piq.piqproject.domain.payments.inapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GooglePaymentRequestDto {
    @NotNull(message = "상품 ID는 필수입니다.")
    private Long productId;

    @NotBlank(message = "Google Purchase Token은 필수입니다.")
    private String purchaseToken;

    @NotBlank(message = "Google Product ID는 필수입니다.")
    private String googleProductId;
}

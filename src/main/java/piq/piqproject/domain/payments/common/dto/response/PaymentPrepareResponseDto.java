package piq.piqproject.domain.payments.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaymentPrepareResponseDto {
    private String merchantUid;
}
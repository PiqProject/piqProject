package piq.piqproject.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import piq.piqproject.domain.users.enums.Gender;

@Getter
@NoArgsConstructor
public class BulkPointRequestDto {

    /**
     * 대상 성별 (null 이면 모든 사용자 대상)
     */
    private Gender targetGender;

    /**
     * 조정할 포인트 금액 (양수: 지급, 음수: 차감)
     */
    @NotNull(message = "포인트 금액은 필수입니다.")
    private Integer amount;

    /**
     * 조정 사유
     */
    @NotBlank(message = "조정 사유는 필수입니다.")
    private String reason;
}

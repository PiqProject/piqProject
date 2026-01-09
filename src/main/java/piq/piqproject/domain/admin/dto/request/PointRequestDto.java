package piq.piqproject.domain.admin.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PointRequestDto {
    @NotNull
    private Integer amount; // 양수: 지급, 음수: 차감

    private String reason; // 지급/차감 사유 (로그용)
}
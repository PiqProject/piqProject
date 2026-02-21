package piq.piqproject.domain.traits.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DeleteTraitOptionRequestDto {
    @NotNull(message = "옵션 항목은 비어있을 수 없습니다.")
    private Long option;
}

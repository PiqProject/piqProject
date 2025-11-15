package piq.piqproject.domain.traits.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class DeleteTraitOptionRequestDto {
    @NotEmpty(message = "옵션 목록을 입력해주세요.")
    private List<@NotNull(message = "옵션 항목은 비어있을 수 없습니다.") Long> options;
}

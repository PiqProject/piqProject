package piq.piqproject.domain.traits.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TraitRequestDto {
    @NotBlank(message = "카테고리 이름은 비어있을 수 없습니다.")
    private String categoryName;

    @NotEmpty(message = "옵션 목록은 비어있을 수 없습니다.")
    private List<@NotBlank(message = "옵션 항목은 비어있을 수 없습니다.") String> optionNames;
}

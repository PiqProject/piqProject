package piq.piqproject.domain.interests.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class InterestRequestDto {

    //TODO: 범위 지정
    @NotBlank(message = "관심사 키워드를 입력해주세요.")
    private String keyword;
}

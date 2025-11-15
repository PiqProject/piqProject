package piq.piqproject.domain.users.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
public class UserTraitRequestDto {

    /**
     * 사용자가 자신의 특성으로 선택한 trait_options의 ID 목록입니다.
     */
    @NotEmpty(message = "특성 목록은 비어 있을 수 없습니다.")
    private List<Long> traitOptionIds;
}
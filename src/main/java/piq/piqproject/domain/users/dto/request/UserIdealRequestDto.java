package piq.piqproject.domain.users.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class UserIdealRequestDto {
    @NotEmpty(message = "이상형 키워드는 적어도 한개를 포함해야합니다.")
    private List<@NotNull(message = "이상형 옵션 아이디를 입력해주세요.") Long> idealOptionIds;
}
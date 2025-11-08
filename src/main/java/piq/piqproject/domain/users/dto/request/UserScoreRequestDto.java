package piq.piqproject.domain.users.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UserScoreRequestDto {
    @NotNull(message="점수를 매길 유저를 입력해주세요.")
    private Long targerUserId;

    @NotNull(message="점수를 입력해주세요")
    @Min(value = 0, message="최소 점수는 0점입니다.")
    @Max(value = 5, message="최대 점수는 5점입니다.")
    private int score;
}

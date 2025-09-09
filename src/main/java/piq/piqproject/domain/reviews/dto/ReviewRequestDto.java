package piq.piqproject.domain.reviews.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ReviewRequestDto {
    //todo: 제목 최대 길이에 대한 확정 필요 (현재 임시 50자)
    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 50, message = "제목은 50자 이하로 입력해주세요.")
    private final String title;

    //todo: 내용 최대 길이에 대한 확정 필요 (현재 제한 없음)
    private final String content;

    @NotNull(message = "별점을 입력해주세요.")
    @Min(value = 1, message = "별점은 1점 이상이어야 합니다.")
    @Max(value = 5, message = "별점은 5점을 초과할 수 없습니다.")
    private final int rate;
}

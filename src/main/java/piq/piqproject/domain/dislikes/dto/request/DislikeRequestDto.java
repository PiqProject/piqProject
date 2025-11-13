package piq.piqproject.domain.dislikes.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor // Jackson이 JSON을 객체로 변환할 때 기본 생성자가 필요합니다.
public class DislikeRequestDto {

    // '싫어요' 대상이 된 사용자의 ID
    private Long dislikedUserId;
}
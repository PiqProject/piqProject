package piq.piqproject.domain.users.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserMbtiRequestDto {
    @NotBlank(message = "MBTI를 입력해주세요.")
    private String mbti;

    public UserMbtiRequestDto(String mbti) {
        this.mbti = mbti;
    }
}

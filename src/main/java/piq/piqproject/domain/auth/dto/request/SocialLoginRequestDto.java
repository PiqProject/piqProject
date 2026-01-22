package piq.piqproject.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import piq.piqproject.domain.users.enums.SocialType;

@Getter
@NoArgsConstructor
public class SocialLoginRequestDto {

    @NotNull(message = "소셜 타입은 필수입니다.")
    private SocialType socialType;

    @NotBlank(message = "토큰은 필수입니다.")
    private String token;
}
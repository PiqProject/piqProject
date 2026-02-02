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

    @NotNull(message = "이용약관 동의는 필수입니다.")
    private Boolean termsAgreed;

    @NotNull(message = "개인정보처리방침 동의는 필수입니다.")
    private Boolean privacyPolicyAgreed;

    @NotNull(message = "위치정보 수집 동의는 필수입니다.")
    private Boolean locationInfoPolicyAgreed;

    @NotNull(message = "성인 여부는 필수입니다.")
    private Boolean isAdult;
}
package piq.piqproject.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 카카오 로그인 요청 DTO
 * 프론트엔드에서 카카오 인가 코드(Authorization Code)를 전달받습니다.
 */
public record KakaoLoginRequest(
        @NotBlank(message = "인가 코드는 필수입니다.") String code) {
}

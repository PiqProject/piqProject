package piq.piqproject.domain.users.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 로그인 성공 후 클라이언트에게 JWT 토큰을 응답하기 위한 DTO 입니다.
 */
@Getter
public class TokensResponseDto {
    private String accessToken;
    private String refreshToken;

    @Builder
    public TokensResponseDto(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
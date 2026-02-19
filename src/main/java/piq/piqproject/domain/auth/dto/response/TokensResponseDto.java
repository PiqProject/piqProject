package piq.piqproject.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 로그인 성공 후 클라이언트에게 JWT 토큰을 응답하기 위한 DTO 입니다.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokensResponseDto {
    private String accessToken;
    private String refreshToken;

    @Builder
    public TokensResponseDto(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
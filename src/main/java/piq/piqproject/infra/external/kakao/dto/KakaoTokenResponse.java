package piq.piqproject.infra.external.kakao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 카카오 인증 서버의 토큰 발급 응답 DTO
 * https://kauth.kakao.com/oauth/token
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoTokenResponse(
                @JsonProperty("token_type") String tokenType,
                @JsonProperty("access_token") String accessToken,
                @JsonProperty("expires_in") Integer expiresIn,
                @JsonProperty("refresh_token") String refreshToken,
                @JsonProperty("refresh_token_expires_in") Integer refreshTokenExpiresIn,
                @JsonProperty("scope") String scope) {
}

package piq.piqproject.domain.users.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 로그인 성공 후 클라이언트에게 JWT 토큰을 응답하기 위한 DTO 입니다.
 */
@Getter
@AllArgsConstructor // 모든 필드를 포함하는 생성자를 자동으로 생성합니다.
public class TokenResponseDto {
    private String accessToken;
}
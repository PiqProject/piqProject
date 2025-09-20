package piq.piqproject.domain.users.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Access Token만 담을 간단한 DTO
@Getter
@AllArgsConstructor
public class AccessTokenResponseDto {
    private String accessToken;
}
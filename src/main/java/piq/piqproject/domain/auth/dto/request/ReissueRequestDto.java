package piq.piqproject.domain.auth.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReissueRequestDto {
    // 앱에서 보낼 때는 여기에 담아서 보냄
    private String refreshToken;
}
package piq.piqproject.domain.matches.dto.response;

import lombok.Builder;
import lombok.Getter;
import piq.piqproject.domain.users.entity.UserEntity;

@Getter
@Builder
public class ContactExchangeResponseDto {

    private String kakaoTalkId;
    private String instagramId;

    public static ContactExchangeResponseDto from(UserEntity user) {
        return ContactExchangeResponseDto.builder()
                .kakaoTalkId(user.getKakaoTalkId())
                // .instagramId(user.getInstagramId())
                .build();
    }
}

package piq.piqproject.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;
import piq.piqproject.domain.users.enums.SocialType;

@Getter
@Builder
public class SocialUserInfo {
    private String socialId;
    private String email;
    private SocialType socialType;
}
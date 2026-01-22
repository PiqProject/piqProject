package piq.piqproject.domain.auth.service.social;

import piq.piqproject.domain.auth.dto.SocialUserInfo;

public interface SocialLoadStrategy {
    SocialUserInfo getUserInfo(String token);
}
package piq.piqproject.config.kakao;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "kakao")
public class KakaoProperties {

    /**
     * 카카오 REST API 키 (= 클라이언트 ID)
     */
    private String clientId;

    /**
     * 카카오 OAuth redirect URI (프론트엔드와 동일해야 함)
     */
    private String redirectUri;

    /**
     * 카카오 client secret key (= 없으면 access token을 못받음)
     */
    private String clientSecret;
}
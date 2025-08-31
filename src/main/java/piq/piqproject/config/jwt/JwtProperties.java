package piq.piqproject.config.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component // spring bean으로 만듦
@ConfigurationProperties("jwt") // application.properties에서 jwt접두사 속성을 class의 필드에 매핑
public class JwtProperties {
    private String issuer;
    private String secretKey;
    private long expiration;
}

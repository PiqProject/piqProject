package piq.piqproject.config.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties("jwt") // application.properties의 jwt접두사를 가진 속성들을 class의 필드에 매핑시킴
public class JwtProperties {
    private String issuer;
    private String secretKey;
    private long expiration;
    private long refreshExpiration;
}

package piq.piqproject.domain.users.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
// value: Redis의 Key Prefix, timeToLive: 만료 시간(초 단위). 7일 = 604800초
// @RedisHash가 붙은 Java 객체(RefreshToken)를 통째로 Hash 자료구조에 저장하고 조회하는 기능을 제공
@RedisHash(value = "refreshToken", timeToLive = 604800)
public class RefreshTokenEntity {

    // Redis에서 userEmail을 key로 사용
    @Id
    private String userEmail;

    // Value가 될 Refresh Token 값
    private String refreshToken;
}
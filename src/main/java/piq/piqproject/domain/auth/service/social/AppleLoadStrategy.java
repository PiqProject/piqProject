package piq.piqproject.domain.auth.service.social;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import piq.piqproject.common.error.exception.CustomException;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.domain.auth.dto.response.ApplePublicKeyResponse;
import piq.piqproject.domain.auth.dto.SocialUserInfo;
import piq.piqproject.domain.users.enums.SocialType;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppleLoadStrategy implements SocialLoadStrategy {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper; // JWT 헤더 파싱용

    private static final String APPLE_PUBLIC_KEYS_URL = "https://appleid.apple.com/auth/keys";

    // 애플에서주는 jwt(애플측 비밀키로 암호화)를 공개키로 복호화하여 헤더와 페이로드 비교하여 사용자 정보 추출 (비대칭 양방향 암호화 방식)
    @Override
    public SocialUserInfo getUserInfo(String identityToken) {
        try {
            // 1. 애플 공개키 목록 가져오기
            ApplePublicKeyResponse response = restTemplate.getForObject(APPLE_PUBLIC_KEYS_URL,
                    ApplePublicKeyResponse.class);
            if (response == null) {
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "애플 공개키 조회 실패");
            }

            // 2. Identity Token의 헤더에서 kid(Key ID) 추출
            String headerOfIdentityToken = identityToken.substring(0, identityToken.indexOf("."));
            Map<String, String> header = objectMapper.readValue(
                    new String(Base64.getUrlDecoder().decode(headerOfIdentityToken), StandardCharsets.UTF_8),
                    Map.class);
            String kid = header.get("kid");

            // 3. 공개키 목록 중 kid가 일치하는 키 찾기
            ApplePublicKeyResponse.AppleKey appleKey = response.getKeys().stream()
                    .filter(key -> key.getKid().equals(kid))
                    .findFirst()
                    .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "일치하는 애플 공개키가 없습니다."));

            // 4. RSA 공개키 생성
            byte[] nBytes = Base64.getUrlDecoder().decode(appleKey.getN());
            byte[] eBytes = Base64.getUrlDecoder().decode(appleKey.getE());

            BigInteger n = new BigInteger(1, nBytes);
            BigInteger e = new BigInteger(1, eBytes);

            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(n, e);
            KeyFactory keyFactory = KeyFactory.getInstance(appleKey.getKty());
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            // 5. JWT 서명 검증 및 Claims 추출
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(identityToken)
                    .getBody();

            // 6. 결과 반환
            // sub: 애플의 고유 식별자 (User ID)
            // email: 이메일 (애플은 이메일 가리기를 하면 랜덤 이메일을 줌)
            String socialId = claims.getSubject();
            String email = claims.get("email", String.class);

            return SocialUserInfo.builder()
                    .socialId(socialId)
                    .email(email) // email은 null일 수도 있음 (두 번째 로그인부터는 안 줄 수도 있음)
                    .socialType(SocialType.APPLE)
                    .build();
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Apple Token 파싱 오류");
        } catch (Exception e) {
            log.error("Apple Login Error: ", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Apple Identity Token 검증 실패");
        }
    }
}
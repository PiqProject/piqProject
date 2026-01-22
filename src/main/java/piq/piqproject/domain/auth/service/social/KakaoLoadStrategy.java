package piq.piqproject.domain.auth.service.social;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import piq.piqproject.common.error.exception.CustomException;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.domain.auth.dto.SocialUserInfo;
import piq.piqproject.domain.users.enums.SocialType;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoLoadStrategy implements SocialLoadStrategy {

    private final RestTemplate restTemplate;

    // 받은 accessToken은 아무의미없는 난수 문자열 -> kakao api에서 사용자 정보를 가져오기 위해 보내줘야함 -> 맞으면 정보를
    // 그쪽에서 쏴줌
    @Override
    public SocialUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://kapi.kakao.com/v2/user/me",
                    HttpMethod.GET,
                    requestEntity,
                    Map.class);

            Map<String, Object> body = response.getBody();
            String socialId = String.valueOf(body.get("id"));

            Map<String, Object> account = (Map<String, Object>) body.get("kakao_account");
            String email = null;
            if (account != null && account.containsKey("email")) {
                email = (String) account.get("email");
            }

            return SocialUserInfo.builder()
                    .socialId(socialId)
                    .email(email)
                    .socialType(SocialType.KAKAO)
                    .build();

        } catch (HttpClientErrorException e) {
            log.error("Kakao API Error: Status Code = {}, Body = {}", e.getStatusCode(), e.getResponseBodyAsString());

            // 401 Unauthorized면 토큰 문제임
            if (e.getStatusCode().value() == 401) {
                throw new CustomException(ErrorCode.BAD_REQUEST, "카카오 액세스 토큰이 만료되었거나 유효하지 않습니다.");
            }
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "카카오 API 연동 에러: " + e.getResponseBodyAsString());

        } catch (Exception e) {
            log.error("Kakao Internal Error: ", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "카카오 로그인 처리 중 알 수 없는 오류 발생");
        }
    }
}
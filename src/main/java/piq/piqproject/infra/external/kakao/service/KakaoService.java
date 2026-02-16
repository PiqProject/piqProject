package piq.piqproject.infra.external.kakao.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.error.exception.CustomException;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.config.kakao.KakaoProperties;
import piq.piqproject.infra.external.kakao.dto.KakaoTokenResponse;
import piq.piqproject.infra.external.kakao.dto.KakaoUserInfoResponse;

/**
 * 카카오 OAuth2 인가 코드(Authorization Code) 방식을 처리하는 서비스
 * 
 * <p>
 * 흐름:<br>
 * 1. 프론트엔드에서 받은 인가 코드(code)로 카카오 인증 서버에 토큰 발급 요청<br>
 * 2. 발급받은 Access Token으로 카카오 API 서버에 사용자 정보 조회<br>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoService {

    private final RestClient restClient;
    private final KakaoProperties kakaoProperties;

    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    @Value("${kakao.admin-key}")
    private String adminKey;

    /**
     * 1단계: 인가 코드로 카카오 Access Token 발급
     *
     * @param code 프론트엔드에서 전달받은 카카오 인가 코드
     * @return KakaoTokenResponse (access_token 포함)
     */
    public KakaoTokenResponse getToken(String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", kakaoProperties.getClientId());
        formData.add("redirect_uri", kakaoProperties.getRedirectUri());
        formData.add("code", code);
        formData.add("client_secret", kakaoProperties.getClientSecret());

        try {
            KakaoTokenResponse response = restClient.post()
                    .uri(KAKAO_TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(KakaoTokenResponse.class);

            if (response == null || response.accessToken() == null) {
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR,
                        "카카오 토큰 발급 응답이 비어 있습니다.");
            }

            log.debug("카카오 토큰 발급 성공");
            return response;

        } catch (RestClientResponseException e) {
            log.error("카카오 토큰 발급 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());

            if (e.getStatusCode().value() == 400) {
                throw new CustomException(ErrorCode.BAD_REQUEST,
                        "카카오 인가 코드가 만료되었거나 유효하지 않습니다.");
            }
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "카카오 토큰 발급 중 오류가 발생했습니다: " + e.getResponseBodyAsString());

        } catch (Exception e) {
            log.error("카카오 토큰 발급 중 알 수 없는 오류: ", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "카카오 토큰 발급 중 알 수 없는 오류가 발생했습니다.");
        }
    }

    /**
     * 2단계: Access Token으로 카카오 사용자 정보 조회
     *
     * @param accessToken 카카오 Access Token
     * @return KakaoUserInfoResponse (사용자 id, email, nickname 등)
     */
    public KakaoUserInfoResponse getUserInfo(String accessToken) {
        try {
            KakaoUserInfoResponse response = restClient.get()
                    .uri(KAKAO_USER_INFO_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-type", "application/x-www-form-urlencoded;charset=utf-8")
                    .retrieve()
                    .body(KakaoUserInfoResponse.class);

            if (response == null || response.id() == null) {
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR,
                        "카카오 사용자 정보 응답이 비어 있습니다.");
            }

            log.debug("카카오 사용자 정보 조회 성공: kakaoId={}", response.id());
            return response;

        } catch (RestClientResponseException e) {
            log.error("카카오 사용자 정보 조회 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());

            if (e.getStatusCode().value() == 401) {
                throw new CustomException(ErrorCode.BAD_REQUEST,
                        "카카오 액세스 토큰이 만료되었거나 유효하지 않습니다.");
            }
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "카카오 사용자 정보 조회 중 오류가 발생했습니다: " + e.getResponseBodyAsString());

        } catch (Exception e) {
            log.error("카카오 사용자 정보 조회 중 알 수 없는 오류: ", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "카카오 사용자 정보 조회 중 알 수 없는 오류가 발생했습니다.");
        }
    }

    /**
     * 카카오 연결 끊기 (Unlink)
     * 유저의 고유번호(socialId)를 사용하여 카카오와의 연결을 강제로 끊습니다.
     */
    public void unlink(String socialId) {
        // 1. 파라미터 구성
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("target_id_type", "user_id");
        params.add("target_id", socialId);

        try {
            // 2. RestClient 요청 수행
            restClient.post()
                    .uri("https://kapi.kakao.com/v1/user/unlink")
                    .header("Authorization", "KakaoAK " + adminKey)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(params) // MultiValueMap 전달
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        // 에러 발생 시 로그를 상세히 남기기 위해 핸들링 가능
                        log.error("카카오 API 에러 발생: {}", response.getStatusCode());
                    })
                    .toBodilessEntity(); // 응답 바디가 필요 없을 때 사용

            log.info("카카오 연결 끊기 성공: socialId={}", socialId);
        } catch (Exception e) {
            log.error("카카오 연결 끊기 실패: socialId={}, Error: {}", socialId, e.getMessage());
        }
    }

}

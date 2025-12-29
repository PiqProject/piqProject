package piq.piqproject.infra.external.kakao.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import piq.piqproject.common.dto.CoordinateDto;
import piq.piqproject.infra.external.kakao.dto.KakaoGeoResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoGeocodingService {

    @Value("${kakao.api.key}")
    private String kakaoApiKey;

    private static final String KAKAO_API_URL = "https://dapi.kakao.com/v2/local/search/address.json";

    /**
     * 주소를 입력받아 위도/경도 좌표를 반환합니다.
     * 
     * @param address 검색할 주소 (예: "서울시 강남구 역삼동")
     * @return 좌표 정보 (CoordinateDto), 실패하거나 결과가 없으면 null 반환
     */
    public CoordinateDto getCoordinate(String address) {
        if (address == null || address.trim().isEmpty()) {
            return null;
        }

        try {
            WebClient webClient = WebClient.builder()
                    .baseUrl(KAKAO_API_URL)
                    .defaultHeader("Authorization", "KakaoAK " + kakaoApiKey.trim()) // KakaoAK 뒤에 공백 필수
                    .build();

            // 1. API 호출 (비동기 방식인 WebClient를 동기처럼 block()으로 사용)
            KakaoGeoResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("query", address)
                            .build())
                    .retrieve()
                    .bodyToMono(KakaoGeoResponse.class) // DTO로 바로 매핑
                    .block();

            // 2. 결과 파싱 및 변환
            return convertToCoordinate(response, address);

        } catch (Exception e) {
            log.error("Kakao Geocoding API 호출 실패. 주소: {}", address, e);
            return null;
        }
    }

    private CoordinateDto convertToCoordinate(KakaoGeoResponse response, String address) {
        if (response == null || response.getDocuments() == null || response.getDocuments().isEmpty()) {
            log.warn("주소에 대한 검색 결과가 없습니다: {}", address);
            return null;
        }

        // 정확도가 가장 높은 첫 번째 결과를 사용합니다.
        KakaoGeoResponse.Document document = response.getDocuments().get(0);

        try {
            double lat = Double.parseDouble(document.getLatitude()); // y
            double lon = Double.parseDouble(document.getLongitude()); // x
            return new CoordinateDto(lat, lon);
        } catch (NumberFormatException e) {
            log.error("좌표 변환 오류 (숫자 형식이 아님). Lat: {}, Lon: {}", document.getLatitude(), document.getLongitude());
            return null;
        }
    }
}
package piq.piqproject.infra.external.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * 카카오 로컬 API의 응답을 매핑하는 DTO 클래스입니다.
 * 구조: { "documents": [ { "x": "...", "y": "..." }, ... ], "meta": { ... } }
 */
@Getter
@NoArgsConstructor
@ToString
public class KakaoGeoResponse {

    private List<Document> documents;

    @Getter
    @NoArgsConstructor
    @ToString
    public static class Document {

        // 카카오 API에서 x는 경도(longitude), y는 위도(latitude)를 의미합니다.
        // JSON 필드명이 x, y이므로 그대로 매핑합니다.
        @JsonProperty("x")
        private String longitude;

        @JsonProperty("y")
        private String latitude;
    }
}
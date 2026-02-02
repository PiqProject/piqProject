package piq.piqproject.domain.ads.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Google AdMob SSV 콜백 요청 DTO
 * Google에서 전송하는 SSV 콜백의 쿼리 파라미터를 매핑합니다.
 * 
 * 참고: https://developers.google.com/admob/android/rewarded-video-ssv
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdSsvCallbackRequest {

    /**
     * 광고 네트워크 ID
     */
    private String adNetwork;

    /**
     * AdMob 광고 단위 ID
     */
    private String adUnit;

    /**
     * 보상 금액
     */
    private String rewardAmount;

    /**
     * 보상 아이템 타입
     */
    private String rewardItem;

    /**
     * SSV 타임스탬프 (Unix timestamp in milliseconds)
     */
    private String timestamp;

    /**
     * 고유 거래 ID (중복 방지용)
     */
    private String transactionId;

    /**
     * 사용자 ID (앱에서 설정한 값)
     */
    private String userId;

    /**
     * 클라이언트에서 전달한 커스텀 데이터
     */
    private String customData;

    /**
     * Google의 서명 (검증용)
     */
    private String signature;

    /**
     * 서명에 사용된 키 ID
     */
    private String keyId;
}

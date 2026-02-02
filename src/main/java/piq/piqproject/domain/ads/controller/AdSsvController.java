package piq.piqproject.domain.ads.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import piq.piqproject.domain.ads.dto.AdSsvCallbackRequest;
import piq.piqproject.domain.ads.service.AdSsvService;

/**
 * Google AdMob SSV 콜백 컨트롤러
 * 
 * Google에서 전송하는 SSV 콜백을 수신하고 처리합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/ads/ssv")
@RequiredArgsConstructor
public class AdSsvController {

    private final AdSsvService adSsvService;

    /**
     * Google AdMob SSV 콜백 엔드포인트
     * 
     * Google에서 광고 시청 완료 시 이 엔드포인트로 콜백을 전송합니다.
     * 
     * @param adNetwork     광고 네트워크 ID
     * @param adUnit        AdMob 광고 단위 ID
     * @param rewardAmount  보상 금액
     * @param rewardItem    보상 아이템 타입
     * @param timestamp     SSV 타임스탬프
     * @param transactionId 고유 거래 ID
     * @param userId        사용자 ID
     * @param customData    커스텀 데이터
     * @param signature     Google 서명
     * @param keyId         서명 키 ID
     * @return HTTP 200 OK (성공) 또는 에러 응답
     */
    @GetMapping("/callback")
    public ResponseEntity<String> handleSsvCallback(
            @RequestParam("ad_network") String adNetwork,
            @RequestParam("ad_unit") String adUnit,
            @RequestParam("reward_amount") String rewardAmount,
            @RequestParam("reward_item") String rewardItem,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("transaction_id") String transactionId,
            @RequestParam("user_id") String userId,
            @RequestParam(value = "custom_data", required = false) String customData,
            @RequestParam("signature") String signature,
            @RequestParam("key_id") String keyId) {

        log.info("[SSV Callback] Received - transactionId: {}, userId: {}, adUnit: {}",
                transactionId, userId, adUnit);

        try {
            // DTO 생성
            AdSsvCallbackRequest request = AdSsvCallbackRequest.builder()
                    .adNetwork(adNetwork)
                    .adUnit(adUnit)
                    .rewardAmount(rewardAmount)
                    .rewardItem(rewardItem)
                    .timestamp(timestamp)
                    .transactionId(transactionId)
                    .userId(userId)
                    .customData(customData)
                    .signature(signature)
                    .keyId(keyId)
                    .build();

            // SSV 검증 및 보상 처리
            adSsvService.processAdReward(request);

            log.info("[SSV Callback] Success - transactionId: {}, userId: {}", transactionId, userId);
            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("[SSV Callback] Error - transactionId: {}, userId: {}", transactionId, userId, e);
            // Google은 200 OK가 아닌 응답을 받으면 재시도하므로, 에러 상황에서도 적절한 처리 필요
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: " + e.getMessage());
        }
    }
}

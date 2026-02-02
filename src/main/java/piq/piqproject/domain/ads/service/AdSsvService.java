package piq.piqproject.domain.ads.service;

import com.google.api.client.googleapis.auth.oauth2.GooglePublicKeysManager;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.InvalidRequestException;
import piq.piqproject.domain.ads.dto.AdSsvCallbackRequest;
import piq.piqproject.domain.ads.entity.AdViewHistoryEntity;
import piq.piqproject.domain.ads.repository.AdViewHistoryRepository;
import piq.piqproject.domain.points.enums.PointType;
import piq.piqproject.domain.points.service.PointService;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.repository.UserRepository;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

/**
 * Google AdMob SSV (Server-Side Verification) 검증 서비스
 * 
 * Google에서 전송하는 SSV 콜백을 검증하고 사용자에게 포인트를 지급합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdSsvService {

    private final AdViewHistoryRepository adViewHistoryRepository;
    private final UserRepository userRepository;
    private final PointService pointService;

    // Google Public Keys Manager (SSV 서명 검증용)
    private static final String GOOGLE_PUBLIC_KEYS_URL = "https://www.gstatic.com/admob/reward/verifier-keys.json";

    private final GooglePublicKeysManager publicKeysManager = new GooglePublicKeysManager.Builder(
            new NetHttpTransport(),
            new GsonFactory()).setPublicCertsEncodedUrl(GOOGLE_PUBLIC_KEYS_URL).build();

    /**
     * SSV 콜백 처리 메인 로직
     * 
     * @param request SSV 콜백 요청 데이터
     * @throws InvalidRequestException 검증 실패 시
     */
    @Transactional
    public void processAdReward(AdSsvCallbackRequest request) {
        log.info("[SSV] Processing ad reward callback - transactionId: {}, userId: {}",
                request.getTransactionId(), request.getUserId());

        // 1. 중복 검증
        validateDuplicateReward(request.getTransactionId());

        // 2. SSV 서명 검증
        validateSsvSignature(request);

        // 3. 사용자 조회
        Long userId = parseUserId(request.getUserId());
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidRequestException(ErrorCode.USER_NOT_FOUND,
                        "사용자를 찾을 수 없습니다. userId: " + userId));

        // 4. 포인트 지급
        int rewardAmount = parseRewardAmount(request.getRewardAmount());
        pointService.chargePoints(user, rewardAmount, PointType.REWARD,
                "광고 시청 보상 (AdUnit: " + request.getAdUnit() + ")");

        // 5. 광고 시청 이력 저장
        saveAdViewHistory(request, user, rewardAmount, true);

        log.info("[SSV] Ad reward processed successfully - userId: {}, amount: {}, transactionId: {}",
                userId, rewardAmount, request.getTransactionId());
    }

    /**
     * 중복 보상 검증
     * 
     * @param transactionId Google SSV transaction ID
     * @throws InvalidRequestException 이미 처리된 보상인 경우
     */
    private void validateDuplicateReward(String transactionId) {
        if (adViewHistoryRepository.existsByRewardId(transactionId)) {
            log.warn("[SSV] Duplicate reward attempt detected - transactionId: {}", transactionId);
            throw new InvalidRequestException(ErrorCode.DUPLICATE_REQUEST,
                    "이미 처리된 광고 보상입니다. transactionId: " + transactionId);
        }
    }

    /**
     * Google SSV 서명 검증
     * 
     * Google의 공개 키를 사용하여 SSV 콜백의 서명을 검증합니다.
     * 
     * @param request SSV 콜백 요청 데이터
     * @throws InvalidRequestException 서명 검증 실패 시
     */
    private void validateSsvSignature(AdSsvCallbackRequest request) {
        try {
            // 서명 검증을 위한 원본 메시지 구성
            String message = buildVerificationMessage(request);

            // Base64 디코딩된 서명
            byte[] signatureBytes = Base64.getUrlDecoder().decode(request.getSignature());

            // Google 공개 키 목록 가져오기
            publicKeysManager.refresh(); // 최신 키 목록으로 갱신
            java.util.List<PublicKey> publicKeys = publicKeysManager.getPublicKeys();

            // 모든 공개 키로 서명 검증 시도
            boolean isValid = false;
            for (PublicKey publicKey : publicKeys) {
                try {
                    Signature sig = Signature.getInstance("SHA256withECDSA");
                    sig.initVerify(publicKey);
                    sig.update(message.getBytes());

                    if (sig.verify(signatureBytes)) {
                        isValid = true;
                        break;
                    }
                } catch (Exception e) {
                    // 이 키로는 검증 실패, 다음 키 시도
                    continue;
                }
            }

            if (!isValid) {
                log.warn("[SSV] Signature verification failed - transactionId: {}", request.getTransactionId());
                throw new InvalidRequestException(ErrorCode.INVALID_SIGNATURE,
                        "SSV 서명 검증에 실패했습니다.");
            }

            log.debug("[SSV] Signature verified successfully - transactionId: {}", request.getTransactionId());

        } catch (GeneralSecurityException | IOException e) {
            log.error("[SSV] Signature verification error - transactionId: {}", request.getTransactionId(), e);
            throw new InvalidRequestException(ErrorCode.INVALID_SIGNATURE,
                    "SSV 서명 검증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * SSV 서명 검증을 위한 메시지 구성
     * 
     * Google SSV 문서에 따라 쿼리 파라미터를 정렬하여 메시지를 구성합니다.
     * 
     * @param request SSV 콜백 요청 데이터
     * @return 검증용 메시지 문자열
     */
    private String buildVerificationMessage(AdSsvCallbackRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("ad_network=").append(request.getAdNetwork());
        sb.append("&ad_unit=").append(request.getAdUnit());

        if (request.getCustomData() != null && !request.getCustomData().isEmpty()) {
            sb.append("&custom_data=").append(request.getCustomData());
        }

        sb.append("&reward_amount=").append(request.getRewardAmount());
        sb.append("&reward_item=").append(request.getRewardItem());
        sb.append("&timestamp=").append(request.getTimestamp());
        sb.append("&transaction_id=").append(request.getTransactionId());
        sb.append("&user_id=").append(request.getUserId());

        return sb.toString();
    }

    /**
     * 광고 시청 이력 저장
     * 
     * @param request      SSV 콜백 요청 데이터
     * @param user         사용자 엔티티
     * @param rewardAmount 지급된 포인트 금액
     * @param verified     검증 완료 여부
     */
    private void saveAdViewHistory(AdSsvCallbackRequest request, UserEntity user,
            int rewardAmount, boolean verified) {
        AdViewHistoryEntity history = AdViewHistoryEntity.builder()
                .user(user)
                .rewardId(request.getTransactionId())
                .adUnitId(request.getAdUnit())
                .adNetwork(request.getAdNetwork())
                .rewardAmount(rewardAmount)
                .rewardItem(request.getRewardItem())
                .customData(request.getCustomData())
                .verified(verified)
                .ssvTimestamp(Long.parseLong(request.getTimestamp()))
                .build();

        adViewHistoryRepository.save(history);
        log.debug("[SSV] Ad view history saved - transactionId: {}", request.getTransactionId());
    }

    /**
     * userId 문자열을 Long으로 파싱
     * 
     * @param userIdStr 사용자 ID 문자열
     * @return 파싱된 사용자 ID
     * @throws InvalidRequestException 파싱 실패 시
     */
    private Long parseUserId(String userIdStr) {
        try {
            return Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            throw new InvalidRequestException(ErrorCode.BAD_REQUEST,
                    "유효하지 않은 사용자 ID 형식입니다: " + userIdStr);
        }
    }

    /**
     * rewardAmount 문자열을 int로 파싱
     * 
     * @param rewardAmountStr 보상 금액 문자열
     * @return 파싱된 보상 금액
     * @throws InvalidRequestException 파싱 실패 시
     */
    private int parseRewardAmount(String rewardAmountStr) {
        try {
            return Integer.parseInt(rewardAmountStr);
        } catch (NumberFormatException e) {
            throw new InvalidRequestException(ErrorCode.BAD_REQUEST,
                    "유효하지 않은 보상 금액 형식입니다: " + rewardAmountStr);
        }
    }
}

package piq.piqproject.domain.ads.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import piq.piqproject.domain.ads.entity.AdViewHistoryEntity;

import java.util.List;
import java.util.Optional;

/**
 * 광고 시청 이력 Repository
 */
public interface AdViewHistoryRepository extends JpaRepository<AdViewHistoryEntity, Long> {

    /**
     * rewardId로 이미 처리된 보상인지 확인 (중복 방지)
     * 
     * @param rewardId Google SSV transaction_id
     * @return 존재 여부
     */
    boolean existsByRewardId(String rewardId);

    /**
     * rewardId로 광고 시청 이력 조회
     * 
     * @param rewardId Google SSV transaction_id
     * @return 광고 시청 이력
     */
    Optional<AdViewHistoryEntity> findByRewardId(String rewardId);

    /**
     * 사용자별 광고 시청 이력 조회
     * 
     * @param userId 사용자 ID
     * @return 광고 시청 이력 목록
     */
    List<AdViewHistoryEntity> findByUserId(Long userId);

    /**
     * 검증 완료된 광고 시청 이력만 조회
     * 
     * @param userId   사용자 ID
     * @param verified 검증 여부
     * @return 광고 시청 이력 목록
     */
    List<AdViewHistoryEntity> findByUserIdAndVerified(Long userId, boolean verified);
}

package piq.piqproject.domain.ads.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import piq.piqproject.domain.BaseEntity;
import piq.piqproject.domain.users.entity.UserEntity;

/**
 * 광고 시청 이력 엔티티
 * Google AdMob SSV 콜백을 통해 검증된 광고 시청 기록을 저장합니다.
 * 중복 보상 방지를 위해 rewardId를 unique 제약조건으로 설정합니다.
 */
@Entity
@Table(name = "ad_view_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AdViewHistoryEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 광고를 시청한 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /**
     * Google SSV에서 제공하는 고유 보상 ID (transaction_id)
     * 중복 보상 방지를 위한 unique 제약조건
     */
    @Column(nullable = false, unique = true, length = 255)
    private String rewardId;

    /**
     * AdMob 광고 단위 ID
     */
    @Column(nullable = false, length = 255)
    private String adUnitId;

    /**
     * 광고 네트워크 ID
     */
    @Column(nullable = false, length = 100)
    private String adNetwork;

    /**
     * 지급된 포인트 금액
     */
    @Column(nullable = false)
    private int rewardAmount;

    /**
     * 보상 아이템 타입 (예: coins, points 등)
     */
    @Column(nullable = false, length = 50)
    private String rewardItem;

    /**
     * 클라이언트에서 전달한 커스텀 데이터
     * 추가 검증이나 로깅에 사용
     */
    @Column(length = 500)
    private String customData;

    /**
     * SSV 검증 완료 여부
     */
    @Column(nullable = false)
    private boolean verified;

    /**
     * Google SSV 타임스탬프
     */
    @Column(nullable = false)
    private Long ssvTimestamp;

    @Builder
    public AdViewHistoryEntity(UserEntity user, String rewardId, String adUnitId, String adNetwork,
            int rewardAmount, String rewardItem, String customData,
            boolean verified, Long ssvTimestamp) {
        this.user = user;
        this.rewardId = rewardId;
        this.adUnitId = adUnitId;
        this.adNetwork = adNetwork;
        this.rewardAmount = rewardAmount;
        this.rewardItem = rewardItem;
        this.customData = customData;
        this.verified = verified;
        this.ssvTimestamp = ssvTimestamp;
    }

    /**
     * 검증 완료 처리
     */
    public void markAsVerified() {
        this.verified = true;
    }
}

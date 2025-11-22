package piq.piqproject.domain.recommendations.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import piq.piqproject.domain.BaseEntity;
import piq.piqproject.domain.users.entity.UserEntity;

@Entity
@Table(name = "daily_recommendations", indexes = {
        // "특정 사용자의 특정 날짜 추천 목록 조회" 쿼리 성능을 위한 인덱스
        @Index(name = "idx_recommendation_user_created_at", columnList = "user_id, createdAt")
})
@Getter
@NoArgsConstructor
public class DailyRecommendationEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 추천을 받는 사용자 (recommendingUser)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    // 추천된 사용자 (recommendedUser)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recommended_user_id", nullable = false)
    private UserEntity recommendedUser;

    @Column(name = "is_actioned", nullable = false)
    private boolean actioned = false;

    @Builder
    public DailyRecommendationEntity(UserEntity user, UserEntity recommendedUser) {
        this.user = user;
        this.recommendedUser = recommendedUser;
    }

    public void markAsActioned() {
        this.actioned = true;
    }
}
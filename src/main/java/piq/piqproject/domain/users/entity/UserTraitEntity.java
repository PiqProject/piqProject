package piq.piqproject.domain.users.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import piq.piqproject.domain.traits.entity.TraitOptionEntity; // 수정된 TraitOptionEntity 참조

/**
 * 사용자가 보유한 '실제 특성'을 나타내는 엔티티입니다.
 * User와 TraitOption 간의 다대다(N:M) 관계를 해소하는 연결 테이블 역할을 합니다.
 */
@Entity
@Table(name = "user_traits")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTraitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 이 특성을 보유한 사용자입니다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /**
     * 사용자가 보유한 구체적인 특성 옵션입니다.
     * (예: 'ENFP', '비흡연' 등)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private TraitOptionEntity traitOption;

    /**
     * Builder 패턴을 사용하여 객체를 생성합니다.
     * 
     * @param user        특성을 보유한 사용자
     * @param traitOption 사용자가 보유한 특성 옵션
     */
    @Builder
    private UserTraitEntity(UserEntity user, TraitOptionEntity traitOption) {
        this.user = user;
        this.traitOption = traitOption;
    }

    /**
     * 정적 팩토리 메서드를 사용하여 객체를 생성합니다.
     * 
     * @param user        특성을 보유한 사용자
     * @param traitOption 사용자가 보유한 특성 옵션
     * @return 생성된 UserTraitEntity 객체
     */
    public static UserTraitEntity of(UserEntity user, TraitOptionEntity traitOption) {
        return UserTraitEntity.builder()
                .user(user)
                .traitOption(traitOption)
                .build();
    }
}
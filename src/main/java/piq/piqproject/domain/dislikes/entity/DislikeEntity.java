package piq.piqproject.domain.dislikes.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import piq.piqproject.domain.BaseEntity; // BaseEntity 상속
import piq.piqproject.domain.users.entity.UserEntity;

/**
 * 사용자 간의 '싫어요' 관계를 기록하는 엔티티입니다.
 */
@Entity
@Table(name = "dislikes",
        // 데이터 무결성을 위한 Unique 제약 조건:
        // (from_user_id, to_user_id) 조합이 유일하도록 설정하여
        // 한 사용자가 다른 사용자를 중복해서 '싫어요' 하는 것을 DB 레벨에서 원천 차단합니다.
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_dislike_from_to", columnNames = { "from_user_id", "to_user_id" })
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DislikeEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * '싫어요' 행동을 한 사용자 (행위의 주체)
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_user_id", nullable = false)
    private UserEntity fromUser;

    /**
     * '싫어요'를 받은 사용자 (행위의 대상)
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_user_id", nullable = false)
    private UserEntity toUser;

    /**
     * Builder 패턴을 사용하여 DislikeEntity 객체를 생성합니다.
     * 
     * @param fromUser '싫어요'를 누른 사용자
     * @param toUser   '싫어요'를 받은 사용자
     */
    @Builder
    public DislikeEntity(UserEntity fromUser, UserEntity toUser) {
        this.fromUser = fromUser;
        this.toUser = toUser;
    }
}
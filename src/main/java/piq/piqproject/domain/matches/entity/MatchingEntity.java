package piq.piqproject.domain.matches.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import piq.piqproject.domain.BaseEntity;
import piq.piqproject.domain.matches.enums.MatchingStatus;
import piq.piqproject.domain.users.entity.UserEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "matching") // 실제 테이블명으로 지정
public class MatchingEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_id") // DB 컬럼명에 맞게 수정
    private Long matchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false) // FK 컬럼명
    private UserEntity sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false) // FK 컬럼명
    private UserEntity receiver;

    @Enumerated(EnumType.STRING)
    @Column(name = "is_succeeded", nullable = false)
    private MatchingStatus status;

    @Builder
    public MatchingEntity(UserEntity sender, UserEntity receiver, MatchingStatus status) {
        this.sender = sender;
        this.receiver = receiver;
        this.status = status;
    }

    // 매칭 상태를 변경하는 편의 메서드
    public void changeStatus(MatchingStatus status) {
        this.status = status;
    }
}
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

    // 사용한 포인트 정보 저장 (환불 시 필요)
    // 매칭 요청 시점의 비용을 기록해둬야 나중에 가격 정책이 바뀌어도 정확히 환불 가능
    @Column(nullable = false)
    private int senderUsedPoints;

    @Column(nullable = false)
    private int receiverUsedPoints;

    @Column(length = 150)
    private String message;

    @Builder
    public MatchingEntity(UserEntity sender, UserEntity receiver, MatchingStatus status, int senderUsedPoints,
            int receiverUsedPoints, String message) {
        this.sender = sender;
        this.receiver = receiver;
        this.status = status;
        this.senderUsedPoints = senderUsedPoints;
        this.receiverUsedPoints = receiverUsedPoints;
        this.message = message;
    }

    // 매칭 상태를 변경하는 편의 메서드
    public void changeStatus(MatchingStatus status) {
        this.status = status;
    }
}
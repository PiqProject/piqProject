package piq.piqproject.domain.points.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import piq.piqproject.domain.BaseEntity;
import piq.piqproject.domain.points.enums.PointType;
import piq.piqproject.domain.users.entity.UserEntity;

@Entity
@Table(name = "point_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PointHistoryEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointType type; // 충전/사용/환불 등

    @Column(nullable = false)
    private int amount; // 변동 금액 (+100, -50 등)

    @Column(nullable = false)
    private int balanceSnapshot; // 변동 후 잔액

    @Column(nullable = false)
    private String description; // 상세 사유

    @Builder
    public PointHistoryEntity(UserEntity user, PointType type, int amount, int balanceSnapshot, String description) {
        this.user = user;
        this.type = type;
        this.amount = amount;
        this.balanceSnapshot = balanceSnapshot;
        this.description = description;
    }
}
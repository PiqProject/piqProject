package piq.piqproject.domain.payments.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import piq.piqproject.domain.BaseEntity;
import piq.piqproject.domain.payments.enums.PaymentStatus;
import piq.piqproject.domain.products.entity.ProductEntity;
import piq.piqproject.domain.users.entity.UserEntity; // 사용자 엔티티

import java.math.BigDecimal;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 무분별한 객체 생성을 막기 위해 접근 수준을 PROTECTED로 설정
public class PaymentEntity extends BaseEntity {

    // 내부 관리용 ID (Primary Key)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 서버에서 사용할 주문번호 (UUID로 생성)
    // 클라이언트, 포트원 서버와 통신할 때 사용하는 공개적인 주문번호
    @Column(nullable = false, unique = true)
    private String merchantUid;

    /**
     * 포트원 거래고유번호 (imp_uid)
     * - 결제가 '완료'된 후에 포트원에서 발급하는 고유 ID입니다.
     * - 결제 취소 API 등을 호출할 때 필요하며, 최종 거래를 식별하는 중요한 정보입니다.
     * - 결제 준비(READY) 단계에서는 null 상태입니다.
     */
    @Column(unique = true) // imp_uid 역시 고유해야 합니다.
    private String impUid;

    /**
     * 결제 금액
     * - 돈과 관련된 데이터는 부동소수점 오류를 피하기 위해 BigDecimal 타입을 사용하는 것이 가장 안전합니다.
     */
    @Column(nullable = false)
    private BigDecimal amount;

    /**
     * 결제 상태
     * - Enum 타입을 사용하여 결제 상태(READY, COMPLETED, FAILED, CANCELLED)를 관리합니다.
     * - @Enumerated(EnumType.STRING)을 사용하여 DB에는 Enum의 이름(문자열)이 저장되도록 합니다.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    /**
     * 연관관계 매핑: 사용자 (N:1)
     * - 하나의 사용자는 여러 번의 결제를 할 수 있습니다. (User 입장에서 OneToMany)
     * - Payment 입장에서는 하나의 결제는 한 명의 사용자에 의해 이루어집니다. (Payment 입장에서 ManyToOne)
     * - @ManyToOne(fetch = FetchType.LAZY)를 사용하여, 실제로 user 객체가 필요할 때만 DB에서 조회하도록
     * 설정합니다. (성능 최적화)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // DB에 생성될 외래 키의 컬럼명을 지정합니다.
    private UserEntity user;

    /**
     * 연관관계 매핑: 상품 (N:1)
     * - 하나의 상품은 여러 번 결제될 수 있습니다. (Shop 입장에서 OneToMany)
     * - Payment 입장에서는 하나의 결제는 하나의 상품에 대해 이루어집니다. (Payment 입장에서 ManyToOne)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id")
    private ProductEntity product;

    // === 빌더 패턴 ===
    @Builder
    public PaymentEntity(String merchantUid, String impUid, BigDecimal amount, PaymentStatus status, UserEntity user,
            ProductEntity product) {
        this.merchantUid = merchantUid;
        this.impUid = impUid;
        this.amount = amount;
        this.status = status;
        this.user = user;
        this.product = product;
    }

    // === 비즈니스 로직 (상태 변경 등) ===

    /**
     * 최종 결제 완료 처리를 위한 메소드
     * - 사후 검증이 모두 완료되었을 때 호출합니다.
     * - impUid를 기록하고, 상태를 'PAID'로 변경합니다.
     * 
     * @param impUid 포트원에서 발급받은 거래 고유번호
     */
    public void completePayment(String impUid) {
        this.impUid = impUid;
        this.status = PaymentStatus.PAID;
        // TODO: 거래 완료에 따른 사용자에게 pq포인트 제공 등의 비지니스로직 추가 예정
    }

    /**
     * 결제 실패 처리를 위한 메소드
     */
    public void failPayment() {
        this.status = PaymentStatus.FAILED;
    }

    /**
     * 결제 취소 처리를 위한 메소드
     */
    public void cancelPayment() {
        this.status = PaymentStatus.CANCELLED;
        // this.cancelReason = reason; // 취소 사유를 저장할 필드가 있다면 추가
        // TODO: shopEntity에 따른 환불 로직 추가예정 + 환불 가능한지도 확인해야함
    }

    /**
     * 결제를 '만료' 상태로 변경합니다.
     * READY 상태일 때만 EXPIRED로 변경되도록 방어 로직을 추가하는 것이 안전합니다.
     */
    public void expirePayment() {
        if (this.status == PaymentStatus.READY) {
            this.status = PaymentStatus.EXPIRED;
        }
    }
}

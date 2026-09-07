package piq.piqproject.domain.payments.common.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import piq.piqproject.domain.payments.common.entity.PaymentEntity;
import piq.piqproject.domain.payments.common.enums.PaymentStatus;
import piq.piqproject.domain.users.entity.UserEntity;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findByMerchantUid(String merchantUid);

    Optional<PaymentEntity> findByTransactionId(String transactionId);

    /**
     * 특정 사용자의 결제 내역을 페이징하여 조회하고, 최신순으로 정렬합니다.
     * Spring Data JPA가 메소드 이름을 분석하여 아래와 같은 JPQL을 자동으로 생성합니다.
     * "SELECT p FROM PaymentEntity p WHERE p.user = :user ORDER BY p.createdAt
     * DESC"
     *
     * @param user     조회할 사용자
     * @param pageable 페이징 및 정렬 정보
     * @return 페이징된 결제 내역
     */
    Page<PaymentEntity> findByUserOrderByCreatedAtDesc(UserEntity user, Pageable pageable);

    /**
     * 특정 상태(status)이면서, 특정 시간(dateTime) 이전에 생성된 모든 결제 정보를 조회합니다.
     *
     * @param status   조회할 결제 상태
     * @param dateTime 기준 시간
     * @return 조건에 맞는 결제 엔티티 리스트
     */
    List<PaymentEntity> findAllByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime dateTime);

    /**
     * [비관적 락 적용] 결제 검증 시 사용
     * PESSIMISTIC_WRITE: 해당 데이터에 '배타적 락(Exclusive Lock)'을 겁니다.
     * 트랜잭션이 끝날 때까지 다른 트랜잭션은 이 데이터를 읽거나 수정할 수 없습니다.
     * SQL: SELECT * FROM payments WHERE merchant_uid = ? FOR UPDATE
     *
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentEntity p where p.merchantUid = :merchantUid")
    Optional<PaymentEntity> findByMerchantUidWithLock(@Param("merchantUid") String merchantUid);
}

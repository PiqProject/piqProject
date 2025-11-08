package piq.piqproject.domain.payments.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import piq.piqproject.domain.payments.entity.PaymentEntity;
import piq.piqproject.domain.payments.enums.PaymentStatus;
import piq.piqproject.domain.payments.repository.PaymentRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentCleanupService {

    private final PaymentRepository paymentRepository;

    /**
     * 생성된 지 24시간 이상 지난 'READY' 상태의 결제들을 'EXPIRED' 상태로 변경합니다.
     * 
     * @return 상태가 변경된 결제의 총 개수
     */
    @Transactional
    public int cleanupOldReadyPayments() {
        // 1. 기준 시간 설정 (현재 시간으로부터 24시간 전)
        // 24시간이상 지난 READY 상태의 결제를 찾음( 추후 변경 가능 - ex. 1시간 )
        LocalDateTime cutoffDateTime = LocalDateTime.now().minusHours(24);

        // 2. Repository를 통해 만료 대상 결제 목록 조회
        List<PaymentEntity> expiredPayments = paymentRepository.findAllByStatusAndCreatedAtBefore(
                PaymentStatus.READY,
                cutoffDateTime);

        if (expiredPayments.isEmpty()) {
            return 0; // 처리할 건이 없으면 0을 반환
        }

        // 3. 조회된 엔티티들의 상태를 EXPIRED로 변경
        for (PaymentEntity payment : expiredPayments) {
            payment.expirePayment();
        }

        // @Transactional 어노테이션에 의해 메소드가 성공적으로 끝나면 변경된 내용이 DB에 자동으로 커밋됩니다.
        return expiredPayments.size();
    }
}
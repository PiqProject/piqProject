package piq.piqproject.domain.points.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.InvalidRequestException;
import piq.piqproject.domain.points.entity.PointHistoryEntity;
import piq.piqproject.domain.points.enums.PointType;
import piq.piqproject.domain.points.repository.PointHistoryRepository;
import piq.piqproject.domain.users.entity.UserEntity;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {

    private final PointHistoryRepository pointHistoryRepository;

    /**
     * 포인트 사용 (차감)
     * 
     * @param user        사용자 Entity (영속 상태여야 함)
     * @param amount      사용 금액 (양수 입력 -> 내부적으로 음수 처리)
     * @param description 사용 사유
     */
    @Transactional
    public void usePoints(UserEntity user, int amount, String description) {
        if (amount <= 0)
            throw new InvalidRequestException(ErrorCode.BAD_REQUEST, "사용 금액은 0보다 커야 합니다.");

        // 1. 잔액 체크 및 차감 (UserEntity 메서드 활용)
        if (user.getPqPoint() < amount) {
            throw new InvalidRequestException(ErrorCode.NOT_ENOUGH_POINT, "포인트가 부족합니다.");
        }

        user.deductPqPoints(amount); // 기존 메서드 재활용 (pqPoint -= amount)

        // 2. 히스토리 저장
        saveHistory(user, PointType.USE, -amount, description);

        log.info("[POINT USE] User: {}, Amount: -{}, Reason: {}", user.getId(), amount, description);
    }

    /**
     * 포인트 충전/지급 (증가)
     */
    @Transactional
    public void chargePoints(UserEntity user, int amount, PointType type, String description) {
        if (amount <= 0)
            throw new InvalidRequestException(ErrorCode.BAD_REQUEST, "충전 금액은 0보다 커야 합니다.");

        // 1. 포인트 증가
        user.refundPqPoints(amount); // 기존 메서드 재활용 (pqPoint += amount)

        // 2. 히스토리 저장
        saveHistory(user, type, amount, description);

        log.info("[POINT CHARGE] User: {}, Amount: +{}, Type: {}", user.getId(), amount, type);
    }

    // 공통 저장 로직
    private void saveHistory(UserEntity user, PointType type, int amount, String description) {
        PointHistoryEntity history = PointHistoryEntity.builder()
                .user(user)
                .type(type)
                .amount(amount)
                .balanceSnapshot(user.getPqPoint()) // 변경된 후의 잔액 저장
                .description(description)
                .build();

        pointHistoryRepository.save(history);
    }
}
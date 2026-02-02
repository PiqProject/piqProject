package piq.piqproject.domain.points.service;

import java.util.ArrayList;
import java.util.List;
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
     * [관리자용] 포인트 회수 (보유량 내에서만 회수)
     */
    @Transactional
    public void revokePointsSafely(UserEntity user, int targetAmount, String description) {
        // 1. 엔티티 로직 호출 (실제 차감된 양 반환)
        int actualDeducted = user.deductPointsFloorZero(targetAmount);

        // 2. 히스토리 기록 (실제 차감된 만큼만)
        if (actualDeducted > 0)
            saveHistory(user, PointType.REFUND, -actualDeducted, description);
    }

    /**
     * 포인트 강제 회수 (음수 허용)
     * 인앱 결제 환불 등 포인트를 반드시 회수해야 하는 경우 사용합니다.
     */
    @Transactional
    public void forceRevokePoints(UserEntity user, int amount, String description) {
        if (amount <= 0)
            throw new InvalidRequestException(ErrorCode.BAD_REQUEST, "회수 금액은 0보다 커야 합니다.");

        // 잔액 체크 없이 강제 차감 (음수 허용)
        user.deductPqPoints(amount);

        // 히스토리 저장
        saveHistory(user, PointType.USE, -amount, description);

        log.info("[POINT FORCE REVOKE] User: {}, Amount: -{}, Reason: {}", user.getId(), amount, description);
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

    /**
     * 포인트 일괄 조정
     * - DB에는 개별 히스토리를 남기되, 쿼리 효율을 위해 Bulk 처리
     */
    @Transactional
    public void adjustPointsBulk(List<UserEntity> users, int amount, String description) {
        if (amount == 0 || users.isEmpty())
            return;

        List<PointHistoryEntity> histories = new ArrayList<>();
        PointType type = PointType.ADMIN;

        for (UserEntity user : users) {
            int actualAmount = amount;

            // 1. 사용자 잔액 변경
            if (amount > 0) {
                user.refundPqPoints(amount);
            } else {
                int absoluteAmount = Math.abs(amount);
                // 보유량 내에서만 차감 (0원 바닥 정책)
                int deductAmount = Math.min(user.getPqPoint(), absoluteAmount);
                if (deductAmount <= 0)
                    continue; // 차감할 게 없으면 히스토리도 패스

                user.deductPqPoints(deductAmount);
                actualAmount = -deductAmount; // 실제 차감액으로 기록
            }

            // 2. 히스토리 객체 생성 (저장은 나중에 한방에)
            PointHistoryEntity history = PointHistoryEntity.builder()
                    .user(user)
                    .type(type)
                    .amount(actualAmount)
                    .balanceSnapshot(user.getPqPoint()) // 변경 후 잔액
                    .description(description)
                    .build();

            histories.add(history);
        }

        // 3. 히스토리 일괄 저장
        pointHistoryRepository.saveAll(histories);

        // 4. 서버 로그
        log.info("[ADMIN BULK POINT] Users: {}, Amount: {}, Reason: {}", users.size(), amount, description);
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
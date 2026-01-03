package piq.piqproject.domain.dislikes.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.domain.dislikes.entity.DislikeEntity;
import piq.piqproject.domain.dislikes.repository.DislikeRepository;
import piq.piqproject.domain.recommendations.service.DailyRecommendationService;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.repository.UserRepository;

@Slf4j
@RequiredArgsConstructor
@Service
public class DislikeService {
    private final DislikeRepository dislikeRepository;
    private final UserRepository userRepository;
    private final DailyRecommendationService dailyRecommendationService;

    /**
     * 특정 사용자가 다른 사용자를 '싫어요' 처리하고, 관련 추천 기록을 업데이트합니다.
     *
     * @param fromUser '싫어요'를 누르는 주체 사용자 엔티티
     * @param toUserId '싫어요' 대상이 되는 사용자 ID
     */
    @Transactional
    public void createDislike(UserEntity fromUser, Long toUserId) {
        // 1. '싫어요' 대상 사용자(toUser) Entity 조회
        UserEntity toUser = userRepository.findById(toUserId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER,
                        "싫어요 대상 사용자를 찾을 수 없습니다. ID: " + toUserId));

        // 2. 중복 '싫어요' 확인 (선택 사항: DB의 Unique Index가 처리할 수도 있지만, 서비스에서 미리 확인하면 깔끔함)
        if (dislikeRepository.existsByFromUserAndToUser(fromUser, toUser)) {
            log.warn("사용자 ID {} 가 사용자 ID {} 에 대해 이미 '싫어요' 기록을 가지고 있습니다. 중복 요청 무시.",
                    fromUser.getId(), toUserId);
            // 이미 존재하면 추가적인 DB 작업을 하지 않고 종료
            return;
        }

        // 3. DislikeEntity 생성 및 저장
        DislikeEntity newDislike = DislikeEntity.builder()
                .fromUser(fromUser)
                .toUser(toUser)
                .build();
        dislikeRepository.save(newDislike);

        // 4. 오늘의 추천 기록 업데이트 (스케줄러 자동 처리 대상에서 제외)
        dailyRecommendationService.markRecommendationAsActioned(fromUser, toUser);
    }

}

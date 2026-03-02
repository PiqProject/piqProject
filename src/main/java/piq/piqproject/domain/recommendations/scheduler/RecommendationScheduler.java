package piq.piqproject.domain.recommendations.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import piq.piqproject.domain.dislikes.entity.DislikeEntity;
import piq.piqproject.domain.dislikes.repository.DislikeRepository;
import piq.piqproject.domain.recommendations.entity.DailyRecommendationEntity;
import piq.piqproject.domain.recommendations.repository.DailyRecommendationRepository;
import piq.piqproject.domain.users.entity.UserEntity;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationScheduler {

    private final DailyRecommendationRepository dailyRecommendationRepository;
    private final DislikeRepository dislikeRepository;

    @Value("${recommendation.reset-hour:4}")
    private int recommendationResetHour;

    /**
     * 매일 추천 리셋 시간에 실행되어, 응답이 없었던 어제의 추천을 '싫어요'로 처리합니다.
     * cron = "0 0 4 * * *" : 매일 새벽 4시 0분 0초에 실행
     * 이 cron 표현식은 application.yml/properties 파일에서 설정값으로 관리하는 것이 더 유연합니다.
     * 예: @Scheduled(cron = "${recommendation.scheduler.cron}")
     */
    @Scheduled(cron = "${recommendation.scheduler.cron}")
    @Transactional
    public void processUnansweredRecommendations() {
        log.info(
                "===== [Scheduler Started] Starting process to mark yesterday's unanswered recommendations as 'disliked'. =====");

        // 1. '어제의 추천 하루'에 해당하는 정확한 시간 범위를 계산합니다.
        // 스케줄러가 새벽 4시에 실행되므로, '지금'이 바로 '어제 하루'가 끝나는 시점입니다.
        LocalDateTime endOfRange = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        LocalDateTime startOfRange = endOfRange.minusDays(1);

        log.info("Target time range: {} to {}", startOfRange, endOfRange);

        // 2. 해당 시간 범위 내에서 'actioned'가 false인 모든 추천 기록을 조회합니다.
        List<DailyRecommendationEntity> unansweredRecs = dailyRecommendationRepository
                .findByActionedFalseAndCreatedAtBetween(startOfRange, endOfRange);

        if (unansweredRecs.isEmpty()) {
            log.info("No unanswered recommendations to process.");
            log.info("===== [Scheduler Ended] Process completed. =====");
            return;
        }

        log.info("Processing {} unanswered recommendations as 'dislikes'.", unansweredRecs.size());

        // 3. 조회된 각 추천 기록을 '싫어요' 테이블에 추가합니다.
        for (DailyRecommendationEntity recommendation : unansweredRecs) {
            UserEntity fromUser = recommendation.getUser();
            UserEntity toUser = recommendation.getRecommendedUser();

            // 3-1. DB의 Unique 제약조건 위반을 방지하기 위해, 이미 '싫어요' 기록이 있는지 확인합니다.
            // (사용자가 스케줄러 실행 직전에 수동으로 싫어요를 눌렀을 경우 등을 대비)
            if (!dislikeRepository.existsByFromUserAndToUser(fromUser, toUser)) {
                DislikeEntity newDislike = DislikeEntity.builder()
                        .fromUser(fromUser)
                        .toUser(toUser)
                        .build();
                dislikeRepository.save(newDislike);
                log.debug("Dislike record created: From {} To {}", fromUser.getId(), toUser.getId());
            } else {
                log.warn("Dislike record already exists, skipping: From {} To {}", fromUser.getId(), toUser.getId());
            }
        }

        // TODO: 알림 전송 로직 필요 (오늘의 추천 도착)
        log.info("===== [Scheduler Ended] Successfully completed process. =====");
    }
}
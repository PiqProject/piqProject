package piq.piqproject.domain.recommendations.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import piq.piqproject.domain.dislikes.repository.DislikeRepository;
import piq.piqproject.domain.matches.repository.MatchingRepository;
import piq.piqproject.domain.recommendations.dto.RecommendedUserResponseDto;
import piq.piqproject.domain.recommendations.entity.DailyRecommendationEntity;
import piq.piqproject.domain.recommendations.repository.DailyRecommendationRepository;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.enums.Gender; // Gender Enum import
import piq.piqproject.domain.users.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyRecommendationService {

    private final UserRepository userRepository;
    private final MatchingRepository matchingRepository;
    private final DislikeRepository dislikeRepository;
    private final DailyRecommendationRepository dailyRecommendationRepository;

    private static final int DAILY_RECOMMENDATION_COUNT_FOR_WOMENS = 2; // 4
    private static final int DAILY_RECOMMENDATION_COUNT_FOR_MENS = 2;
    private static final int CANDIDATE_POOL_SIZE = 2;// 30

    // --- ▼▼▼ 기준 시간 설정 ▼▼▼ ---
    /**
     * 추천 목록이 업데이트되는 기준 시간(Hour, 0-23)입니다.
     * application.yml 또는 application.properties 파일에서 값을 주입받습니다.
     * 예: recommendation.reset-hour: 4 (새벽 4시)
     */
    @Value("${recommendation.reset-hour:4}") // 기본값을 새벽 4시로 설정
    private int recommendationResetHour;

    @Transactional
    public List<RecommendedUserResponseDto> getDailyRecommendations(UserEntity user) {
        // '추천 하루'의 정확한 시작과 끝 시간을 계산
        List<LocalDateTime> timeRange = getRecommendationTimeRange();
        LocalDateTime startOfRecommendationDay = timeRange.get(0);
        LocalDateTime endOfRecommendationDay = timeRange.get(1);

        // [1단계] 오늘 이미 추천받은 기록이 있는지 확인 (Fast Path)
        List<DailyRecommendationEntity> existingRecommendations = dailyRecommendationRepository
                .findByRecommendingUserAndDateRange(user, startOfRecommendationDay, endOfRecommendationDay);

        if (!existingRecommendations.isEmpty()) {
            log.info("사용자 ID {} 에게 기존 추천 목록을 반환합니다.", user.getId());
            return existingRecommendations.stream()
                    // 추천 정보에서 userEntity를 가져와 사용자에게 간략한 정보를 전달하는 DTO로 변환하는 부분
                    .map(recommendation -> convertToDto(recommendation.getRecommendedUser()))
                    .collect(Collectors.toList());
        }

        // [1단계 실패 시] 새로운 추천 생성 로직 실행 (Slow Path)
        log.info("사용자 ID {} 에게 새로운 추천 목록을 생성합니다.", user.getId());
        List<UserEntity> finalRecommendations = generateAndSaveNewRecommendations(user);

        return finalRecommendations.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 특정 사용자가 추천받은 상대에 대해 액션을 취했음을 기록합니다.
     * 이 메서드는 다른 서비스(Matching, Dislike)에서 호출됩니다.
     * 
     * @param user            액션을 취한 주체
     * @param recommendedUser 액션의 대상이 된 사용자
     */
    @Transactional // 중요: 외부에서 호출되므로 트랜잭션 전파를 위해 필요
    public void markRecommendationAsActioned(UserEntity user, UserEntity recommendedUser) {
        List<LocalDateTime> timeRange = getRecommendationTimeRange();
        LocalDateTime startOfRecommendationDay = timeRange.get(0);
        LocalDateTime endOfRecommendationDay = timeRange.get(1);

        dailyRecommendationRepository
                .findByUserAndRecommendedUserAndDateRange(user, recommendedUser, startOfRecommendationDay,
                        endOfRecommendationDay)
                .ifPresent(recommendation -> {
                    recommendation.markAsActioned();
                    log.info("추천 기록 ID {} 가 사용자의 액션으로 인해 actioned 상태로 변경되었습니다.", recommendation.getId());
                });
    }

    /**
     * 현재 시간을 기준으로 '추천 하루'의 정확한 시작 시간과 종료 시간을 계산하여 반환합니다.
     * 
     * @return List<LocalDateTime> [시작시간, 종료시간]
     */
    private List<LocalDateTime> getRecommendationTimeRange() {
        LocalDateTime now = LocalDateTime.now();
        LocalTime resetTime = LocalTime.of(recommendationResetHour, 0);

        // 오늘 날짜의 리셋 시간 (예: 오늘 새벽 4시)
        LocalDateTime todayResetDateTime = now.toLocalDate().atTime(resetTime);

        LocalDateTime startDateTime;
        LocalDateTime endDateTime;

        if (now.isBefore(todayResetDateTime)) {
            // 현재 시간이 오늘 리셋 시간 이전인 경우 (예: 새벽 3시)
            // -> '추천 하루'는 [어제 리셋 시간]부터 [오늘 리셋 시간]까지입니다.
            startDateTime = todayResetDateTime.minusDays(1);
            endDateTime = todayResetDateTime;
        } else {
            // 현재 시간이 오늘 리셋 시간 이후인 경우 (예: 오전 10시)
            // -> '추천 하루'는 [오늘 리셋 시간]부터 [내일 리셋 시간]까지입니다.
            startDateTime = todayResetDateTime;
            endDateTime = todayResetDateTime.plusDays(1);
        }
        return List.of(startDateTime, endDateTime);
    }

    private List<UserEntity> generateAndSaveNewRecommendations(UserEntity user) {
        // [2단계] 추천 제외 대상 ID 목록 수집 (매칭을 이미 해본 사람(보내든,받았든) + 싫어요 + 본인)
        Set<Long> excludedUserIds = getExcludedUserIds(user.getId());

        // [3단계] 1차 후보군 조회 (상위 30명)
        Page<UserEntity> candidatePage = userRepository.findTopCandidatesByPreferences(
                user, excludedUserIds, PageRequest.of(0, CANDIDATE_POOL_SIZE));

        List<UserEntity> candidates = new ArrayList<>(candidatePage.getContent());

        // 사용자의 성별에 따라 최종 추천 인원 결정
        int recommendationCount = (user.getGender() == Gender.FEMALE)
                ? DAILY_RECOMMENDATION_COUNT_FOR_WOMENS
                : DAILY_RECOMMENDATION_COUNT_FOR_MENS;

        // [4단계] 후보군 고갈 시나리오 처리
        if (candidates.size() < recommendationCount) {
            log.warn("사용자 ID {} 의 추천 후보군이 부족합니다. '싫어요' 목록을 초기화하고 재시도합니다.", user.getId());
            dislikeRepository.deleteAllByFromUser(user);
            Set<Long> permanentExcludedIds = getPermanentlyExcludedUserIds(user.getId());
            Page<UserEntity> candidatePageRetry = userRepository.findTopCandidatesByPreferences(
                    user, permanentExcludedIds, PageRequest.of(0, CANDIDATE_POOL_SIZE));
            candidates = candidatePageRetry.getContent();
        }

        if (candidates.isEmpty()) {
            log.info("사용자 ID {} 에게 추천할 사용자가 없습니다.", user.getId());
            return Collections.emptyList();
        }

        // [5단계] 최종 추천 대상 랜덤 선택 및 저장
        Collections.shuffle(candidates);
        List<UserEntity> finalRecommendations = candidates.stream()
                .limit(recommendationCount)
                .collect(Collectors.toList());

        finalRecommendations.forEach(recommendedUser -> {
            DailyRecommendationEntity recommendation = DailyRecommendationEntity.builder()
                    .user(user)
                    .recommendedUser(recommendedUser)
                    .build();
            dailyRecommendationRepository.save(recommendation);
        });

        log.info("사용자 ID {} 에게 새로운 추천 목록 {} 명을 생성 및 저장했습니다.", user.getId(), finalRecommendations.size());
        return finalRecommendations;
    }

    private Set<Long> getExcludedUserIds(Long userId) {
        Set<Long> excludedIds = getPermanentlyExcludedUserIds(userId);
        // 내가 싫어요한 사용자 목록을 추천에서 제외할 id집합에 추가
        excludedIds.addAll(dislikeRepository.findToUserIdsByFromUserId(userId));
        return excludedIds;
    }

    private Set<Long> getPermanentlyExcludedUserIds(Long userId) {
        Set<Long> excludedIds = new HashSet<>();
        excludedIds.add(userId);
        excludedIds.addAll(matchingRepository.findAllMatchedUserIdsByUserId(userId));
        excludedIds.add(1L);// 관리자 계정 ID 제외
        return excludedIds;
    }

    private RecommendedUserResponseDto convertToDto(UserEntity user) {
        // ... UserEntity를 RecommendedUserResponseDto로 변환하는 로직 ...
        // 예시: return RecommendedUserResponseDto.fromEntity(user);
        return new RecommendedUserResponseDto(user);
    }
}
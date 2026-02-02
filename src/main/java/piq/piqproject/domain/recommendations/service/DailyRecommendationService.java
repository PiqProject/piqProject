package piq.piqproject.domain.recommendations.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.domain.dislikes.repository.DislikeRepository;
import piq.piqproject.domain.matches.repository.MatchingRepository;
import piq.piqproject.domain.recommendations.dto.RecommendedUserResponseDto;
import piq.piqproject.domain.recommendations.entity.DailyRecommendationEntity;
import piq.piqproject.domain.recommendations.repository.DailyRecommendationRepository;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.enums.Gender; // Gender Enum import
import piq.piqproject.domain.users.repository.UserRepository;
import piq.piqproject.domain.matches.enums.MatchingStatus;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyRecommendationService {

        private final UserRepository userRepository;
        private final MatchingRepository matchingRepository;
        private final DislikeRepository dislikeRepository;
        private final DailyRecommendationRepository dailyRecommendationRepository;

        private static final int DAILY_RECOMMENDATION_COUNT_FOR_WOMENS = 4;
        private static final int DAILY_RECOMMENDATION_COUNT_FOR_MENS = 2;
        private static final int CANDIDATE_POOL_SIZE = 15;
        private static final double[] SEARCH_RADIUS_STEPS = { 30000.0, 50000.0, 100000.0, 200000 };

        // --- ▼▼▼ 기준 시간 설정 ▼▼▼ ---
        /**
         * 추천 목록이 업데이트되는 기준 시간(Hour, 0-23)입니다.
         * application.yml 또는 application.properties 파일에서 값을 주입받습니다.
         * 예: recommendation.reset-hour: 4 (새벽 4시)
         */
        @Value("${recommendation.reset-hour:4}") // 기본값을 새벽 4시로 설정
        private int recommendationResetHour;

        @Transactional
        public List<RecommendedUserResponseDto> getDailyRecommendations(UserEntity principalUser,
                        boolean isPremiumRequest) {

                UserEntity user = userRepository.findById(principalUser.getId())
                                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER, "User not found"));

                // '추천 하루'의 정확한 시작과 끝 시간을 계산
                List<LocalDateTime> timeRange = getRecommendationTimeRange();
                LocalDateTime startOfRecommendationDay = timeRange.get(0);
                LocalDateTime endOfRecommendationDay = timeRange.get(1);

                // [1단계] 오늘 이미 추천받은 기록이 있는지 확인 (Fast Path)
                List<DailyRecommendationEntity> existingRecommendations = dailyRecommendationRepository
                                .findByRecommendingUserAndDateRange(user, startOfRecommendationDay,
                                                endOfRecommendationDay);

                if (!existingRecommendations.isEmpty()) {
                        log.info("사용자 ID {} 에게 기존 추천 목록을 반환합니다.", user.getId());
                        return existingRecommendations.stream()
                                        // 추천 정보에서 userEntity를 가져와 사용자에게 간략한 정보를 전달하는 DTO로 변환하는 부분
                                        .map(recommendation -> convertToDto(recommendation.getRecommendedUser()))
                                        .collect(Collectors.toList());
                }

                // [1단계 실패 시] 새로운 추천 생성 로직 실행 (Slow Path)
                log.info("사용자 ID {} 에게 새로운 추천 목록을 생성합니다.", user.getId());
                List<UserEntity> finalRecommendations = generateAndSaveNewRecommendations(user, isPremiumRequest);

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
                                .findByUserAndRecommendedUserAndDateRange(user, recommendedUser,
                                                startOfRecommendationDay,
                                                endOfRecommendationDay)
                                .ifPresent(recommendation -> {
                                        recommendation.markAsActioned();
                                        log.info("추천 기록 ID {} 가 사용자의 액션으로 인해 actioned 상태로 변경되었습니다.",
                                                        recommendation.getId());
                                });
        }

        /**
         * 현재 시간을 기준으로 '추천 하루'의 정확한 시작 시간과 종료 시간을 계산하여 반환합니다.
         * 
         * @return List<LocalDateTime> [시작시간, 종료시간]
         */
        public List<LocalDateTime> getRecommendationTimeRange() {
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

        /**
         * 추천 목록 생성 로직 (반경 확장 로직 적용)
         */
        private List<UserEntity> generateAndSaveNewRecommendations(UserEntity user, boolean isPremiumRequest) {

                Set<Long> excludedUserIds = getExcludedUserIds(user.getId());
                String targetGender = (user.getGender() == Gender.MALE) ? "FEMALE" : "MALE";

                // 좌표가 없는 경우 기본값 (서울시청)
                double myLat = (user.getLatitude() != null) ? user.getLatitude() : 37.5665;
                double myLon = (user.getLongitude() != null) ? user.getLongitude() : 126.9780;

                List<UserEntity> candidates = new ArrayList<>();

                // ▼▼▼ [핵심 로직] 반경을 넓혀가며 재시도 ▼▼▼
                for (double radius : SEARCH_RADIUS_STEPS) {

                        log.info("추천 후보 조회 시도: UserID={}, Radius={}km, Premium={}",
                                        user.getId(), radius / 1000, isPremiumRequest);

                        if (isPremiumRequest) {
                                // [유료] 이상형 점수 기반 조회
                                candidates = userRepository.findUsersByIdealMatch(
                                                user.getId(), targetGender, myLon, myLat,
                                                radius, // 현재 단계의 반경
                                                excludedUserIds, CANDIDATE_POOL_SIZE);
                        } else {
                                // [기본] 단순 거리 기반 조회
                                candidates = userRepository.findNearbyUsers(
                                                user.getId(), targetGender, myLon, myLat,
                                                radius, // 현재 단계의 반경
                                                excludedUserIds, CANDIDATE_POOL_SIZE);
                        }

                        // 후보군을 충분히 찾았다면 루프 종료 (예: 2명 이상이면 충분)
                        // CANDIDATE_POOL_SIZE(30)까지는 아니더라도, 최소 추천 개수(2)만 넘으면 멈춤
                        int minRequired = (user.getGender() == Gender.FEMALE)
                                        ? DAILY_RECOMMENDATION_COUNT_FOR_WOMENS
                                        : DAILY_RECOMMENDATION_COUNT_FOR_MENS;

                        if (candidates.size() >= minRequired) {
                                log.info("후보군 확보 성공: {}명 (Radius: {}km)", candidates.size(), radius / 1000);
                                break; // 루프 탈출
                        }
                }
                // ▲▲▲ [핵심 로직] ▲▲▲

                // [고갈 처리] 500km까지 뒤졌는데도 부족하다면?
                // -> 싫어요 목록을 초기화하고, '전국(500km)' 범위로 다시 한번 시도
                int requiredCount = (user.getGender() == Gender.FEMALE)
                                ? DAILY_RECOMMENDATION_COUNT_FOR_WOMENS
                                : DAILY_RECOMMENDATION_COUNT_FOR_MENS;

                if (candidates.size() < requiredCount) {
                        log.warn(" 범위 조회 실패. '싫어요' 목록 초기화 후 재시도합니다. UserID={}", user.getId());

                        dislikeRepository.deleteAllByFromUser(user);
                        Set<Long> permanentExcludedIds = getPermanentlyExcludedUserIds(user.getId()); // 싫어요 제외된 목록

                        // 마지막 단계 반경으로 재시도
                        double maxRadius = SEARCH_RADIUS_STEPS[SEARCH_RADIUS_STEPS.length - 1];

                        if (isPremiumRequest) {
                                candidates = userRepository.findUsersByIdealMatch(
                                                user.getId(), targetGender, myLon, myLat, maxRadius,
                                                permanentExcludedIds, CANDIDATE_POOL_SIZE);
                        } else {
                                candidates = userRepository.findNearbyUsers(
                                                user.getId(), targetGender, myLon, myLat, maxRadius,
                                                permanentExcludedIds, CANDIDATE_POOL_SIZE);
                        }
                }

                // 셔플 및 저장 (기존 로직)
                if (!isPremiumRequest) {
                        Collections.shuffle(candidates);
                }

                List<UserEntity> finalRecommendations = candidates.stream()
                                .limit(requiredCount)
                                .collect(Collectors.toList());

                // [저장] 여기서 DB에 실제로 넣습니다.
                for (UserEntity recommendedUser : finalRecommendations) {
                        DailyRecommendationEntity recommendation = DailyRecommendationEntity.builder()
                                        .user(user)
                                        .recommendedUser(recommendedUser)
                                        .build();
                        dailyRecommendationRepository.save(recommendation);
                }

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
                excludedIds.addAll(matchingRepository.findAllMatchedUserIdsByUserIdAndStatus(userId,
                                MatchingStatus.SUCCESS));
                excludedIds.add(1L);// 관리자 계정 ID 제외
                return excludedIds;
        }

        private RecommendedUserResponseDto convertToDto(UserEntity user) {
                // ... UserEntity를 RecommendedUserResponseDto로 변환하는 로직 ...
                // 예시: return RecommendedUserResponseDto.fromEntity(user);
                return new RecommendedUserResponseDto(user);
        }
}
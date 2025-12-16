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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.elastic.clients.elasticsearch._types.FieldValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.domain.dislikes.repository.DislikeRepository;
import piq.piqproject.domain.matches.repository.MatchingRepository;
import piq.piqproject.domain.recommendations.dto.RecommendedUserResponseDto;
import piq.piqproject.domain.recommendations.entity.DailyRecommendationEntity;
import piq.piqproject.domain.recommendations.repository.DailyRecommendationRepository;
import piq.piqproject.domain.search.document.UserDocument;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.enums.Gender; // Gender Enum import
import piq.piqproject.domain.users.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyRecommendationService {

    private final UserRepository userRepository;
    private final MatchingRepository matchingRepository;
    private final DislikeRepository dislikeRepository;
    private final DailyRecommendationRepository dailyRecommendationRepository;
    private final ElasticsearchOperations elasticsearchOperations;

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
    public List<RecommendedUserResponseDto> getDailyRecommendations(UserEntity principalUser) {

        UserEntity user = userRepository.findById(principalUser.getId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND_USER, "User not found"));

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

    /**
     * 새로운 추천 목록을 생성하고 저장합니다.
     * (Elasticsearch + RDB 하이브리드 방식)
     */
    private List<UserEntity> generateAndSaveNewRecommendations(UserEntity user) {

        // 1. [준비] 추천 제외 대상 ID 목록 수집 (매칭됨 + 싫어요 + 본인)
        Set<Long> excludedUserIds = getExcludedUserIds(user.getId());

        // 2. [검색 - 1차 시도] Elasticsearch에서 알고리즘에 기반한 상위 후보군 ID 조회
        List<Long> candidateIds = searchCandidatesInElasticsearch(user, excludedUserIds);

        // 3. [변환] ID 목록으로 RDB에서 실제 UserEntity 리스트 조회
        // (DailyRecommendationEntity에 저장하려면 영속성 컨텍스트가 관리하는 엔티티가 필요함)
        List<UserEntity> candidates = userRepository.findAllById(candidateIds);

        // 사용자의 성별에 따라 필요한 추천 인원 수 결정
        int recommendationCount = (user.getGender() == Gender.FEMALE)
                ? DAILY_RECOMMENDATION_COUNT_FOR_WOMENS
                : DAILY_RECOMMENDATION_COUNT_FOR_MENS;

        // 4. [고갈 처리] 후보군이 부족할 경우 재시도 로직
        if (candidates.size() < recommendationCount) {
            log.warn("사용자 ID {} 의 추천 후보군이 부족합니다({}명). '싫어요' 목록을 초기화하고 재시도합니다.",
                    user.getId(), candidates.size());

            // 4-1. RDB에서 '싫어요' 기록 삭제
            dislikeRepository.deleteAllByFromUser(user);

            // 4-2. 제외 대상 재설정 (매칭됨 + 본인만 제외, '싫어요'는 제외 안 함)
            Set<Long> permanentExcludedIds = getPermanentlyExcludedUserIds(user.getId());

            // 4-3. [검색 - 2차 시도] Elasticsearch 재검색
            List<Long> retryCandidateIds = searchCandidatesInElasticsearch(user, permanentExcludedIds);

            // 4-4. [변환] 재조회된 ID로 엔티티 리스트 갱신
            candidates = userRepository.findAllById(retryCandidateIds);
        }

        // 5. [예외] 그래도 없으면 빈 리스트 반환
        if (candidates.isEmpty()) {
            log.info("사용자 ID {} 에게 추천할 사용자가 없습니다.", user.getId());
            return Collections.emptyList();
        }

        // 6. [선택] 후보군 내에서 랜덤 셔플 및 상위 N명 선택
        // (ES 점수가 높은 순으로 왔지만, 상위권 내에서는 랜덤성을 부여하여 매번 다른 느낌을 줌)
        // ※ 만약 '점수 순서대로' 보여주고 싶다면 shuffle을 제거하세요.
        // 하지만 findAllById는 순서를 보장하지 않으므로, 점수 순 유지를 원하면 별도 정렬 로직이 필요합니다.
        // 현재 기획(셔플)상 findAllById의 순서 미보장은 문제되지 않습니다.
        List<UserEntity> mutableCandidates = new ArrayList<>(candidates); // 수정 가능한 리스트로 변환
        Collections.shuffle(mutableCandidates);

        List<UserEntity> finalRecommendations = mutableCandidates.stream()
                .limit(recommendationCount)
                .collect(Collectors.toList());

        // 7. [저장] 최종 결과를 RDB(DailyRecommendation)에 저장
        finalRecommendations.forEach(recommendedUser -> {
            DailyRecommendationEntity recommendation = DailyRecommendationEntity.builder()
                    .user(user)
                    .recommendedUser(recommendedUser)
                    .build();
            dailyRecommendationRepository.save(recommendation);
        });

        log.info("사용자 ID {} 에게 새로운 추천 목록 {} 명을 생성 및 저장했습니다. (Source: Elasticsearch)",
                user.getId(), finalRecommendations.size());

        return finalRecommendations;
    }

    /**
     * Elasticsearch에 쿼리를 날려 추천 후보군의 ID 목록을 조회합니다.
     */
    private List<Long> searchCandidatesInElasticsearch(UserEntity user, Set<Long> excludedUserIds) {

        // 1. [준비] 쿼리에 필요한 데이터 추출
        // 1-1. 상대방 성별 결정 (이성 추천)
        Gender targetGender = (user.getGender() == Gender.MALE) ? Gender.FEMALE : Gender.MALE;

        // 1-2. 나의 관심사 ID 목록 추출
        List<Long> myInterestIds = user.getUserInterests().stream()
                .map(ui -> ui.getInterest().getId())
                .collect(Collectors.toList());

        // 1-3. 나의 이상형(TraitOption) ID 목록 추출
        List<Long> myIdealOptionIds = user.getUserIdeals().stream()
                .map(ui -> ui.getIdealOption().getId()) // 이상형 엔티티에서 Option ID 추출
                .collect(Collectors.toList());

        // 1-4. 제외할 ID 목록을 String으로 변환 후 FieldValue로 변환 (ES의 @Id가 String이므로)
        List<FieldValue> excludedIdValues = excludedUserIds.stream()
                .map(String::valueOf) // Long -> String
                .map(FieldValue::of) // String -> FieldValue (핵심!)
                .collect(Collectors.toList());

        // 2. [쿼리 빌드] NativeQuery를 사용하여 Elasticsearch Query DSL 구성
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> {

                    // 2-1. Filter (점수 계산 X, 무조건 만족해야 하는 조건 - 빠름)
                    b.filter(f -> f.term(t -> t.field("gender").value(targetGender.name()))); // 성별 일치
                    b.filter(f -> f.term(t -> t.field("isActive").value(true))); // 활성 유저만

                    // 2-2. Must Not (제외 조건)
                    if (!excludedIdValues.isEmpty()) {
                        b.mustNot(mn -> mn.terms(t -> t
                                .field("id") // UserDocument의 @Id 필드
                                .terms(v -> v.value(excludedIdValues))));
                    }

                    // 2-3. Should (점수 계산 O, 일치하면 가중치 부여)
                    // 조건 A: 관심사가 일치하는가? (가중치 1.0)
                    if (!myInterestIds.isEmpty()) {
                        List<FieldValue> interestFieldValues = myInterestIds.stream()
                                .map(FieldValue::of) // Long -> FieldValue 변환
                                .collect(Collectors.toList());

                        b.should(s -> s.terms(t -> t
                                .field("interestIds") // 검색할 필드명
                                .terms(tv -> tv.value(interestFieldValues)) // 나의 관심사 목록
                                .boost(1.0f) // 가중치
                        ));
                    }

                    // 조건 B: 상대의 특성이 나의 이상형과 일치하는가? (가중치 1.5)
                    if (!myIdealOptionIds.isEmpty()) {
                        List<FieldValue> idealFieldValues = myIdealOptionIds.stream()
                                .map(FieldValue::of)
                                .collect(Collectors.toList());

                        b.should(s -> s.terms(t -> t
                                .field("traitOptionIds") // 검색할 필드명 (상대방의 실제 특성)
                                .terms(tv -> tv.value(idealFieldValues)) // 나의 이상형 목록
                                .boost(1.5f) // 가중치
                        ));
                    }

                    return b;
                }))
                // 3. [페이징] 상위 30개만 조회
                .withPageable(PageRequest.of(0, CANDIDATE_POOL_SIZE))
                .build();

        // 4. [실행] 검색 수행
        SearchHits<UserDocument> searchHits = elasticsearchOperations.search(nativeQuery, UserDocument.class);

        // 5. [결과 변환] Document의 ID(String)를 Long으로 변환하여 반환
        return searchHits.stream()
                .map(hit -> Long.valueOf(hit.getContent().getId()))
                .collect(Collectors.toList());
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
package piq.piqproject.domain.recommendations.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import piq.piqproject.domain.recommendations.entity.DailyRecommendationEntity;
import piq.piqproject.domain.users.entity.UserEntity;

public interface DailyRecommendationRepository extends JpaRepository<DailyRecommendationEntity, Long> {
        /**
         * 특정 사용자의 특정 날짜에 생성된 추천 기록을 조회합니다.
         * createdAt (LocalDateTime) 필드를 기준으로 날짜의 시작과 끝 범위를 검색하여
         * 인덱스를 효율적으로 사용합니다.
         *
         * @param user       조회할 사용자 엔티티
         * @param startOfDay 조회할 날짜의 시작 시간 (예: 2025-11-10 00:00:00)
         * @param endOfDay   조회할 날짜의 다음 날 시작 시간 (예: 2025-11-11 00:00:00)
         * @return 추천 엔티티 리스트
         */
        @Query("SELECT r FROM DailyRecommendationEntity r " +
                        "WHERE r.user = :user " +
                        "AND r.createdAt >= :startOfDay AND r.createdAt < :endOfDay")
        List<DailyRecommendationEntity> findByRecommendingUserAndDateRange(
                        @Param("user") UserEntity user,
                        @Param("startOfDay") LocalDateTime startOfDay,
                        @Param("endOfDay") LocalDateTime endOfDay);

        /**
         * 특정 사용자가 특정 날짜 범위 내에 특정 상대를 추천받은 기록을 조회합니다. (최대 1개)
         */
        @Query("SELECT r FROM DailyRecommendationEntity r " +
                        "WHERE r.user = :user AND r.recommendedUser = :recommendedUser " +
                        "AND r.createdAt >= :startOfDay AND r.createdAt < :endOfDay")
        Optional<DailyRecommendationEntity> findByUserAndRecommendedUserAndDateRange(
                        @Param("user") UserEntity user,
                        @Param("recommendedUser") UserEntity recommendedUser,
                        @Param("startOfDay") LocalDateTime startOfDay,
                        @Param("endOfDay") LocalDateTime endOfDay);

        List<DailyRecommendationEntity> findByActionedFalseAndCreatedAtBetween(LocalDateTime start, LocalDateTime end);

        Boolean existsByUserAndRecommendedUser(UserEntity user, UserEntity recommendedUser);

        boolean existsByUserAndRecommendedUserAndCreatedAtBetween(
                        UserEntity user,
                        UserEntity recommendedUser,
                        LocalDateTime start,
                        LocalDateTime end);
}
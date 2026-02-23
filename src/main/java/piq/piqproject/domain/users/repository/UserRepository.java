package piq.piqproject.domain.users.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.enums.Gender;
import piq.piqproject.domain.users.enums.SocialType;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

  // 이메일을 통해 사용자를 찾는 메소드
  Optional<UserEntity> findByEmail(@Param("email") String email);

  @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.roles WHERE u.id = :id")
  Optional<UserEntity> findByIdWithRoles(@Param("id") Long id);

  // 이메일 존재 여부 확인
  boolean existsByEmail(String email);

  /**
   * 성별(gender)을 기준으로 모든 사용자 엔티티를 찾아 리스트로 반환합니다.
   * Spring Data JPA가 메소드 이름을 분석하여 아래와 같은 JPQL을 자동으로 생성합니다.
   * "SELECT u FROM UserEntity u WHERE u.gender = :gender"
   *
   * @param gender 검색할 성별 (Gender Enum 타입), Pageable pageable
   * @return 해당 성별을 가진 UserEntity의 리스트
   */
  Page<UserEntity> findAllByGender(Gender gender, Pageable pageable);

  List<UserEntity> findAllByGender(Gender gender);

  boolean existsByNickname(String nickname);

  // User를 조회할 때 연관된 images 한 번의 쿼리로 함께 가져온다.
  // 'LEFT JOIN FETCH'가 핵심입니다.
  // UserEntity의 이미지 컬렉션 필드명이 'images'라고 가정하겠습니다.
  @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.images WHERE u.id = :id")
  Optional<UserEntity> findByIdWithImages(@Param("id") Long id);

  // =================================================================================
  // 1. [기본/무료] 단순 거리 기반 추천
  // - 반경(radius)을 파라미터로 받아서 동적으로 필터링합니다.
  // ※ST_DistanceSphere()는 gostgresql에서만 사용가능함
  // =================================================================================
  @Query(value = """
      SELECT * FROM users u
      WHERE u.id != :myId
        AND u.gender = :targetGender
        AND u.is_active = true
        AND u.id NOT IN (:excludedIds)
        AND u.location IS NOT NULL
        AND ST_DistanceSphere(u.location, ST_MakePoint(:myLon, :myLat)) <= :radius
      ORDER BY
        ST_DistanceSphere(u.location, ST_MakePoint(:myLon, :myLat)) ASC
      LIMIT :limit
      """, nativeQuery = true)
  List<UserEntity> findNearbyUsers(
      @Param("myId") Long myId,
      @Param("targetGender") String targetGender,
      @Param("myLon") Double myLon,
      @Param("myLat") Double myLat,
      @Param("radius") double radius,
      @Param("excludedIds") Set<Long> excludedIds,
      @Param("limit") int limit);

  // =================================================================================
  // 2. [유료/프리미엄] 이상형 적합도 기반 추천
  // - 반경(radius)을 파라미터로 받아서 동적으로 필터링합니다.
  // =================================================================================
  @Query(value = """
      SELECT u.* FROM users u
      WHERE u.id != :myId
        AND u.gender = :targetGender
        AND u.is_active = true
        AND u.id NOT IN (:excludedIds)
        AND u.location IS NOT NULL
        AND ST_DistanceSphere(u.location, ST_MakePoint(:myLon, :myLat)) <= :radius
      ORDER BY
         (
             -- [A] 관심사 점수 (가중치 1.0)
             (SELECT COUNT(*) FROM user_interests ui
              WHERE ui.user_id = u.id
              AND ui.interest_id IN (
                  SELECT my_ui.interest_id FROM user_interests my_ui WHERE my_ui.user_id = :myId
              )) * 1.0
             +
             -- [B] 이상형 점수 (가중치 1.5)
             (SELECT COUNT(*) FROM user_traits ut
              WHERE ut.user_id = u.id
              AND ut.option_id IN (
                  SELECT my_ideal.option_id FROM user_ideals my_ideal WHERE my_ideal.user_id = :myId
              )) * 1.5
         ) DESC,
         -- 점수 동점 시 거리순
         ST_DistanceSphere(u.location, ST_MakePoint(:myLon, :myLat)) ASC
      LIMIT :limit
      """, nativeQuery = true)
  List<UserEntity> findUsersByIdealMatch(
      @Param("myId") Long myId,
      @Param("targetGender") String targetGender,
      @Param("myLon") Double myLon,
      @Param("myLat") Double myLat,
      @Param("radius") double radius,
      @Param("excludedIds") Set<Long> excludedIds,
      @Param("limit") int limit);

  Optional<UserEntity> findBySocialId(String socialId);

  /**
   * 특정 시점 이전에 탈퇴한 사용자 목록을 조회합니다 (스케줄러용).
   */
  java.util.List<UserEntity> findByWithdrawnAtBefore(java.time.LocalDateTime dateTime);

  /**
   * 소셜 타입과 ID로 사용자를 찾습니다.
   * 소셜 타입과 ID가 유니크한 제약 조건을 가지므로, 이 메소드는 유일한 결과를 반환
   */
  Optional<UserEntity> findBySocialTypeAndSocialId(SocialType socialType, String socialId);

  // searchId가 없으면 전체 조회, 있으면 정확히 일치하는 ID만 조회
  @Query("SELECT u FROM UserEntity u " +
      "WHERE (:searchId IS NULL OR u.id = :searchId)")
  Page<UserEntity> searchAdminUsers(@Param("searchId") Long searchId, Pageable pageable);
}
package piq.piqproject.domain.users.repository;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.enums.Gender;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

        // 이메일을 통해 사용자를 찾는 메소드
        Optional<UserEntity> findByEmail(@Param("email") String email);

        // 이메일로 사용자를 찾을 때, roles 컬렉션까지 JOIN FETCH로 함께 가져온다.
        @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.roles WHERE u.email = :email")
        Optional<UserEntity> findByEmailWithRoles(@Param("email") String email);

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

        boolean existsByNickname(String nickname);

        // User를 조회할 때 연관된 images 한 번의 쿼리로 함께 가져온다.
        // 'LEFT JOIN FETCH'가 핵심입니다.
        // UserEntity의 이미지 컬렉션 필드명이 'images'라고 가정하겠습니다.
        @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.images WHERE u.id = :id")
        Optional<UserEntity> findByIdWithImages(@Param("id") Long id);

        /**
         * 현재 사용자의 성별, 이상형, 관심사를 기반으로 최적의 추천 후보군을 조회합니다.
         * JPQL을 사용하여 복합적인 점수를 계산하고, 점수가 높은 순으로 정렬합니다.
         * ※Limit와 Offset을 적용하기 위해 Pageable 파라미터를 사용해야함
         * ex) Pageable pageRequest = PageRequest.of(0, 30) -> 이걸 pageable에 넘겨 30명 조회
         * 
         * @param user            현재 사용자 엔티티. 이 사용자의 성별, 이상형, 관심사가 기준이 됩니다.
         * @param excludedUserIds 추천에서 제외할 사용자들의 ID 목록.
         * @param pageable        조회할 인원 수(limit)와 페이지 번호를 담은 객체.
         * @return 점수 순으로 정렬된 추천 후보 사용자 페이지 객체.
         */
        @Query(value = "SELECT u FROM UserEntity u WHERE " +
                        "   u.id != :#{#user.id} AND " + // 1. 자기 자신 제외
                        "   u.id NOT IN :excludedUserIds AND " + // 2. 제외 목록에 있는 사용자 제외
                        "   u.gender != :#{#user.gender} AND " + // 3. 동성 제외
                        "   u.isActive = true" + // 4. 활성화된 사용자만 포함
                        " ORDER BY " +
                        "   (" + // -- 점수 계산 시작 --
                        "       (" + // 5. 공통 관심사 점수 계산 (가중치 1.0)
                        "           SELECT COUNT(common_interest) FROM UserInterestEntity common_interest " +
                        "           WHERE common_interest.user = u AND common_interest.interest IN (" +
                        "               SELECT cur_interest.interest FROM UserInterestEntity cur_interest WHERE cur_interest.user = :user"
                        +
                        "           )" +
                        "       ) * 1.0" +
                        "       +" + // TODO: 이상형 점수 계산이 같은 이상형을 갖는게 아니라 이상형에 부합하는 사람이면 점수를 주는 방식으로 바꿔야함 + 자신의 특성
                                     // db구축(이상형 옵션들 다 갖고있어야함)
                        "       (" + // 6. 공통 이상형 점수 계산 (가중치 1.5)
                        "           SELECT COUNT(common_ideal) FROM UserIdealEntity common_ideal " +
                        "           WHERE common_ideal.user = u AND common_ideal.idealOption IN (" +
                        "               SELECT cur_ideal.idealOption FROM UserIdealEntity cur_ideal WHERE cur_ideal.user = :user"
                        +
                        "           )" +
                        "       ) * 1.5" +
                        "   ) DESC, " + // -- 점수 계산 종료 --
                        "   u.id ASC" // 6. 점수가 같을 경우 ID 순으로 정렬 (일관된 순서 보장)
        )
        Page<UserEntity> findTopCandidatesByPreferences(
                        @Param("user") UserEntity user,
                        @Param("excludedUserIds") Set<Long> excludedUserIds,
                        Pageable pageable);

}
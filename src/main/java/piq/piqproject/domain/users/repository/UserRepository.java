package piq.piqproject.domain.users.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import piq.piqproject.domain.users.entity.Gender;
import piq.piqproject.domain.users.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    // 이메일을 통해 사용자를 찾는 메소드
    Optional<UserEntity> findByEmail(String email);

    // 이메일 존재 여부 확인
    boolean existsByEmail(String email);

    /**
     * 성별(gender)을 기준으로 모든 사용자 엔티티를 찾아 리스트로 반환합니다.
     * Spring Data JPA가 메소드 이름을 분석하여 아래와 같은 JPQL을 자동으로 생성합니다.
     * "SELECT u FROM UserEntity u WHERE u.gender = :gender"
     *
     * @param gender 검색할 성별 (Gender Enum 타입)
     * @return 해당 성별을 가진 UserEntity의 리스트
     */
    List<UserEntity> findAllByGender(Gender gender);

}
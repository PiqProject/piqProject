package piq.piqproject.domain.users.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import piq.piqproject.domain.users.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    // 이메일을 통해 사용자를 찾는 메소드
    Optional<UserEntity> findByEmail(String email);

    // 이메일 존재 여부 확인
    boolean existsByEmail(String email);
}
package piq.piqproject.domain.userimages.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import piq.piqproject.domain.userimages.entity.UserImageEntity;
import piq.piqproject.domain.users.entity.UserEntity;

public interface UserImageRepository extends JpaRepository<UserImageEntity, Long> {

    // 특정 유저의 대표 이미지를 찾는 쿼리 메서드
    Optional<UserImageEntity> findByUserAndIsMainImage(UserEntity user, boolean isMain);

    // 특정 유저의 모든 이미지를 찾는 쿼리 메서드
    List<UserImageEntity> findAllByUser(UserEntity user);

    // 특정 유저의 이미지 개수를 세는 쿼리 메서드
    long countByUser(UserEntity user);

    // 특정 유저의 대표 이미지 존재 여부 확인 쿼리 메서드
    boolean existsByUserAndIsMainImage(UserEntity user, boolean isMain);
}
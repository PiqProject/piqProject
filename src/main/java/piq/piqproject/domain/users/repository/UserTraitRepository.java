package piq.piqproject.domain.users.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import piq.piqproject.domain.users.entity.UserTraitEntity;

public interface UserTraitRepository extends JpaRepository<UserTraitEntity, Long> {

    List<UserTraitEntity> findAllByUserId(Long userId);
}

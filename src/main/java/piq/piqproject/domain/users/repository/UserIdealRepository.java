package piq.piqproject.domain.users.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.lettuce.core.dynamic.annotation.Param;
import piq.piqproject.domain.users.entity.UserIdealEntity;

public interface UserIdealRepository extends JpaRepository<UserIdealEntity, Long> {

    @Query("SELECT uie FROM UserIdealEntity uie " +
       "JOIN FETCH uie.idealOption io " +
       "JOIN FETCH io.category c " +
       "WHERE uie.user.id = :userId")
    List<UserIdealEntity> findAllByUserId(@Param("userId") Long userId);

} 
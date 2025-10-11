package piq.piqproject.domain.users.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.lettuce.core.dynamic.annotation.Param;
import piq.piqproject.domain.interests.entity.InterestEntity;
import piq.piqproject.domain.users.entity.UserEntity;
import piq.piqproject.domain.users.entity.UserInterestEntity;

public interface UserInterestRepository extends JpaRepository<UserInterestEntity, Long> {

    void deleteAllByInterest(InterestEntity interest);

    boolean existsByUser(UserEntity user);

    @Query("SELECT uie FROM UserInterestEntity uie JOIN FETCH uie.interest i WHERE uie.user.id = :userId")
    List<UserInterestEntity> findAllByUserId(@Param("userId") Long userId);

} 
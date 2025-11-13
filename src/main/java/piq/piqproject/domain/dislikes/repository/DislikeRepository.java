package piq.piqproject.domain.dislikes.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import piq.piqproject.domain.dislikes.entity.DislikeEntity;
import piq.piqproject.domain.users.entity.UserEntity;

public interface DislikeRepository extends JpaRepository<DislikeEntity, Long> {

    // 1-1. 중복 확인을 위한 메서드 (existsBy...)
    boolean existsByFromUserAndToUser(UserEntity fromUser, UserEntity toUser);

    /**
     * 특정 사용자가 'fromUser'로서 기록한 모든 '싫어요' 기록을 삭제합니다.
     * 이 메서드는 서비스 계층에서 @Transactional 내에서 호출되어야 합니다.
     *
     * @param fromUser '싫어요' 기록을 남긴 주체 사용자
     * @return 삭제된 엔티티의 수 (int)
     */
    void deleteAllByFromUser(UserEntity fromUser);

    @Query("SELECT d.toUser.id" +
            " FROM DislikeEntity d " +
            "WHERE d.fromUser.id = :fromUserId")
    Set<Long> findToUserIdsByFromUserId(Long fromUserId);
}
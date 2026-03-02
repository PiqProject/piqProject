package piq.piqproject.domain.notifications.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import piq.piqproject.domain.notifications.entity.NotificationEntity;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

        /**
         * 특정 사용자의 알림 + 전체 공지(user IS NULL) 를 최신순으로 조회
         */
        @Query("SELECT n FROM NotificationEntity n " +
                        "WHERE n.user.id = :userId OR n.user IS NULL " +
                        "ORDER BY n.createdAt DESC")
        Page<NotificationEntity> findAllByUserIdOrGlobal(@Param("userId") Long userId, Pageable pageable);

        /**
         * 특정 사용자의 읽지 않은 알림 개수 (개인 알림만)
         */
        long countByUserIdAndIsRead(Long userId, boolean isRead);

        /**
         * 전체 공지 중 읽지 않은 것은 별도 관리가 필요하므로,
         * 전체 공지 중 특정 시간 이후에 만들어진 것의 개수로 간이 처리
         */
        @Query("SELECT COUNT(n) FROM NotificationEntity n " +
                        "WHERE n.user IS NULL AND n.isRead = false")
        long countUnreadGlobalNotifications();

        /**
         * 특정 사용자의 모든 알림을 읽음 처리
         */
        @Modifying(clearAutomatically = true)
        @Query("UPDATE NotificationEntity n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
        int markAllAsReadByUserId(@Param("userId") Long userId);

        /**
         * 특정 사용자의 개인 알림 목록 (최신순)
         */
        Page<NotificationEntity> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

        /**
         * 생성된 지 특정 시간보다 이전인 알림 삭제
         */
        @Modifying(clearAutomatically = true)
        @Query("DELETE FROM NotificationEntity n WHERE n.createdAt < :thresholdDate")
        int deleteByCreatedAtBefore(LocalDateTime thresholdDate);
}

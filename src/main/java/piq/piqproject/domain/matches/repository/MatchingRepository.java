package piq.piqproject.domain.matches.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import piq.piqproject.domain.matches.entity.MatchingEntity;
import piq.piqproject.domain.matches.enums.MatchingStatus;

public interface MatchingRepository extends JpaRepository<MatchingEntity, Long> {

        // 보낸 사람과 받는 사람 ID로 매칭 정보가 이미 존재하는지 확인하기 위한 메서드
        Optional<MatchingEntity> findBySenderIdAndReceiverId(Long senderId, Long receiverId);

        // 받는 사람 ID로 매칭 정보 조회
        List<MatchingEntity> findAllByReceiverId(Long receiverId);

        // 특정 유저가 '보낸' 모든 매칭 요청 목록을 페이징하여 조회
        Page<MatchingEntity> findBySenderId(Long senderId, Pageable pageable);

        // 특정 유저가 '받은' 모든 매칭 요청 목록을 페이징하여 조회
        Page<MatchingEntity> findByReceiverId(Long receiverId, Pageable pageable);

        @Query("SELECT m FROM MatchingEntity m JOIN FETCH m.sender s JOIN FETCH m.receiver r WHERE s.id = :senderId")
        Page<MatchingEntity> findBySenderIdWithUsers(@Param("senderId") Long senderId, Pageable pageable);

        @Query("SELECT m FROM MatchingEntity m JOIN FETCH m.sender s JOIN FETCH m.receiver r WHERE r.id = :receiverId")
        Page<MatchingEntity> findByReceiverIdWithUsers(@Param("receiverId") Long receiverId, Pageable pageable);

        @Query("SELECT m FROM MatchingEntity m WHERE " +
                        "(m.sender.id = :user1Id AND m.receiver.id = :user2Id) OR " +
                        "(m.sender.id = :user2Id AND m.receiver.id = :user1Id)")
        Optional<MatchingEntity> findMatchBetweenUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

        /**
         * 특정 사용자와 매칭된(받든 보냈든) 모든 상대방들의 ID 목록을 조회합니다.
         *
         * @param userId 현재 사용자의 ID
         * @return 매칭된 상대방들의 User ID Set
         */
        @Query("SELECT CASE " +
                        "    WHEN m.sender.id = :userId THEN m.receiver.id " + // 내가 sender면, receiver의 ID를 반환
                        "    ELSE m.sender.id " + // (내가 receiver면) sender의 ID를 반환
                        "END " +
                        "FROM MatchingEntity m " +
                        "WHERE (m.sender.id = :userId OR m.receiver.id = :userId) " +
                        "AND m.status = :status") // 내가 sender 이거나 receiver인 모든 튜플 중 특정 상태인 것만 찾음
        Set<Long> findAllMatchedUserIdsByUserIdAndStatus(@Param("userId") Long userId,
                        @Param("status") MatchingStatus status);

        /**
         * PENDING 상태이고 특정 시간 이전에 생성된 매칭 조회
         * (만료된 매칭을 스케줄러에서 처리하기 위함)
         *
         * @param status        매칭 상태 (PENDING)
         * @param expiredBefore 이 시간 이전에 생성된 매칭을 조회
         * @return 만료된 매칭 목록 (sender와 함께 fetch)
         */
        @Query("SELECT m FROM MatchingEntity m " +
                        "JOIN FETCH m.sender s " +
                        "WHERE m.status = :status AND m.createdAt < :expiredBefore")
        List<MatchingEntity> findByStatusAndCreatedAtBefore(
                        @Param("status") MatchingStatus status,
                        @Param("expiredBefore") LocalDateTime expiredBefore);
}
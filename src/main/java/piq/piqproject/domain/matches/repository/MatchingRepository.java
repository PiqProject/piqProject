package piq.piqproject.domain.matches.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import piq.piqproject.domain.matches.entity.MatchingEntity;
import piq.piqproject.domain.users.entity.UserEntity;

import java.util.List;
import java.util.Optional;

public interface MatchingRepository extends JpaRepository<MatchingEntity, Long> {

    // 보낸 사람과 받는 사람 ID로 매칭 정보가 이미 존재하는지 확인하기 위한 메서드
    Optional<MatchingEntity> findBySenderIdAndReceiverId(Long senderId, Long receiverId);

    // 받는 사람 ID로 매칭 정보 조회
    List<MatchingEntity> findAllByReceiverId(Long receiverId);

    // [추가] 특정 유저가 '보낸' 모든 매칭 요청 목록을 페이징하여 조회
    Page<MatchingEntity> findBySenderId(Long senderId, Pageable pageable);

    // [추가] 특정 유저가 '받은' 모든 매칭 요청 목록을 페이징하여 조회
    Page<MatchingEntity> findByReceiverId(Long receiverId, Pageable pageable);

    @Query("SELECT m FROM MatchingEntity m JOIN FETCH m.sender s JOIN FETCH m.receiver r WHERE s.id = :senderId")
    Page<MatchingEntity> findBySenderIdWithUsers(@Param("senderId") Long senderId, Pageable pageable);

    @Query("SELECT m FROM MatchingEntity m JOIN FETCH m.sender s JOIN FETCH m.receiver r WHERE r.id = :receiverId")
    Page<MatchingEntity> findByReceiverIdWithUsers(@Param("receiverId") Long receiverId, Pageable pageable);


    @Query("SELECT m FROM MatchingEntity m WHERE " +
           "(m.sender.id = :user1Id AND m.receiver.id = :user2Id) OR " +
           "(m.sender.id = :user2Id AND m.receiver.id = :user1Id)")
    Optional<MatchingEntity> findMatchBetweenUsers(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);
}
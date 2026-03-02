package piq.piqproject.domain.points.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import piq.piqproject.domain.points.entity.PointHistoryEntity;

public interface PointHistoryRepository extends JpaRepository<PointHistoryEntity, Long> {

    List<PointHistoryEntity> findByUserOrderByCreatedAtDesc(piq.piqproject.domain.users.entity.UserEntity user);

    /**
     * [스케줄러용] 보관 기간(5년)이 지난 포인트 이력을 DB에서 완전히 삭제합니다.
     * JPA의 delete()를 쓰면 한 건씩 조회해서 지우느라 느리지만,
     * 이 방식은 SQL DELETE 문을 바로 날려서 매우 빠릅니다.
     *
     * @param cutoffDate 삭제 기준 시간 (이 시간 이전의 데이터는 모두 삭제)
     */
    @Modifying(clearAutomatically = true) // 쿼리 실행 후 영속성 컨텍스트를 비워서 데이터 불일치 방지
    @Query("DELETE FROM PointHistoryEntity p WHERE p.createdAt < :cutoffDate")
    void deleteExpiredHistories(@Param("cutoffDate") LocalDateTime cutoffDate);
}
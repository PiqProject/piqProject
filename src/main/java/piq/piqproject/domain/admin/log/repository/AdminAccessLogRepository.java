package piq.piqproject.domain.admin.log.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import piq.piqproject.domain.admin.log.entity.AdminAccessLogEntity;

public interface AdminAccessLogRepository extends JpaRepository<AdminAccessLogEntity, Long> {
    /**
     * 특정 날짜 이전의 로그를 일괄 삭제합니다.
     * 
     * @param cutoffDate 삭제 기준 날짜 (예: 1년 전)
     */
    @Modifying(clearAutomatically = true) // 벌크 연산 후 영속성 컨텍스트 비우기
    @Query("DELETE FROM AdminAccessLogEntity a WHERE a.createdAt < :cutoffDate")
    void deleteLogsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}
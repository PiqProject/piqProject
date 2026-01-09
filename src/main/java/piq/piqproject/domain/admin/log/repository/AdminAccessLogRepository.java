package piq.piqproject.domain.admin.log.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import piq.piqproject.domain.admin.log.entity.AdminAccessLogEntity;

public interface AdminAccessLogRepository extends JpaRepository<AdminAccessLogEntity, Long> {
}
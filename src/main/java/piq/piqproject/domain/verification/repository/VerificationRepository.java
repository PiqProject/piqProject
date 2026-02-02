package piq.piqproject.domain.verification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import piq.piqproject.domain.verification.entity.VerificationEntity;

public interface VerificationRepository extends JpaRepository<VerificationEntity, Long> {

    List<VerificationEntity> findAllByUserId(Long userId);

}

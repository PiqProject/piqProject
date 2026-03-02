package piq.piqproject.domain.verification.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import piq.piqproject.domain.verification.entity.VerificationEntity;
import piq.piqproject.domain.verification.enums.ContentType;
import piq.piqproject.domain.verification.enums.VerificationStatus;

public interface VerificationRepository extends JpaRepository<VerificationEntity, Long> {

    List<VerificationEntity> findAllByUserId(Long userId);

    Page<VerificationEntity> findAllByContentType(ContentType contentType, Pageable pageable);

    Page<VerificationEntity> findAllByStatus(VerificationStatus status, Pageable pageable);

    Page<VerificationEntity> findAllByContentTypeAndStatus(ContentType contentType, VerificationStatus status,
            Pageable pageable);

}

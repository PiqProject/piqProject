package piq.piqproject.domain.inquiries.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import piq.piqproject.domain.inquiries.entity.InquiryEntity;
import piq.piqproject.domain.inquiries.enums.InquiryStatus;
import piq.piqproject.domain.users.entity.UserEntity;

public interface InquiryRepository extends JpaRepository<InquiryEntity, Long> {

        // 1. [사용자용] 내가 쓴 문의 내역 조회 (최신순)
        Page<InquiryEntity> findByUserOrderByCreatedAtDesc(UserEntity user, Pageable pageable);

        // 2. [관리자용] 문의 목록 조회 (상태, 작성자 ID로 검색)
        @Query("SELECT i FROM InquiryEntity i JOIN i.user u " +
                        "WHERE (:status IS NULL OR i.status = :status) " +
                        "AND (:searchId IS NULL OR u.id = :searchId)")
        Page<InquiryEntity> searchAdminInquiries(
                        @Param("status") InquiryStatus status,
                        @Param("searchId") Long searchId,
                        Pageable pageable);

}
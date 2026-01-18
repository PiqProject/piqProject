package piq.piqproject.domain.inquiries.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import piq.piqproject.domain.inquiries.entity.InquiryEntity;
import piq.piqproject.domain.inquiries.enums.InquiryStatus;
import piq.piqproject.domain.users.entity.UserEntity;

import java.util.List;

public interface InquiryRepository extends JpaRepository<InquiryEntity, Long> {

    // 1. [사용자용] 내가 쓴 문의 내역 조회 (최신순)
    Page<InquiryEntity> findByUserOrderByCreatedAtDesc(UserEntity user, Pageable pageable);

    // 2. [관리자용] 답변 대기 중인 문의만 조회 (오래된 순 - 먼저 온 문의부터 처리 FIFO)
    // status가 PENDING인 것들을 생성일 오름차순(Asc)으로 조회
    Page<InquiryEntity> findByStatusOrderByCreatedAtAsc(InquiryStatus status, Pageable pageable);

    // 3. [관리자용] 전체 문의 조회 (최신순 - 이력 확인용)
    Page<InquiryEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 4. [관리자용] 특정 유저의 문의 이력 조회 (CS 처리 시 참고용)
    List<InquiryEntity> findByUserOrderByCreatedAtDesc(UserEntity user);
}

package piq.piqproject.domain.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.domain.admin.dto.request.InquiryAnswerRequestDto;
import piq.piqproject.domain.admin.dto.response.AdminInquiryDetailResponseDto;
import piq.piqproject.domain.admin.dto.response.AdminInquiryListResponseDto;
import piq.piqproject.domain.inquiries.entity.InquiryEntity;
import piq.piqproject.domain.inquiries.enums.InquiryStatus;
import piq.piqproject.domain.inquiries.repository.InquiryRepository;
import piq.piqproject.domain.users.entity.UserEntity;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminInquiryService {

    private final InquiryRepository inquiryRepository;

    /**
     * 문의 목록 조회 (필터링 지원)
     * - status가 있으면: 해당 상태만 조회 (PENDING일 경우 오래된 순 ASC)
     * - status가 없으면: 전체 조회 (최신순 DESC)
     */
    public Page<AdminInquiryListResponseDto> getInquiries(InquiryStatus status, Pageable pageable) {
        Page<InquiryEntity> page;

        if (status != null) {
            // 특정 상태 조회 (PENDING은 들어온 순서대로 처리해야 하므로 ASC 권장)
            page = inquiryRepository.findByStatusOrderByCreatedAtAsc(status, pageable);
        } else {
            // 전체 조회 (이력 확인용이므로 최신순 DESC)
            page = inquiryRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return page.map(AdminInquiryListResponseDto::from);
    }

    /**
     * 문의 상세 조회
     */
    public AdminInquiryDetailResponseDto getInquiryDetail(Long inquiryId) {
        InquiryEntity inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "해당 문의를 찾을 수 없습니다."));

        return AdminInquiryDetailResponseDto.from(inquiry);
    }

    /**
     * 답변 등록 및 수정
     */
    @Transactional
    public void replyInquiry(Long inquiryId, InquiryAnswerRequestDto request, UserEntity admin) {
        InquiryEntity inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "해당 문의를 찾을 수 없습니다."));

        inquiry.registerAnswer(request.getAnswer(), admin.getEmail());

        // TODO: 알림 전송 로직 필요 (문의 답변 등록)
        // inquiry.getUser()에게 "작성하신 문의에 답변이 등록되었습니다." 알림 전송
    }
}
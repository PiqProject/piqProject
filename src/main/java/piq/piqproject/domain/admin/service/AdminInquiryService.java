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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

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
    public Page<AdminInquiryListResponseDto> getInquiries(InquiryStatus status, Long searchId, Pageable pageable) {
        // 1. 상태(status) 유무에 따라 정렬 방향(ASC/DESC) 결정
        Sort sort = (status == InquiryStatus.PENDING)
                ? Sort.by(Sort.Direction.ASC, "createdAt") // PENDING 등 특정 상태는 오래된 순
                : Sort.by(Sort.Direction.DESC, "createdAt"); // 전체 조회,Answered 상태는 최신 순

        // 2. 컨트롤러에서 넘어온 pageable의 '페이지 번호'와 '크기'는 유지하되, 정렬만 우리가 정한 걸로 교체
        Pageable customPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        // 3. 상태와 키워드를 모두 처리하는 단일 Repository 메서드 호출
        Page<InquiryEntity> page = inquiryRepository.searchAdminInquiries(status, searchId, customPageable);

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
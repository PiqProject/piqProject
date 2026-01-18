package piq.piqproject.domain.inquiries.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import piq.piqproject.common.error.exception.ErrorCode;
import piq.piqproject.common.error.exception.NotFoundException;
import piq.piqproject.common.error.exception.UnauthorizedException;
import piq.piqproject.domain.inquiries.dto.request.InquiryCreateRequestDto;
import piq.piqproject.domain.inquiries.dto.response.InquiryDetailResponseDto;
import piq.piqproject.domain.inquiries.dto.response.InquiryListResponseDto;
import piq.piqproject.domain.inquiries.entity.InquiryEntity;
import piq.piqproject.domain.inquiries.repository.InquiryRepository;
import piq.piqproject.domain.users.entity.UserEntity;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;

    /**
     * 문의 등록
     */
    @Transactional
    public Long createInquiry(UserEntity user, InquiryCreateRequestDto request) {
        InquiryEntity inquiry = InquiryEntity.builder()
                .user(user)
                .category(request.getCategory())
                .title(request.getTitle())
                .content(request.getContent())
                // status는 Default로 PENDING
                .build();

        return inquiryRepository.save(inquiry).getId();
    }

    /**
     * 내 문의 목록 조회 (페이징)
     */
    public Page<InquiryListResponseDto> getMyInquiries(UserEntity user, Pageable pageable) {
        return inquiryRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(InquiryListResponseDto::from);
    }

    /**
     * 문의 상세 조회 (본인 확인 필수)
     */
    public InquiryDetailResponseDto getInquiryDetail(UserEntity user, Long inquiryId) {
        InquiryEntity inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND, "해당 문의를 찾을 수 없습니다."));

        // [보안] 본인이 쓴 글인지 확인 (ID가 다르면 예외 발생)
        if (!inquiry.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException(ErrorCode.NOT_OWNER, "본인의 문의 내역만 확인할 수 있습니다.");
        }

        return InquiryDetailResponseDto.from(inquiry);
    }
}